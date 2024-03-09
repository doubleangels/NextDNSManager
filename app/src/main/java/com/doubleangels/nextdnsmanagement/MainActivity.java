package com.doubleangels.nextdnsmanagement;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import com.doubleangels.nextdnsmanagement.protocoltest.VisualIndicator;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.sentry.ITransaction;
import io.sentry.Sentry;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private Boolean darkMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ITransaction mainActivityCreateTransaction = Sentry.startTransaction("MainActivity_onCreate()", "MainActivity");
        try {
            setupToolbar();
            setupLanguage();
            setupDarkMode();
            setupVisualIndicator();
            setupClickListeners();
            provisionWebView(getString(R.string.main_url), darkMode);
        } catch (Exception e) {
            Sentry.captureException(e);
        } finally {
            mainActivityCreateTransaction.finish();
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
    }

    private void setupLanguage() {
        String appLocaleString = getResources().getConfiguration().getLocales().get(0).toString();
        String appLocaleStringResult = appLocaleString.split("_")[0];
        Locale appLocale = Locale.forLanguageTag(appLocaleStringResult);
        Locale.setDefault(appLocale);
        Configuration appConfig = new Configuration();
        appConfig.locale = appLocale;
        getResources().updateConfiguration(appConfig, getResources().getDisplayMetrics());
    }

    private void setupDarkMode() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        darkMode = currentNightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    private void setupVisualIndicator() {
        try {
            VisualIndicator visualIndicator = new VisualIndicator();
            visualIndicator.initiateVisualIndicator(this, getApplicationContext());
        } catch (Exception e) {
            Sentry.captureException(e);
        }
    }

    private void setupClickListeners() {
        ImageView statusIcon = findViewById(R.id.connectionStatus);
        if (statusIcon != null) {
            statusIcon.setOnClickListener(v -> {
                Intent helpIntent = new Intent(v.getContext(), StatusActivity.class);
                startActivity(helpIntent);
            });
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void provisionWebView(String url, Boolean darkMode) {
        try {
            setupWebView();
            setupDownloadManager();
            replaceCSS(url, darkMode);
        } catch (Exception e) {
            Sentry.captureException(e);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        webView = findViewById(R.id.mWebview);
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
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());
    }

    private void setupDownloadManager() {
        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url.trim()));
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, "NextDNS-Configuration.mobileconfig");
            DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            downloadManager.enqueue(request);
            Toast.makeText(getApplicationContext(), "Downloading file!", Toast.LENGTH_LONG).show();
        });
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

    @SuppressLint("SetJavaScriptEnabled")
    public void replaceCSS(String url, boolean isDarkThemeOn) {
        try {
            setupWebViewClient(isDarkThemeOn);
            webView.loadUrl(url);
        } catch (Exception e) {
            Sentry.captureException(e);
        }
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
            case R.id.testNextDNS -> startIntent(TestActivity.class);
            case R.id.returnHome -> provisionWebView(getString(R.string.main_url), darkMode);
            case R.id.settings -> startIntent(SettingsActivity.class);
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("NewApi")
    private WebResourceResponse handleWebResourceRequests(String url) {
        Set<String> allowedDomains = new HashSet<>(Arrays.asList(
                "apple.nextdns.io", "help.nextdns.io", "bitpay.com", "github.com", "oisd.nl", "adguard.com",
                "easylist.to", "disconnect.me", "developerdan.com", "someonewhocares.org", "pgl.yoyo",
                "gitlab.com", "fanboy.co.nz", "oO.pages.dev", "mvps.org", "sysctl.org", "unchecky.com",
                "lanik.us", "280blocker.net", "shallalist.de", "github.io", "molinero.dev", "abpvn.com",
                "hostsfile.org", "firebog.net", "notabug.org", "donate.stripe.com"
        ));
        for (String domain : allowedDomains) {
            if (url.contains(domain)) {
                return null;
            }
        }
        Map<String, String> resourceMap = new HashMap<>();
        resourceMap.put(".css", "styles.css");
        resourceMap.put("ens-text", "ens-text.png");
        resourceMap.put("unstoppabledomains", "unstoppabledomains.png");
        resourceMap.put("handshake", "handshake.png");
        resourceMap.put("ipfs", "ipfs.png");
        for (Map.Entry<String, String> entry : resourceMap.entrySet()) {
            if (url.contains(entry.getKey())) {
                if (entry.toString().contains(".css")) {
                    return getCssWebResourceResponseFromAsset();
                }
                return getPngWebResourceResponse(entry.getValue());
            }
        }

        return null;
    }

    @SuppressLint("NewApi")
    private WebResourceResponse getCssWebResourceResponseFromAsset() {
        try {
            InputStream fileInput = getAssets().open("nextdns.css");
            return getUtf8EncodedCssWebResourceResponse(fileInput);
        } catch (IOException e) {
            Sentry.captureException(e);
        }
        return null;
    }

    @SuppressLint("NewApi")
    private WebResourceResponse getPngWebResourceResponse(String assetFileName) {
        try {
            InputStream is = getAssets().open(assetFileName);
            return new WebResourceResponse("image/png", "UTF-8", is);
        } catch (IOException e) {
            Sentry.captureException(e);
        }
        return null;
    }

    @SuppressLint("NewApi")
    private WebResourceResponse getUtf8EncodedCssWebResourceResponse(InputStream fileStream) {
        return new WebResourceResponse("text/css", "UTF-8", fileStream);
    }
}
