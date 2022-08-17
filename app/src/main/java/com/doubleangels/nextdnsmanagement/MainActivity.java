package com.doubleangels.nextdnsmanagement;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import java.io.InputStream;
import java.util.Objects;

import io.sentry.ITransaction;
import io.sentry.Sentry;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    public ExceptionHandler exceptionHandler = new ExceptionHandler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ITransaction MainActivity_create_transaction = Sentry.startTransaction("onCreate()", "MainActivity");
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

            // Determine if dark theme is on.
            boolean isDarkThemeOn = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            if (isDarkThemeOn) {
                Sentry.setTag("dark_mode_on", " true");
            }

            // Provision our web view.
            provisionWebView(getString(R.string.main_url), isDarkThemeOn);
        } catch (Exception e) {
            exceptionHandler.captureExceptionAndFeedback(e, this);
        } finally {
            MainActivity_create_transaction.finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean isDarkThemeOn = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)  == Configuration.UI_MODE_NIGHT_YES;
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
            provisionWebView(getString(R.string.main_url), isDarkThemeOn);
            return true;
        }
        if (item.getItemId() == R.id.help) {
            Intent troubleshootingIntent = new Intent(this, troubleshooting.class);
            startActivity(troubleshootingIntent);
            return true;
        }
        if (item.getItemId() ==  R.id.preferences) {
            Intent preferencesIntent = new Intent(this, preferences.class);
            startActivity(preferencesIntent);
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
        ITransaction replace_css_transaction = Sentry.startTransaction("replaceCSS()", "MainActivity");
        try {
            if (isDarkThemeOn) {
                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public WebResourceResponse shouldInterceptRequest(final WebView view, String url) {
                        if (url.contains(".css")) {
                            return getCssWebResourceResponseFromAsset();
                        } if (url.contains("ens-image.010effe074fead9f3c3fc3fdd87b2260.svg")) {
                            return getSvgWebResourceResponseFromAsset();
                        } else {
                            return super.shouldInterceptRequest(view, url);
                        }
                    }

                    private WebResourceResponse getSvgWebResourceResponseFromAsset() {
                        try {
                            // Return custom CSS file from assets.
                            InputStream fileInput = getAssets().open("ens.svg");
                            return getUtf8EncodedSvgWebResourceResponse(fileInput);
                        } catch (Exception e) {
                            Sentry.captureException(e);
                            return null;
                        }
                    }

                    private WebResourceResponse getUtf8EncodedSvgWebResourceResponse(InputStream fileStream) {
                        return new WebResourceResponse("image/svg+xml", "UTF-8", fileStream);
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
            exceptionHandler.captureExceptionAndFeedback(e, this);
        } finally {
            replace_css_transaction.finish();
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @SuppressWarnings({"unused"})
    public void provisionWebView(String url, Boolean isDarkThemeOn) {
        ITransaction MainActivity_provision_web_view_transaction = Sentry.startTransaction("provisionWebView()", "MainActivity");
        try {
            webView = findViewById(R.id.mWebview);
            webView.setWebChromeClient(new WebChromeClient());
            webView.setWebViewClient(new WebViewClient());
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setDomStorageEnabled(true);
            webView.getSettings().setDatabaseEnabled(true);
            webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            WebSettings webSettings = webView.getSettings();
            webSettings.setAllowContentAccess(true);
            webSettings.setUseWideViewPort(true);
            webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);

            replaceCSS(url, isDarkThemeOn);
        } catch (Exception e) {
            exceptionHandler.captureExceptionAndFeedback(e, this);
        } finally {
            MainActivity_provision_web_view_transaction.finish();
        }
    }
}