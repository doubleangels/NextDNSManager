package com.doubleangels.nextdnsmanagement;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import java.io.InputStream;
import java.util.Objects;

import io.sentry.ITransaction;
import io.sentry.Sentry;

public class MainActivity extends AppCompatActivity {
    private final DarkModeHandler darkModeHandler = new DarkModeHandler();
    private Boolean isDarkNavigation;
    private Boolean isDarkModeOn;
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ITransaction mainActivityCreateTransaction = Sentry.startTransaction("MainActivity_onCreate()", "MainActivity");

        try {
            initializePreferencesAndStyles();
            setupVisualIndicator();
            provisionWebView(getString(R.string.main_url), isDarkModeOn);
        } catch (Exception e) {
            Sentry.captureException(e);
        } finally {
            mainActivityCreateTransaction.finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        darkModeHandler.handleDarkMode(this);
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refreshNextDNS -> webView.reload();
            case R.id.pingNextDNS -> startIntent(PingActivity.class);
            case R.id.testNextDNS -> startIntent(TestActivity.class);
            case R.id.returnHome -> provisionWebView(getString(R.string.main_url), isDarkModeOn);
            case R.id.settings -> startIntent(SettingsActivity.class);
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void replaceCSS(String url, boolean isDarkThemeOn) {
        ITransaction replaceCSSTransaction = Sentry.startTransaction("MainActivity_replaceCSS()", "MainActivity");
        try {
            setupWebViewClient(isDarkThemeOn);
            webView.loadUrl(url);
        } catch (Exception e) {
            Sentry.captureException(e);
        } finally {
            replaceCSSTransaction.finish();
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void provisionWebView(String url, Boolean isDarkThemeOn) {
        ITransaction provisionWebViewTransaction = Sentry.startTransaction("MainActivity_provisionWebView()", "MainActivity");
        try {
            setupWebView();
            setupDownloadManager();
            configureCookieManager();
            replaceCSS(url, isDarkThemeOn);
        } catch (Exception e) {
            Sentry.captureException(e);
        } finally {
            provisionWebViewTransaction.finish();
        }
    }

    private void initializePreferencesAndStyles() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        isDarkNavigation = sharedPreferences.getBoolean(SettingsActivity.DARK_NAVIGATION, false);
        setupWindowStyles();
        setupVisualIndicator();
        configureDarkModeSettings(sharedPreferences);
        setAppCompatDelegate();
    }

    private void setupWindowStyles() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        if (isDarkNavigation) {
            setupDarkNavigationStyles(window);
        } else {
            setupDefaultNavigationStyles();
        }
    }

    private void setupDarkNavigationStyles(Window window) {
        int darkGrayColor = ContextCompat.getColor(this, R.color.darkgray);
        window.setStatusBarColor(darkGrayColor);
        window.setNavigationBarColor(darkGrayColor);
        setToolbarStyles(darkGrayColor);
    }

    private void setupDefaultNavigationStyles() {
        int blueColor = ContextCompat.getColor(this, R.color.blue);
        setToolbarStyles(blueColor);
    }

    private void setToolbarStyles(int backgroundColor) {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        toolbar.setBackgroundColor(backgroundColor);
    }

    private void configureDarkModeSettings(SharedPreferences sharedPreferences) {
        boolean overrideDarkMode = sharedPreferences.getBoolean(SettingsActivity.OVERRIDE_DARK_MODE, false);
        boolean manualDarkMode = sharedPreferences.getBoolean(SettingsActivity.MANUAL_DARK_MODE, false);

        if (overrideDarkMode) {
            isDarkModeOn = manualDarkMode;
        } else {
            isDarkModeOn = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        }
    }

    private void setAppCompatDelegate() {
        if (isDarkModeOn) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void setupWebView() {
        webView = findViewById(R.id.mWebview);
        configureWebView(webView);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView(WebView webView) {
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
    }

    private void setupWebViewClient(boolean isDarkThemeOn) {
        if (isDarkThemeOn) {
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public WebResourceResponse shouldInterceptRequest(final WebView view, String url) {
                    return handleWebResourceRequests(url);
                }
            });
        }
    }

    @SuppressLint("NewApi")
    private WebResourceResponse handleWebResourceRequests(String url) {
        if (url.contains("apple.nextdns.io")) {
            return null;
        } else if (url.contains("help.nextdns.io")) {
            return null;
        } else if (url.contains("bitpay.com")) {
            return null;
        } else if (url.endsWith(".css")) {
            return getCssWebResourceResponseFromAsset();
        } else if (url.contains("ens-text")) {
            return getPngWebResourceResponse("ens-text.png");
        } else if (url.contains("unstoppabledomains")) {
            return getPngWebResourceResponse("unstoppabledomains.png");
        } else if (url.contains("handshake")) {
            return getPngWebResourceResponse("handshake.png");
        } else if (url.contains("ipfs")) {
            return getPngWebResourceResponse("ipfs.png");
        } else {
            return null;
        }
    }

    @SuppressLint("NewApi")
    private WebResourceResponse getCssWebResourceResponseFromAsset() {
        try {
            InputStream fileInput = getAssets().open("nextdns.css");
            return getUtf8EncodedCssWebResourceResponse(fileInput);
        } catch (Exception e) {
            Sentry.captureException(e);
        }
        return null;
    }

    @SuppressLint("NewApi")
    private WebResourceResponse getPngWebResourceResponse(String assetFileName) {
        try {
            InputStream is = getAssets().open(assetFileName);
            return new WebResourceResponse("image/png", "UTF-8", is);
        } catch (Exception e) {
            Sentry.captureException(e);
        }
        return null;
    }

    @SuppressLint("NewApi")
    private WebResourceResponse getUtf8EncodedCssWebResourceResponse(InputStream fileStream) {
        return new WebResourceResponse("text/css", "UTF-8", fileStream);
    }

    private void setupDownloadManager() {
        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "NextDNS-Configuration.mobileconfig");
            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            dm.enqueue(request);
            Toast.makeText(getApplicationContext(), "Downloading file!", Toast.LENGTH_LONG).show();
        });
    }

    private void configureCookieManager() {
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);
    }

    private void setupVisualIndicator() {
        VisualIndicator visualIndicator = new VisualIndicator();
        visualIndicator.initiateVisualIndicator(this, getApplicationContext());
    }

    private void startIntent(Class<?> targetClass) {
        Intent intent = new Intent(this, targetClass);
        startActivity(intent);
    }
}
