package com.doubleangels.nextdnsmanagement;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import java.util.Objects;

import io.sentry.ITransaction;
import io.sentry.Sentry;

public class TestActivity extends AppCompatActivity {
    private final DarkModeHandler darkModeHandler = new DarkModeHandler();
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ITransaction testCreateTransaction = Sentry.startTransaction("test_onCreate()", "TestActivity");
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_test);

            // Get shared preferences for settings
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

            // Initialize views and set up the action bar
            initializeViews(sharedPreferences);
            // Set a visual indicator for the activity
            setVisualIndicator();
            // Set click listeners for views
            setClickListeners();
            // Provision the WebView with a URL
            provisionWebView(getString(R.string.test_url));
        } catch (Exception e) {
            // Capture and report any exceptions to Sentry
            Sentry.captureException(e);
        } finally {
            // Finish the Sentry transaction
            testCreateTransaction.finish();
        }
    }

    // Method to initialize views, typically used for fragments
    private void initializeViews(SharedPreferences sharedPreferences) {
        boolean darkNavigation = sharedPreferences.getBoolean(SettingsActivity.DARK_NAVIGATION, false);
        setWindowAndToolbar(darkNavigation);
    }

    // Method to set window and toolbar styles based on dark mode settings
    private void setWindowAndToolbar(boolean isDark) {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        int statusBarColor = isDark ? R.color.darkgray : R.color.blue;

        window.setStatusBarColor(ContextCompat.getColor(this, statusBarColor));
        window.setNavigationBarColor(ContextCompat.getColor(this, statusBarColor));

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        toolbar.setBackgroundColor(ContextCompat.getColor(this, statusBarColor));
    }

    // Method to set up a visual indicator
    private void setVisualIndicator() {
        try {
            VisualIndicator visualIndicator = new VisualIndicator();
            visualIndicator.initiateVisualIndicator(this, getApplicationContext());
        } catch (Exception e) {
            Sentry.captureException(e);
        }
    }

    // Method to set click listeners for views
    private void setClickListeners() {
        ImageView statusIcon = findViewById(R.id.connectionStatus);
        statusIcon.setOnClickListener(v -> {
            Intent helpIntent = new Intent(v.getContext(), HelpActivity.class);
            startActivity(helpIntent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            darkModeHandler.handleDarkMode(this);
        } catch (Exception e) {
            Sentry.captureException(e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu_back_only, menu);
        return true;
    }

    @SuppressLint("SetJavaScriptEnabled")
    @SuppressWarnings("unused")
    public void provisionWebView(String url) {
        ITransaction testProvisionWebViewTransaction = Sentry.startTransaction("test_provisionWebView()", "TestActivity");
        try {
            if (webView == null) {
                webView = findViewById(R.id.mWebview);
                setupWebViewSettings();
            }
            webView.loadUrl(url);
        } catch (Exception e) {
            Sentry.captureException(e);
        } finally {
            testProvisionWebViewTransaction.finish();
        }
    }

    // Method to set up WebView settings
    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebViewSettings() {
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.back) {
            Intent mainIntent = new Intent(this, MainActivity.class);
            startActivity(mainIntent);
        }
        return super.onOptionsItemSelected(item);
    }
}
