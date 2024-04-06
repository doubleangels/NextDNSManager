package com.doubleangels.nextdnsmanagement;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.LifecycleOwner;

import com.doubleangels.nextdnsmanagement.protocoltest.VisualIndicator;
import com.doubleangels.nextdnsmanagement.sentry.SentryInitializer;
import com.doubleangels.nextdnsmanagement.sentry.SentryManager;

import java.util.Locale;
import java.util.Objects;

public class PingActivity extends AppCompatActivity {

    public SentryManager sentryManager;
    public WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ping);
        sentryManager = new SentryManager(this);
        SharedPreferences sharedPreferences = this.getSharedPreferences("preferences", Context.MODE_PRIVATE);
        try {
            if (sentryManager.isSentryEnabled()) {
                SentryInitializer sentryInitializer = new SentryInitializer();
                sentryInitializer.execute(this);
            }
            setupToolbar();
            setupLanguage();
            setupDarkMode(sharedPreferences);
            setupVisualIndicator(sentryManager, this);
            setupWebView(getString(R.string.ping_url));
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        webView.removeAllViews();
        webView.destroy();
    }

    private void setupToolbar() {
        setSupportActionBar(findViewById(R.id.toolbar));
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
    }

    private void setupLanguage() {
        Resources resources = getResources();
        Configuration configuration = resources.getConfiguration();
        Locale appLocale = configuration.getLocales().get(0);
        if (appLocale != null) {
            Locale.setDefault(appLocale);
            configuration.setLocale(appLocale);
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        }
    }

    private void setupDarkMode(SharedPreferences sharedPreferences) {
        String darkModeOverride = sharedPreferences.getString("darkmode_override", "match");
        if (darkModeOverride.contains("match")) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        } else if (darkModeOverride.contains("on")) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void setupVisualIndicator(SentryManager sentryManager, LifecycleOwner lifecycleOwner) {
        try {
            new VisualIndicator(this).initiateVisualIndicator(this, lifecycleOwner, this);
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void setupWebView(String url) {
        webView = findViewById(R.id.webView);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        webSettings.setAllowFileAccess(false);
        webSettings.setAllowContentAccess(false);
        webSettings.setAllowUniversalAccessFromFileURLs(false);
        webSettings.setSaveFormData(true);
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl(url);
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu_back_only, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.back) {
            Intent mainIntent = new Intent(this, MainActivity.class);
            startActivity(mainIntent);
        }
        return super.onContextItemSelected(item);
    }
}
