package com.doubleangels.nextdnsmanagement;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.biometric.BiometricPrompt;
import androidx.lifecycle.LifecycleOwner;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import com.doubleangels.nextdnsmanagement.biometriclock.BiometricLock;
import com.doubleangels.nextdnsmanagement.protocol.VisualIndicator;
import com.doubleangels.nextdnsmanagement.sentry.SentryInitializer;
import com.doubleangels.nextdnsmanagement.sentry.SentryManager;
import com.doubleangels.nextdnsmanagement.utils.ExternalLinkHandler;
import com.doubleangels.nextdnsmanagement.utils.InsetsHelper;
import com.doubleangels.nextdnsmanagement.sharedpreferences.SharedPreferencesManager;
import com.doubleangels.nextdnsmanagement.webview.WebAppInterface;
import com.doubleangels.nextdnsmanagement.webview.WebDownloadHelper;
import com.doubleangels.nextdnsmanagement.webview.WebViewInteractionScript;
import com.doubleangels.nextdnsmanagement.webview.WebViewLayoutHelper;

import java.util.Locale;

/**
 * Shared implementation for the app's main activity: UI, WebView, and various
 * settings such as dark mode, locale, biometric re-authentication, and notification
 * permission checks.
 * <p>
 * The gms and foss flavors differ only in Firebase Cloud Messaging availability;
 * {@link #initializeMessaging()} and {@link #onBiometricAuthenticationSucceeded()}
 * are the two hooks each flavor's thin {@code MainActivity} subclass overrides for
 * that purpose. Everything else lives here, shared, so a fix only needs to be made
 * once.
 */
public abstract class BaseMainActivity extends BaseActivity {

    // Main WebView for displaying web content
    private WebView webView;
    // SwipeRefreshLayout wrapping the WebView to enable pull-to-refresh
    // functionality
    private SwipeRefreshLayout swipeRefreshLayout;
    // Flag indicating whether dark mode is enabled
    private Boolean darkModeEnabled = false;
    // Flag to avoid re-initializing the WebView if it has already been set up
    private Boolean isWebViewInitialized = false;
    // Biometric authentication timeout in milliseconds (5 minutes)
    private static final long AUTH_TIMEOUT_MS = 5 * 60 * 1000;
    private static final String LAST_WEBVIEW_URL_KEY = "last_webview_url";
    private static final String LAST_AUTH_TIME_KEY = "last_authenticated_time";
    // Timestamp (in ms) of the last successful biometric authentication
    private long lastAuthenticatedTime = 0;
    private boolean isBiometricPromptShowing = false;
    private int biometricErrorRetries = 0;
    private static final int MAX_BIOMETRIC_ERROR_RETRIES = 3;
    private float webViewTouchStartX;
    private float webViewTouchStartY;
    private int webViewTouchSlop;

    // Blur overlay view to hide content during biometric authentication
    private View blurOverlay;
    private View webViewErrorView;
    // SentryManager instance for error logging. Protected so flavor subclasses can
    // report from their own hook overrides (e.g. gms's FCM init failure path).
    protected SentryManager sentryManager;

    /**
     * Saves the current state of the activity, including the WebView state and dark
     * mode flag.
     *
     * @param outState Bundle in which to save the activity state.
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        try {
            // We consciously avoid saving the WebView state to prevent
            // TransactionTooLargeException which occurs when the WebView
            // accumulates too much data (e.g. from single-page apps like nextdns.io)
            // in its saved bundle.
            outState.putBoolean("darkModeEnabled", darkModeEnabled);
        } catch (Exception e) {
            SentryManager.captureStaticException(e);
        }
    }

    /**
     * Called when the activity is created.
     * Initializes Sentry, messaging, UI components, language, dark mode, visual
     * indicator,
     * swipe-to-refresh, and the WebView.
     *
     * @param savedInstanceState Bundle containing the activity's previously saved
     *                           state.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        androidx.core.splashscreen.SplashScreen splashScreen =
                androidx.core.splashscreen.SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            darkModeEnabled = savedInstanceState.getBoolean("darkModeEnabled");
        }
        try {
            setContentView(R.layout.activity_main);
        } catch (Throwable e) {
            // Some devices ship a broken/mismatched system WebView provider that fails
            // to inflate; there is no way to recover the WebView-based UI in that case.
            SentryManager.captureStaticException(e instanceof Exception ? (Exception) e : new RuntimeException(e));
            Toast.makeText(this, R.string.webview_unavailable, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Enable hardware acceleration programmatically
        getWindow().setFlags(
                android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);

        // Initialize blur overlay
        blurOverlay = findViewById(R.id.blurOverlay);
        webViewErrorView = findViewById(R.id.webViewErrorView);

        sentryManager = new SentryManager(this);

        if (SharedPreferencesManager.isInitialized()) {
            // Warm recreation (locale/density/theme change, or returning to a
            // still-warm process) -- nothing to wait on, so skip the background
            // thread and let the splash screen dismiss immediately instead of
            // artificially holding it for a round-trip that has nothing to do.
            finishStartup();
        } else {
            AppStartupHelper.initializePreferencesAsync(this, splashScreen, this::finishStartup);
        }
    }

    private void finishStartup() {
        if (isFinishing() || isDestroyed() || sentryManager == null) {
            return;
        }

        lastAuthenticatedTime = SharedPreferencesManager.getLong(LAST_AUTH_TIME_KEY, 0);

        try {
            if (sentryManager.isEnabled()) {
                SentryInitializer.initialize(this);
            }
        } catch (Exception e) {
            sentryManager.captureException(e);
        }

        try {
            initializeMessaging();
        } catch (Exception e) {
            sentryManager.captureException(e);
        }

        try {
            setupInsetsForActivity();
            setupToolbarForActivity();
            setupPredictiveBackForActivity();
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
        try {
            setupLanguageForActivity();
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
        try {
            setupDarkModeForActivity(sentryManager, SharedPreferencesManager.getString("dark_mode", "match"));
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
        try {
            setupVisualIndicatorForActivity(sentryManager, this);
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
        try {
            setupSwipeToRefreshForActivity();
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
        SwipeRefreshLayout refreshLayout = findViewById(R.id.swipeRefreshLayout);
        if (refreshLayout != null) {
            refreshLayout.post(() -> {
                if (!isFinishing()) {
                    try {
                        setupWebViewForActivity(getString(R.string.main_url));
                    } catch (Exception e) {
                        sentryManager.captureException(e);
                    }
                }
            });
        }

        maybeShowBiometricPrompt();
    }

    /**
     * Starts any push-messaging registration the flavor needs (FCM token/topic
     * registration for gms). No-op by default; foss has no messaging to initialize.
     * Called from {@link #finishStartup()}, already wrapped in a try/catch that
     * reports to Sentry.
     */
    protected void initializeMessaging() {
        // No-op by default.
    }

    private void setupPredictiveBackForActivity() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView != null && webView.canGoBack()) {
                    webView.goBack();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void setupInsetsForActivity() {
        View root = findViewById(R.id.root);
        InsetsHelper.installOnRoot(root);
        View statusBarScrim = findViewById(R.id.statusBarScrim);
        InsetsHelper.applyStatusBarScrimHeight(statusBarScrim);
        SwipeRefreshLayout refreshLayout = findViewById(R.id.swipeRefreshLayout);
        InsetsHelper.applyBottomSystemBarPadding(refreshLayout);
    }

    private void persistLastAuthenticatedTime() {
        lastAuthenticatedTime = System.currentTimeMillis();
        SharedPreferencesManager.putLong(LAST_AUTH_TIME_KEY, lastAuthenticatedTime);
    }

    /**
     * Called when the activity is destroyed.
     * Cleans up the WebView and SwipeRefreshLayout to avoid memory leaks.
     * Enhanced with comprehensive memory management.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            // Remove WebView from its parent if attached
            if (webView != null && webView.getParent() != null) {
                ((ViewGroup) webView.getParent()).removeView(webView);
            }
            if (webView != null) {
                webView.evaluateJavascript(WebViewInteractionScript.DISCONNECT_SCRIPT, null);
                // Remove JavaScript interfaces and clients
                webView.removeJavascriptInterface("AndroidInterface");
                webView.setWebViewClient(new WebViewClient());
                webView.setWebChromeClient(new WebChromeClient());
                webView.setDownloadListener(null);
                // Clear WebView cache and data
                webView.clearCache(true);
                webView.clearHistory();
                webView.clearFormData();
                // Destroy the WebView
                webView.destroy();
                webView = null;
            }
            if (swipeRefreshLayout != null) {
                // Remove refresh listener
                swipeRefreshLayout.setOnRefreshListener(null);
                swipeRefreshLayout = null;
            }
            if (blurOverlay != null) {
                blurOverlay.animate().cancel();
                blurOverlay.clearAnimation();
                blurOverlay = null;
            }
            // Clear sentry manager reference
            sentryManager = null;
        } catch (Exception e) {
            SentryManager.captureStaticException(e);
        }
    }

    /**
     * Pauses the WebView when the activity is paused.
     * Also pauses JavaScript execution and timers to save battery.
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
            webView.pauseTimers();
        }
        try {
            CookieManager.getInstance().flush();
        } catch (Exception e) {
            SentryManager.captureStaticException(e);
        }
    }

    /**
     * MainActivity declares configChanges for screen size/layout/orientation so the
     * activity survives a foldable fold/unfold transition instead of being destroyed
     * and recreated. Android does not automatically repaint the WebView's native
     * surface at its new size in that case, which otherwise leaves it showing a
     * blank/stale frame sized for the previous fold state. Force a re-measure and
     * repaint once the resize settles.
     */
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (webView == null) {
            return;
        }
        webView.post(() -> {
            if (isFinishing() || isDestroyed() || webView == null) {
                return;
            }
            webView.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
            webView.requestLayout();
            webView.invalidate();
        });
    }

    /**
     * Handles results from other activities to ensure biometric authentication is
     * maintained.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (SharedPreferencesManager.isInitialized()) {
            maybeShowBiometricPrompt();
        }
    }

    /**
     * Resumes the WebView and triggers biometric authentication if needed.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (!SharedPreferencesManager.isInitialized()) {
            return;
        }
        // Resume WebView if it exists; otherwise, initialize it
        if (webView != null) {
            webView.onResume();
            // Resume JavaScript execution and timers
            webView.resumeTimers();
        } else if (!isWebViewInitialized) {
            try {
                setupWebViewForActivity(getString(R.string.main_url));
            } catch (Exception e) {
                if (sentryManager != null) {
                    sentryManager.captureException(e);
                } else {
                    SentryManager.captureStaticException(e);
                }
            }
        }
        if (sentryManager != null) {
            setupDarkModeForActivity(sentryManager, SharedPreferencesManager.getString("dark_mode", "match"));
        }
        maybeShowBiometricPrompt();
    }

    /**
     * Determines whether biometric authentication is required based on the timeout.
     *
     * @return true if the elapsed time since last authentication exceeds the
     *         timeout.
     */
    private boolean shouldAuthenticate() {
        return System.currentTimeMillis() - lastAuthenticatedTime > AUTH_TIMEOUT_MS;
    }

    private void maybeShowBiometricPrompt() {
        if (SharedPreferencesManager.getBoolean("app_lock_enable", false) && shouldAuthenticate()) {
            showBiometricPrompt();
        }
    }

    /**
     * Sets up the toolbar for the activity.
     * Configures the action bar and assigns a click listener to the connection
     * status icon.
     */
    private void setupToolbarForActivity() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Disable default title display
            actionBar.setDisplayShowTitleEnabled(false);
        }
        // Launch StatusActivity when the connection status icon is clicked
        ImageView imageView = findViewById(R.id.connectionStatus);
        if (imageView != null) {
            imageView.setOnClickListener(v -> startActivity(new Intent(this, StatusActivity.class)));
        }
    }

    /**
     * Configures the language settings for the activity.
     * Applies the current locale to the application's configuration.
     *
     * @return The language code of the current locale.
     */
    private String setupLanguageForActivity() {
        Configuration config = getResources().getConfiguration();
        Locale appLocale = !config.getLocales().isEmpty()
                ? config.getLocales().get(0)
                : Locale.getDefault();
        Locale.setDefault(appLocale);
        Configuration newConfig = new Configuration(config);
        newConfig.setLocale(appLocale);
        new ContextThemeWrapper(getBaseContext(), R.style.AppTheme).applyOverrideConfiguration(newConfig);
        return appLocale.getLanguage();
    }

    /**
     * Sets up dark mode based on user preference.
     * Configures the AppCompatDelegate accordingly and logs the setting.
     *
     * @param sentryManager SentryManager instance for logging.
     * @param darkMode      Dark mode setting ("match", "on", "disabled", "off").
     */
    private void setupDarkModeForActivity(SentryManager sentryManager, String darkMode) {
        switch (darkMode) {
            case "match":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                updateDarkModeState();
                break;
            case "on":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                darkModeEnabled = true;
                break;
            case "disabled":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                darkModeEnabled = false;
                break;
            case "off":
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                darkModeEnabled = false;
                break;
        }
    }

    /**
     * Updates the internal dark mode flag based on the current UI configuration.
     */
    private void updateDarkModeState() {
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        darkModeEnabled = (nightModeFlags == Configuration.UI_MODE_NIGHT_YES);
    }

    /**
     * Initializes a visual indicator (e.g., a network or loading indicator) using
     * the VisualIndicator class.
     *
     * @param sentryManager  SentryManager instance for logging.
     * @param lifecycleOwner LifecycleOwner to manage the indicator's lifecycle.
     */
    private void setupVisualIndicatorForActivity(SentryManager sentryManager, LifecycleOwner lifecycleOwner) {
        try {
            new VisualIndicator(this).initialize(this, lifecycleOwner, this);
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
    }

    /**
     * Configures and loads the WebView with the specified URL.
     * Restores saved state if dark mode is enabled and state exists.
     *
     * @param url The URL to load in the WebView.
     */
    @SuppressLint("SetJavaScriptEnabled")
    public void setupWebViewForActivity(String url) {
        webView = WebViewLayoutHelper.findOrCreateWebView(this, swipeRefreshLayout);
        if (webView == null) {
            return;
        }
        if (swipeRefreshLayout == null) {
            swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        }

        try {
            WebSettings webViewSettings = webView.getSettings();
            webViewSettings.setJavaScriptEnabled(true);
            webViewSettings.setDomStorageEnabled(true);
            webViewSettings.setDatabaseEnabled(true);
            webViewSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
            webViewSettings.setAllowFileAccess(false);
            webViewSettings.setAllowContentAccess(false);
            webViewSettings.setBuiltInZoomControls(false);
            webViewSettings.setDisplayZoomControls(false);
            webViewSettings.setSupportZoom(false);
            webViewSettings.setLoadWithOverviewMode(true);
            webViewSettings.setUseWideViewPort(true);

            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    Uri uri = request.getUrl();
                    if (uri == null) {
                        return false;
                    }
                    if (ExternalLinkHandler.isNextDnsHost(uri)) {
                        return false;
                    }
                    return ExternalLinkHandler.openExternalLink(view.getContext(), view, uri);
                }

                @Override
                public void onPageStarted(WebView view, String pageUrl, android.graphics.Bitmap favicon) {
                    hideWebViewError();
                }

                @Override
                public void onPageFinished(WebView view, String pageUrl) {
                    try {
                        hideWebViewError();
                        if (swipeRefreshLayout != null) {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                        if (isValidNextDnsUrl(pageUrl)) {
                            SharedPreferencesManager.putString(LAST_WEBVIEW_URL_KEY, pageUrl);
                        }
                        CookieManager.getInstance().setAcceptCookie(true);
                        CookieManager.getInstance().acceptCookie();
                        view.evaluateJavascript(WebViewInteractionScript.PAGE_FINISHED_SCRIPT, null);
                    } catch (Exception e) {
                        SentryManager.captureStaticException(e);
                    }
                }

                @Override
                public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                    if (request.isForMainFrame()) {
                        showWebViewError();
                        if (sentryManager != null) {
                            sentryManager.captureMessage(
                                    "WebView error: " + error.getDescription() + " url=" + request.getUrl());
                        }
                    }
                }

                @Override
                public void onReceivedHttpError(WebView view, WebResourceRequest request,
                        WebResourceResponse errorResponse) {
                    if (request.isForMainFrame()) {
                        showWebViewError();
                        if (sentryManager != null) {
                            sentryManager.captureMessage(
                                    "WebView HTTP error: " + errorResponse.getStatusCode()
                                            + " url=" + request.getUrl());
                        }
                    }
                }

                @Override
                public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                    if (sentryManager != null) {
                        sentryManager.captureMessage(
                                "WebView render process gone, didCrash=" + detail.didCrash());
                    }
                    teardownWebViewForRecovery();
                    setupWebViewForActivity(getString(R.string.main_url));
                    return true;
                }
            });

            webView.setWebChromeClient(new WebChromeClient() {
                @Override
                public void onShowCustomView(android.view.View view, CustomViewCallback callback) {
                    super.onShowCustomView(view, callback);
                }

                @Override
                public void onHideCustomView() {
                    super.onHideCustomView();
                }

                @Override
                public boolean onShowFileChooser(WebView webView, android.webkit.ValueCallback<Uri[]> filePathCallback,
                        FileChooserParams fileChooserParams) {
                    return super.onShowFileChooser(webView, filePathCallback, fileChooserParams);
                }
            });

            if (Boolean.TRUE.equals(darkModeEnabled)) {
                if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                    WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.getSettings(), true);
                }
            }

            setupDownloadManagerForActivity();
            setupWebViewTouchHandling();
            hideWebViewError();
            loadWebViewUrl(webView, url);
            isWebViewInitialized = true;
        } catch (Exception e) {
            SentryManager.captureStaticException(e);
        }
    }

    /**
     * Configures the SwipeRefreshLayout to allow pull-to-refresh on the WebView.
     * Also adds a JavaScript interface to control swipe refresh behavior from web
     * content.
     */
    private void setupSwipeToRefreshForActivity() {
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        webView = findViewById(R.id.webView);
        if (webView == null || swipeRefreshLayout == null) {
            return;
        }
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new WebAppInterface(this, swipeRefreshLayout), "AndroidInterface");
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (webView != null) {
                webView.reload();
            }
        });
        swipeRefreshLayout.setDistanceToTriggerSync(200);
        swipeRefreshLayout.setSlingshotDistance(200);
    }

    /**
     * Configures the download manager to handle file downloads initiated within the
     * WebView.
     * Downloads are saved to the app's external downloads directory.
     */
    private void setupDownloadManagerForActivity() {
        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            if (!WebDownloadHelper.isDownloadManagerSupportedUrl(url)) {
                Toast.makeText(BaseMainActivity.this, R.string.download_failed, Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url.trim()));
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS,
                        "NextDNS-Configuration-" + System.currentTimeMillis() + ".mobileconfig");
                DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                if (downloadManager != null) {
                    // Enqueue the download request
                    downloadManager.enqueue(request);
                } else {
                    throw new Exception("DownloadManager is null");
                }
                Toast.makeText(getApplicationContext(), R.string.download_started, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                SentryManager.captureStaticException(e);
                Toast.makeText(BaseMainActivity.this, R.string.download_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Shows the blur overlay to hide WebView content during biometric
     * authentication.
     */
    private void showBlurOverlay() {
        if (blurOverlay == null) {
            return;
        }
        View overlay = blurOverlay;
        overlay.animate().cancel();
        // Set overlay color based on current theme
        int overlayColor;
        if (darkModeEnabled) {
            // Dark mode: #212529
            overlayColor = android.graphics.Color.parseColor("#212529");
        } else {
            // Light mode: #007bff
            overlayColor = android.graphics.Color.parseColor("#007bff");
        }

        overlay.setBackgroundColor(overlayColor);
        overlay.setVisibility(View.VISIBLE);
        overlay.setScaleX(0.95f);
        overlay.setScaleY(0.95f);
        overlay.animate()
                .alpha(1.0f)
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(300)
                .start();
    }

    /**
     * Hides the blur overlay after biometric authentication is complete.
     */
    private void hideBlurOverlay() {
        if (blurOverlay == null) {
            return;
        }
        View overlay = blurOverlay;
        overlay.animate().cancel();
        overlay.animate()
                .alpha(0.0f)
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(300)
                .withEndAction(() -> {
                    if (isFinishing() || isDestroyed() || blurOverlay != overlay || !overlay.isAttachedToWindow()) {
                        return;
                    }
                    overlay.setVisibility(View.GONE);
                    overlay.setScaleX(1.0f);
                    overlay.setScaleY(1.0f);
                })
                .start();
    }

    /**
     * Displays a biometric prompt to the user if authentication is available.
     * On successful authentication, updates the last authentication time and, if
     * necessary,
     * requests notification permission.
     */
    private void showBiometricPrompt() {
        if (isBiometricPromptShowing || isFinishing()) {
            return;
        }

        final BiometricLock biometricLock = new BiometricLock(this);
        if (!biometricLock.canAuthenticate()) {
            handleAppLockUnavailable();
            return;
        }

        isBiometricPromptShowing = true;
        showBlurOverlay();

        biometricLock.showPrompt(
                getString(R.string.unlock_title),
                getString(R.string.unlock_description),
                "",
                new BiometricLock.BiometricLockCallback() {
                    @Override
                    public void onAuthenticationSucceeded() {
                        isBiometricPromptShowing = false;
                        biometricErrorRetries = 0;
                        persistLastAuthenticatedTime();
                        hideBlurOverlay();
                        onBiometricAuthenticationSucceeded();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, String error) {
                        isBiometricPromptShowing = false;
                        if (errorCode == BiometricPrompt.ERROR_USER_CANCELED
                                || errorCode == BiometricPrompt.ERROR_CANCELED
                                || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                            hideBlurOverlay();
                            finish();
                        } else if (biometricErrorRetries < MAX_BIOMETRIC_ERROR_RETRIES) {
                            biometricErrorRetries++;
                            getWindow().getDecorView().post(BaseMainActivity.this::showBiometricPrompt);
                        } else {
                            hideBlurOverlay();
                            finish();
                        }
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        // Keep blur overlay visible; the system prompt allows retry.
                    }
                });
    }

    /**
     * Called right after a successful biometric unlock. No-op by default; gms
     * overrides this to request the POST_NOTIFICATIONS permission, since that's
     * only meaningful where FCM push notifications exist.
     */
    protected void onBiometricAuthenticationSucceeded() {
        // No-op by default.
    }

    private void handleAppLockUnavailable() {
        showBlurOverlay();
        Toast.makeText(this, R.string.app_lock_unavailable, Toast.LENGTH_LONG).show();
        finish();
    }

    /**
     * Inflates the options menu.
     *
     * @param menu The menu in which items are placed.
     * @return true for the menu to be displayed.
     */
    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * Handles selections from the options menu.
     *
     * @param item The selected menu item.
     * @return The result of the menu item selection.
     */
    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.back) {
            if (webView == null) {
                setupWebViewForActivity(getString(R.string.main_url));
            } else {
                webView.goBack();
            }
        } else if (itemId == R.id.refreshNextDNS) {
            if (webView == null) {
                setupWebViewForActivity(getString(R.string.main_url));
            } else {
                webView.reload();
            }
        } else if (itemId == R.id.pingNextDNS) {
            startActivity(new Intent(this, PingActivity.class));
        } else if (itemId == R.id.returnHome) {
            if (webView == null) {
                setupWebViewForActivity(getString(R.string.main_url));
            } else {
                webView.loadUrl(getString(R.string.main_url));
            }
        } else if (itemId == R.id.privateDNS) {
            startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
        } else if (itemId == R.id.settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupWebViewTouchHandling() {
        if (webView == null || swipeRefreshLayout == null) {
            return;
        }
        webViewTouchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
        webView.setOnTouchListener((view, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    webViewTouchStartX = event.getX();
                    webViewTouchStartY = event.getY();
                    // Claim the gesture for the WebView up front. SwipeRefreshLayout's
                    // own onInterceptTouchEvent() runs ahead of this listener on every
                    // MOVE, so waiting until we see a horizontal drag to disallow
                    // intercept is too late: SwipeRefreshLayout can already have
                    // claimed an ambiguous (slightly diagonal) gesture by then, which
                    // cancels whatever Chromium was doing with that touch stream --
                    // stuttering an in-page horizontal scroll or orphaning the text
                    // selection toolbar mid-render (a black box). We only release the
                    // claim below once it's clearly a downward pull at the page top.
                    swipeRefreshLayout.requestDisallowInterceptTouchEvent(true);
                    break;
                case MotionEvent.ACTION_MOVE:
                    float dx = Math.abs(event.getX() - webViewTouchStartX);
                    float dy = event.getY() - webViewTouchStartY;
                    if (dy > webViewTouchSlop && dy > dx && webView.getScrollY() == 0) {
                        swipeRefreshLayout.requestDisallowInterceptTouchEvent(false);
                    }
                    break;
                default:
                    break;
            }
            return false;
        });
    }

    private void loadWebViewUrl(WebView targetWebView, String defaultUrl) {
        String currentUrl = targetWebView.getUrl();
        if (isValidNextDnsUrl(currentUrl)) {
            return;
        }
        String savedUrl = SharedPreferencesManager.getString(LAST_WEBVIEW_URL_KEY, null);
        if (isValidNextDnsUrl(savedUrl)) {
            targetWebView.loadUrl(savedUrl);
            return;
        }
        targetWebView.loadUrl(defaultUrl);
    }

    private void showWebViewError() {
        if (webViewErrorView != null) {
            webViewErrorView.setVisibility(View.VISIBLE);
            webViewErrorView.setOnClickListener(v -> {
                hideWebViewError();
                if (webView != null) {
                    webView.reload();
                } else {
                    setupWebViewForActivity(getString(R.string.main_url));
                }
            });
        }
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void hideWebViewError() {
        if (webViewErrorView != null) {
            webViewErrorView.setVisibility(View.GONE);
        }
    }

    private void teardownWebViewForRecovery() {
        isWebViewInitialized = false;
        if (webView == null) {
            return;
        }
        try {
            if (webView.getParent() != null) {
                ((ViewGroup) webView.getParent()).removeView(webView);
            }
            webView.removeJavascriptInterface("AndroidInterface");
            webView.setWebViewClient(new WebViewClient());
            webView.setWebChromeClient(new WebChromeClient());
            webView.setDownloadListener(null);
            webView.destroy();
        } catch (Exception e) {
            SentryManager.captureStaticException(e);
        } finally {
            webView = null;
        }
    }

    private boolean isValidNextDnsUrl(String url) {
        if (url == null || url.isBlank() || "about:blank".equals(url)) {
            return false;
        }
        try {
            return ExternalLinkHandler.isNextDnsHost(Uri.parse(url));
        } catch (Exception e) {
            return false;
        }
    }
}
