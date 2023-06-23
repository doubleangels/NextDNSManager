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
import android.widget.ImageView;
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
    public DarkModeHandler darkModeHandler = new DarkModeHandler();
    public Boolean isDarkModeOn;
    private WebView webView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ITransaction MainActivity_create_transaction = Sentry.startTransaction("MainActivity_onCreate()", "MainActivity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            // Set up our window, status bar, and toolbar.
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.status_bar_background_color));
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
            toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.toolbar_background_color));

            // Set up the visual indicator.
            VisualIndicator visualIndicator = new VisualIndicator();
            visualIndicator.initiateVisualIndicator(this, getApplicationContext());

            // Let us touch the visual indicator to open an explanation.
            ImageView statusIcon = findViewById(R.id.connectionStatus);
            statusIcon.setOnClickListener(v -> {
                Intent helpIntent = new Intent(v.getContext(), help.class);
                startActivity(helpIntent);
            });

            // Get dark mode settings.
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            boolean overrideDarkMode = sharedPreferences.getBoolean(settings.OVERRIDE_DARK_MODE, false);
            boolean manualDarkMode = sharedPreferences.getBoolean(settings.MANUAL_DARK_MODE, false);
            if (overrideDarkMode) {
                isDarkModeOn = manualDarkMode;
                Sentry.setTag("overridden_dark_mode", "true");
                Sentry.addBreadcrumb("Turned on override for dark mode");
            } else {
                isDarkModeOn = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)  == Configuration.UI_MODE_NIGHT_YES;
                Sentry.setTag("overridden_dark_mode", "false");
                Sentry.addBreadcrumb("Turned off override for dark mode");

            }
            if (isDarkModeOn) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                Sentry.setTag("manual_dark_mode", "true");
                Sentry.addBreadcrumb("Set manual dark mode to true");
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                Sentry.setTag("manual_dark_mode", "false");
                Sentry.addBreadcrumb("Set manual dark mode to false");
            }

            // Provision our web view.
            provisionWebView(getString(R.string.main_url), isDarkModeOn);
        } catch (Exception e) {
            Sentry.captureException(e);
        } finally {
            MainActivity_create_transaction.finish();
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.refreshNextDNS) {
            webView.reload();
            return true;
        }
        if (item.getItemId() == R.id.pingNextDNS) {
            Intent pingIntent = new Intent(this, ping.class);
            startActivity(pingIntent);
            return true;
        }
        if (item.getItemId() == R.id.testNextDNS) {
            Intent testIntent = new Intent(this, test.class);
            startActivity(testIntent);
            return true;
        }
        if (item.getItemId() == R.id.returnHome) {
            provisionWebView(getString(R.string.main_url), isDarkModeOn);
            return true;
        }
        if (item.getItemId() == R.id.help) {
            Intent troubleshootingIntent = new Intent(this, troubleshooting.class);
            startActivity(troubleshootingIntent);
            return true;
        }
        if (item.getItemId() ==  R.id.whitelist) {
            Intent preferencesIntent = new Intent(this, whitelist.class);
            startActivity(preferencesIntent);
            return true;
        }
        if (item.getItemId() == R.id.settings) {
            Intent settingsIntent = new Intent(this, settings.class);
            startActivity(settingsIntent);
            return true;
        }
        if (item.getItemId() == R.id.about) {
            Intent aboutIntent = new Intent(this, about.class);
            startActivity(aboutIntent);
            return true;
        }
        return super.onContextItemSelected(item);
    }

    @SuppressWarnings("deprecation")
    public void replaceCSS( String url, boolean isDarkThemeOn) {
        ITransaction replace_css_transaction = Sentry.startTransaction("MainActivity_replaceCSS()", "MainActivity");
        try {
            if (isDarkThemeOn) {
                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public WebResourceResponse shouldInterceptRequest(final WebView view, String url) {
                        if (url.contains("apple.nextdns.io")) {
                            Sentry.addBreadcrumb("Visited Apple mobile configuration page");
                            return null;
                        } else if (url.contains(".css")) {
                            return getCssWebResourceResponseFromAsset();
                        } else if (url.contains("ens-text.174d0fb96836a3e4cde0338d1f9bbe36.svg")) {
                            try {
                                InputStream is = getAssets().open("ens-text.png");
                                return new WebResourceResponse("image/*", "base64", is);
                            } catch (Exception e) {
                                Sentry.captureException(e);
                                return null;
                            }
                        } else if (url.contains("unstoppabledomains.ff6c5299ea094f70f72d4276898d5cb7.svg")) {
                            try {
                                InputStream is = getAssets().open("unstoppabledomains.png");
                                return new WebResourceResponse("image/*", "base64", is);
                            } catch (Exception e) {
                                Sentry.captureException(e);
                                return null;
                            }

                        } else if (url.contains("handshake.41f677899dce13d473e16bc247dda52b.svg")) {
                            try {
                                InputStream is = getAssets().open("handshake.png");
                                return new WebResourceResponse("image/*", "base64", is);
                            } catch (Exception e) {
                                Sentry.captureException(e);
                                return null;
                            }
                        } else if (url.contains("ipfs.74e89455fca824894e90734312409fc1.svg")) {
                            try {
                                InputStream is = getAssets().open("ipfs.png");
                                return new WebResourceResponse("image/*", "base64", is);
                            } catch (Exception e) {
                                Sentry.captureException(e);
                                return null;
                            }
                        } else {
                            return super.shouldInterceptRequest(view, url);
                        }
                    }

                    private WebResourceResponse getCssWebResourceResponseFromAsset() {
                        try {
                            // Return custom CSS file from assets.
                            InputStream fileInput = getAssets().open("nextdns.css");
                            return getUtf8EncodedCssWebResourceResponse(fileInput);
                        } catch (Exception e) {
                            Sentry.captureException(e);
                            return null;
                        }
                    }

                    private WebResourceResponse getUtf8EncodedCssWebResourceResponse(InputStream fileStream) {
                        return new WebResourceResponse("text/css", "UTF-8", fileStream);
                    }
                });
            }
            // Load the webview with the URL and the custom CSS.
            webView.loadUrl(url);
        } catch (Exception e) {
            Sentry.captureException(e);
        } finally {
            replace_css_transaction.finish();
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @SuppressWarnings({"unused"})
    public void provisionWebView(String url, Boolean isDarkThemeOn) {
        ITransaction MainActivity_provision_web_view_transaction = Sentry.startTransaction("MainActivity_provisionWebView()", "MainActivity");
        try {
            webView = findViewById(R.id.mWebview);
            webView.setWebChromeClient(new WebChromeClient());
            webView.setWebViewClient(new WebViewClient());
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setDomStorageEnabled(true);
            webView.getSettings().setDatabaseEnabled(true);
            webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
            webView.setDownloadListener((url1, userAgent, contentDisposition, mimetype, contentLength) -> {
                DownloadManager.Request request = new DownloadManager.Request(
                        Uri.parse(url1));
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "NextDNS-Configuration.mobileconfig");
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                dm.enqueue(request);
                Toast.makeText(getApplicationContext(), "Downloading file!",
                        Toast.LENGTH_LONG).show();
            });
            WebSettings webSettings = webView.getSettings();
            webSettings.setAllowContentAccess(true);
            webSettings.setUseWideViewPort(true);
            webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);
            cookieManager.setAcceptThirdPartyCookies(webView, true);
            replaceCSS(url, isDarkThemeOn);
        } catch (Exception e) {
            Sentry.captureException(e);
        } finally {
            MainActivity_provision_web_view_transaction.finish();
        }
    }
}

