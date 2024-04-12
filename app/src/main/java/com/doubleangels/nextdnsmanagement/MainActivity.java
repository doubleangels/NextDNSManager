package com.doubleangels.nextdnsmanagement;

import static android.Manifest.permission.POST_NOTIFICATIONS;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
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
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
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

import com.doubleangels.nextdnsmanagement.protocoltest.VisualIndicator;
import com.doubleangels.nextdnsmanagement.sentry.SentryInitializer;
import com.doubleangels.nextdnsmanagement.sentry.SentryManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/** @noinspection deprecation*/
public class MainActivity extends AppCompatActivity {

    private SentryManager sentryManager;
    private WebView webView;
    private Boolean darkMode;

    @SuppressLint("WrongThread")
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sentryManager = new SentryManager(this);
        SharedPreferences sharedPreferences = this.getSharedPreferences("preferences", Context.MODE_PRIVATE);
        try {
            if (ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(this, new String[]{POST_NOTIFICATIONS}, 1);
            }
            if (sentryManager.isEnabled()) {
                sentryManager.captureMessage("Sentry is enabled for NextDNS Manager.");
                SentryInitializer.initialize(this);
            }
            setupToolbarForActivity();
            String appLocale = setupLanguageForActivity();
            sentryManager.captureMessage("Using locale: " + appLocale);
            setupDarkModeForActivity(sentryManager, sharedPreferences);
            setupVisualIndicatorForActivity(sentryManager, this);
            setupWebViewForActivity(getString(R.string.main_url));
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        webView.removeAllViews();
        webView.destroy();
    }

    private void setupToolbarForActivity() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }
        ImageView imageView = findViewById(R.id.connectionStatus);
        imageView.setOnClickListener(v -> startActivity(new Intent(this, StatusActivity.class)));
    }

    private String setupLanguageForActivity() {
        Configuration config = getResources().getConfiguration();
        Locale appLocale = config.getLocales().get(0);
        Locale.setDefault(appLocale);
        Configuration newConfig = new Configuration(config);
        newConfig.setLocale(appLocale);
        new ContextThemeWrapper(getBaseContext(), R.style.AppTheme).applyOverrideConfiguration(newConfig);
        return appLocale.getLanguage();
    }

    private void setupDarkModeForActivity(SentryManager sentryManager, SharedPreferences sharedPreferences) {
        String darkMode = sharedPreferences.getString("dark_mode", "match");
        if (darkMode.contains("match")) {
            sentryManager.setTag("dark_mode", "match");
            sentryManager.captureMessage("Dark mode set to match system.");
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            this.darkMode = currentNightMode == Configuration.UI_MODE_NIGHT_YES;
        } else if (darkMode.contains("on")) {
            sentryManager.setTag("dark_mode", "on");
            sentryManager.captureMessage("Dark mode set to on.");
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            this.darkMode = true;
        } else {
            sentryManager.setTag("dark_mode", "off");
            sentryManager.captureMessage("Dark mode set to off.");
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            this.darkMode = false;
        }
    }

    private void setupVisualIndicatorForActivity(SentryManager sentryManager, LifecycleOwner lifecycleOwner) {
        try {
            new VisualIndicator(this).initialize(this, lifecycleOwner, this);
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void setupWebViewForActivity(String url) {
        webView = findViewById(R.id.webView);
        WebSettings webViewSettings = webView.getSettings();
        webViewSettings.setJavaScriptEnabled(true);
        webViewSettings.setDomStorageEnabled(true);
        webViewSettings.setDatabaseEnabled(true);
        webViewSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webViewSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        webViewSettings.setAllowFileAccess(false);
        webViewSettings.setAllowContentAccess(false);
        webViewSettings.setAllowUniversalAccessFromFileURLs(false);
        if (darkMode) {
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                    if (request.getUrl().toString().endsWith(".css")) {
                        try {
                            InputStream inputStream = getAssets().open("minimized-full.css");
                            String cssContent = convertStreamToString(inputStream);
                            return new WebResourceResponse("text/css", "UTF-8", new ByteArrayInputStream(cssContent.getBytes()));
                        } catch (IOException e) {
                            sentryManager.captureMessage("Error loading CSS from assets:" + e);
                        }
                    }
                    return super.shouldInterceptRequest(view, request);
                }
            });
        } else {
            webView.setWebViewClient(new WebViewClient());
        }
        setupDownloadManagerForActivity();
        webView.loadUrl(url);
    }

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
    private String convertStreamToString(InputStream is) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = is.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString("UTF-8");
    }

    private void startIntent(Class<?> targetClass) {
        Intent intent = new Intent(this, targetClass);
        startActivity(intent);
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
            case R.id.back -> webView.goBack();
            case R.id.refreshNextDNS -> webView.reload();
            case R.id.pingNextDNS -> startIntent(PingActivity.class);
            case R.id.returnHome -> webView.loadUrl(getString(R.string.main_url));
            case R.id.privateDNS  -> startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
            case R.id.settings -> startIntent(SettingsActivity.class);
        }
        return super.onOptionsItemSelected(item);
    }
}