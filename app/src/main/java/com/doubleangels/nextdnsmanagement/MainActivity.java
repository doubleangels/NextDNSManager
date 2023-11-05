package com.doubleangels.nextdnsmanagement;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.CookieManager;
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Objects;

import io.sentry.ITransaction;
import io.sentry.Sentry;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Start a Sentry transaction for the 'onCreate' method
        ITransaction mainActivityCreateTransaction = Sentry.startTransaction("MainActivity_onCreate()", "MainActivity");
        try {
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);

            // Set up shared preferences
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

            // Set up selected language.
            String selectedLanguage = sharedPreferences.getString(SettingsActivity.SELECTED_LANGUAGE,"en");
            Sentry.setTag("locale", selectedLanguage);
            Locale appLocale;
            if (selectedLanguage.contains("pt")) {
                appLocale = new Locale(selectedLanguage, "BR");
            } else if (selectedLanguage.contains("zh")) {
                appLocale = new Locale(selectedLanguage, "HANS");
            } else {
                appLocale = new Locale(selectedLanguage);
            }
            Locale.setDefault(appLocale);
            Configuration appConfig = new Configuration();
            appConfig.locale = appLocale;
            getResources().updateConfiguration(appConfig, getResources().getDisplayMetrics());

            // Load user's preference for dark mode and set it
            boolean darkMode = sharedPreferences.getBoolean(SettingsActivity.DARK_MODE, false);
            if (darkMode) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
            setupVisualIndicator(); // Set the visual connection status indicator
            setClickListeners(); // Set click listeners for the status icon
            provisionWebView(getString(R.string.main_url), darkMode); // Load the main web page
            configureCookieManager(); // Configure cookies
        } catch (Exception e) {
            Sentry.captureException(e); // Capture and report any exceptions to Sentry
        } finally {
            mainActivityCreateTransaction.finish(); // Finish the transaction
        }
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        // Inflate the main menu
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean darkMode = sharedPreferences.getBoolean(SettingsActivity.DARK_MODE, false);

        // Handle menu item clicks, navigate to the respective activities
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

    @SuppressLint("SetJavaScriptEnabled")
    public void replaceCSS(String url, boolean isDarkThemeOn) {
        // Start a Sentry transaction for the 'replaceCSS' method
        ITransaction replaceCSSTransaction = Sentry.startTransaction("MainActivity_replaceCSS()", "MainActivity");
        try {
            setupWebViewClient(isDarkThemeOn);
            webView.loadUrl(url);
        } catch (Exception e) {
            Sentry.captureException(e); // Capture and report any exceptions to Sentry
        } finally {
            replaceCSSTransaction.finish(); // Finish the transaction
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void provisionWebView(String url, Boolean darkMode) {
        // Start a Sentry transaction for the 'provisionWebView' method
        ITransaction provisionWebViewTransaction = Sentry.startTransaction("MainActivity_provisionWebView()", "MainActivity");
        try {
            setupWebView();
            setupDownloadManager();
            configureCookieManager();
            replaceCSS(url, darkMode);
        } catch (Exception e) {
            Sentry.captureException(e); // Capture and report any exceptions to Sentry
        } finally {
            provisionWebViewTransaction.finish(); // Finish the transaction
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

        // Configure WebView settings, such as enabling JavaScript, DOM storage, and cookies
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // Enable safer WebView settings
        webSettings.setAllowFileAccess(false);
        webSettings.setAllowContentAccess(false);
        webSettings.setAllowFileAccessFromFileURLs(false);
        webSettings.setAllowUniversalAccessFromFileURLs(false);
    }

    private void setupWebViewClient(boolean isDarkThemeOn) {
        if (isDarkThemeOn) {
            // Configure the WebView client for handling web resources, including CSS
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
        // Define list of domains not to apply custom dark mode to
        String[] urlsToCheck = {
                "apple.nextdns.io", "help.nextdns.io", "bitpay.com", "github.com", "oisd.nl", "adguard.com",
                "easylist.to", "disconnect.me", "developerdan.com", "someonewhocares.org", "pgl.yoyo",
                "gitlab.com", "fanboy.co.nz", "oO.pages.dev", "mvps.org", "sysctl.org", "unchecky.com",
                "lanik.us", "280blocker.net", "shallalist.de", "github.io", "molinero.dev", "abpvn.com",
                "hostsfile.org", "firebog.net", "notabug.org", "donate.stripe.com"
        };
        boolean containsMatch = false;
        for (String urlToCheck : urlsToCheck) {
            if (url.contains(urlToCheck)) {
                containsMatch = true;
                break; // Exit the loop as soon as a match is found
            }
        }
        // Handle different types of web resource requests
        if (containsMatch) {
            return null; // Allow certain domains, skip intercepting resources
        } else if (url.endsWith(".css")) {
            return getCssWebResourceResponseFromAsset(); // Load CSS from assets
        } else if (url.contains("ens-text")) {
            return getPngWebResourceResponse("ens-text.png"); // Load PNG resource
        } else if (url.contains("unstoppabledomains")) {
            return getPngWebResourceResponse("unstoppabledomains.png"); // Load PNG resource
        } else if (url.contains("handshake")) {
            return getPngWebResourceResponse("handshake.png"); // Load PNG resource
        } else if (url.contains("ipfs")) {
            return getPngWebResourceResponse("ipfs.png"); // Load PNG resource
        } else {
            return null; // Allow other requests, skip intercepting resources
        }
    }

    @SuppressLint("NewApi")
    private WebResourceResponse getCssWebResourceResponseFromAsset() {
        try {
            InputStream fileInput = getAssets().open("nextdns.css");
            return getUtf8EncodedCssWebResourceResponse(fileInput); // Load and encode CSS from assets
        } catch (IOException e) {
            Sentry.captureException(e); // Capture and report any exceptions to Sentry
        }
        return null;
    }

    @SuppressLint("NewApi")
    private WebResourceResponse getPngWebResourceResponse(String assetFileName) {
        try {
            InputStream is = getAssets().open(assetFileName);
            return new WebResourceResponse("image/png", "UTF-8", is); // Load PNG resource
        } catch (IOException e) {
            Sentry.captureException(e); // Capture and report any exceptions to Sentry
        }
        return null;
    }

    @SuppressLint("NewApi")
    private WebResourceResponse getUtf8EncodedCssWebResourceResponse(InputStream fileStream) {
        return new WebResourceResponse("text/css", "UTF-8", fileStream); // Encode CSS
    }

    private void setupDownloadManager() {
        // Configure the WebView to handle downloads
        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

            request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, "NextDNS-Configuration.mobileconfig");

            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            dm.enqueue(request);
            Toast.makeText(getApplicationContext(), "Downloading file!", Toast.LENGTH_LONG).show();
        });
    }

    private void configureCookieManager() {
        // Configure CookieManager to accept cookies and third-party cookies
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);
    }

    private void setupVisualIndicator() {
        try {
            VisualIndicator visualIndicator = new VisualIndicator();
            visualIndicator.initiateVisualIndicator(this, getApplicationContext());
        } catch (Exception e) {
            Sentry.captureException(e); // Capture and report any exceptions to Sentry
        }
    }

    private void startIntent(Class<?> targetClass) {
        // Start a new activity based on the provided class
        Intent intent = new Intent(this, targetClass);
        startActivity(intent);
    }

    private void setClickListeners() {
        // Set a click listener for the status icon
        ImageView statusIcon = findViewById(R.id.connectionStatus);
        if (statusIcon != null) {
            statusIcon.setOnClickListener(v -> {
                Intent helpIntent = new Intent(v.getContext(), StatusActivity.class);
                startActivity(helpIntent);
            });
        }
    }
}
