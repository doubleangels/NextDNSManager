package com.doubleangels.nextdnsmanagement;

import static android.Manifest.permission.POST_NOTIFICATIONS;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceManager;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import com.doubleangels.nextdnsmanagement.protocol.VisualIndicator;
import com.doubleangels.nextdnsmanagement.sentry.SentryInitializer;
import com.doubleangels.nextdnsmanagement.sentry.SentryManager;
import com.jakewharton.processphoenix.ProcessPhoenix;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // WebView for displaying web content
    private WebView webView;
    // Boolean flag for dark mode status
    private Boolean darkModeEnabled = false;
    @SuppressLint("WrongThread")
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (!ProcessPhoenix.isPhoenixProcess(this)) {
            // Initialize SentryManager for error tracking
            SentryManager sentryManager = new SentryManager(this);
            // Get SharedPreferences for storing app preferences
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            try {
                // Request necessary permissions
                if (ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED) {
                    ActivityCompat.requestPermissions(this, new String[]{POST_NOTIFICATIONS}, 1);
                }
                // Check if Sentry is enabled and initialize it
                if (sentryManager.isEnabled()) {
                    sentryManager.captureMessage("Sentry is enabled for NextDNS Manager.");
                    SentryInitializer.initialize(this);
                }
                // Setup toolbar
                setupToolbarForActivity();
                // Setup language/locale
                String appLocale = setupLanguageForActivity();
                sentryManager.captureMessage("Using locale: " + appLocale);
                // Setup dark mode
                setupDarkModeForActivity(sentryManager, sharedPreferences);
                // Setup visual indicator
                setupVisualIndicatorForActivity(sentryManager, this);
                // Setup WebView
                setupWebViewForActivity(getString(R.string.main_url));
            } catch (Exception e) {
                // Catch and log exceptions
                sentryManager.captureException(e);
            }
        }
    }

    // Cleanup when activity is destroyed
    protected void onDestroy() {
        super.onDestroy();
        webView.removeAllViews();
        webView.destroy();
    }

    // Setup toolbar for the activity
    private void setupToolbarForActivity() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }
        // Setup click listener for connection status ImageView
        ImageView imageView = findViewById(R.id.connectionStatus);
        imageView.setOnClickListener(v -> startActivity(new Intent(this, StatusActivity.class)));
    }

    // Setup language/locale for the activity
    private String setupLanguageForActivity() {
        Configuration config = getResources().getConfiguration();
        Locale appLocale = config.getLocales().get(0);
        Locale.setDefault(appLocale);
        Configuration newConfig = new Configuration(config);
        newConfig.setLocale(appLocale);
        new ContextThemeWrapper(getBaseContext(), R.style.AppTheme).applyOverrideConfiguration(newConfig);
        return appLocale.getLanguage();
    }

    // Setup dark mode for the activity based on user preference
    private void setupDarkModeForActivity(SentryManager sentryManager, SharedPreferences sharedPreferences) {
        // Retrieve the stored dark mode preference, defaulting to "match" if not found
        String darkMode = sharedPreferences.getString("dark_mode", "match");
        // Apply the appropriate dark mode setting based on the preference value
        switch (darkMode) {
            // Follow the system's dark mode setting
            case "match":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                updateDarkModeState();
                sentryManager.captureMessage("Dark mode set to follow system.");
                break;
            // Enable dark mode explicitly
            case "on":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                darkModeEnabled = true;
                sentryManager.captureMessage("Dark mode set to on.");
                break;
            // Disable dark mode due to unsupported SDK version
            case "disabled":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                darkModeEnabled = false;
                sentryManager.captureMessage("Dark mode is disabled due to SDK version.");
                break;
            // Default case to disable dark mode when preference is "off" or unrecognized
            case "off":
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                darkModeEnabled = false;
                sentryManager.captureMessage("Dark mode set to off.");
                break;
        }
    }

    // Update the internal dark mode state when following the system theme
    private void updateDarkModeState() {
        // Get the current UI mode and extract the night mode flag
        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        // Determine whether dark mode is enabled based on the night mode flag
        switch (nightModeFlags) {
            // Set darkModeEnabled to true if night mode is active
            case Configuration.UI_MODE_NIGHT_YES:
                darkModeEnabled = true;
                break;
            // Set darkModeEnabled to false if night mode is not active
            case Configuration.UI_MODE_NIGHT_NO:
                darkModeEnabled = false;
                break;
        }
    }

    // Setup visual indicator for the activity
    private void setupVisualIndicatorForActivity(SentryManager sentryManager, LifecycleOwner lifecycleOwner) {
        try {
            new VisualIndicator(this).initialize(this, lifecycleOwner, this);
        } catch (Exception e) {
            // Catch and log exceptions
            sentryManager.captureException(e);
        }
    }

    // Setup WebView for the activity
    @SuppressLint("SetJavaScriptEnabled")
    public void setupWebViewForActivity(String url) {
        webView = findViewById(R.id.webView);
        WebSettings webViewSettings = webView.getSettings();
        webViewSettings.setJavaScriptEnabled(true);
        webViewSettings.setDomStorageEnabled(true);
        webViewSettings.setDatabaseEnabled(true);
        webViewSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webViewSettings.setAllowFileAccess(false);
        webViewSettings.setAllowContentAccess(false);
        if (Boolean.TRUE.equals(darkModeEnabled)) {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.getSettings(), true);
            }
        } else {
            // Allow cookies so that user can stay logged in
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView webView, String url) {
                    CookieManager.getInstance().setAcceptCookie(true);
                    CookieManager.getInstance().acceptCookie();
                    CookieManager.getInstance().flush();
                }
            });
        }
        // Setup DownloadManager for handling file downloads
        setupDownloadManagerForActivity();
        // Load URL into WebView
        webView.loadUrl(url);
    }

    // Setup DownloadManager for handling file downloads
    private void setupDownloadManagerForActivity() {
        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url.trim()));
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, "NextDNS-Configuration.mobileconfig");
            DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            downloadManager.enqueue(request);
            Toast.makeText(getApplicationContext(), "Downloading file!", Toast.LENGTH_LONG).show();
        });
    }

    // Start new activity using intent
    private void startIntent(Class<?> targetClass) {
        Intent intent = new Intent(this, targetClass);
        startActivity(intent);
    }

    // Inflate menu for the activity
    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    // Handle menu item selection
    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.back:
                webView.goBack(); // Navigate back in WebView
                break;
            case R.id.refreshNextDNS:
                webView.reload(); // Reload content in WebView
                break;
            case R.id.pingNextDNS:
                startIntent(PingActivity.class); // Start PingActivity
                break;
            case R.id.returnHome:
                webView.loadUrl(getString(R.string.main_url)); // Load main URL in WebView
                break;
            case R.id.privateDNS:
                startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS)); // Open system settings for private DNS
                break;
            case R.id.settings:
                startIntent(SettingsActivity.class); // Start SettingsActivity
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
