package com.doubleangels.nextdnsmanagement;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import java.util.Objects;

import io.sentry.ITransaction;
import io.sentry.Sentry;

public class PingActivity extends AppCompatActivity {
    private WebView webView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Start a Sentry transaction for the 'onCreate' method
        ITransaction pingCreateTransaction = Sentry.startTransaction("ping_onCreate()", "PingActivity");
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_ping);

            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);

            // Load user's preference for dark mode and set it
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            boolean darkMode = sharedPreferences.getBoolean(SettingsActivity.DARK_MODE, false);
            if (darkMode) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
            setVisualIndicator(); // Set the visual connection status indicator

            setClickListeners(); // Set click listeners for the status icon
            provisionWebView(getString(R.string.ping_url)); // Load a web page in the WebView
        } catch (Exception e) {
            Sentry.captureException(e); // Capture and report any exceptions to Sentry
        } finally {
            pingCreateTransaction.finish(); // Finish the transaction
        }
    }

    private void setVisualIndicator() {
        try {
            VisualIndicator visualIndicator = new VisualIndicator();
            visualIndicator.initiateVisualIndicator(this, getApplicationContext());
        } catch (Exception e) {
            Sentry.captureException(e); // Capture and report any exceptions to Sentry
        }
    }

    private void setClickListeners() {
        ImageView statusIcon = findViewById(R.id.connectionStatus);
        if (statusIcon != null) {
            statusIcon.setOnClickListener(v -> {
                // Handle click on the status icon, navigate to the StatusActivity
                Intent helpIntent = new Intent(v.getContext(), StatusActivity.class);
                startActivity(helpIntent);
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        // Inflate the menu for the activity
        getMenuInflater().inflate(R.menu.menu_back_only, menu);
        return true;
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void provisionWebView(String url) {
        // Start a Sentry transaction for the 'provisionWebView' method
        ITransaction provisionWebViewTransaction = Sentry.startTransaction("ping_provisionWebView()", "PingActivity");
        try {
            if (webView == null) {
                webView = findViewById(R.id.mWebview);
                setupWebViewSettings();
            }
            webView.loadUrl(url); // Load the specified URL in the WebView
        } catch (Exception e) {
            Sentry.captureException(e); // Capture and report any exceptions to Sentry
        } finally {
            provisionWebViewTransaction.finish(); // Finish the transaction
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebViewSettings() {
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());

        // Configure WebView settings, such as enabling JavaScript, DOM storage, and cookies
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);

        // Configure CookieManager to accept cookies and third-party cookies
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.back) {
            // Handle the 'back' menu item, navigate to the MainActivity
            Intent mainIntent = new Intent(this, MainActivity.class);
            startActivity(mainIntent);
        }
        return super.onContextItemSelected(item);
    }
}
