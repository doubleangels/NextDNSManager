package com.doubleangels.nextdnsmanagement;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
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

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.AddTrace;
import com.google.firebase.perf.metrics.Trace;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.io.InputStream;
import java.util.Objects;

import io.sentry.ISpan;
import io.sentry.ITransaction;
import io.sentry.Sentry;

public class MainActivity extends AppCompatActivity {

    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private WebView webView;
    private Boolean useCustomCSS;

    @Override
    @AddTrace(name = "MainActivity_create")
    protected void onCreate(Bundle savedInstanceState) {
        ITransaction MainActivity_create_transaction = Sentry.startTransaction("onCreate()", "MainActivity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            // Get our remote configuration information.
            Trace remoteConfigStartTrace = FirebasePerformance.getInstance().newTrace("remoteConfig_setup");
            remoteConfigStartTrace.start();
            mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
            FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(1800).build();
            mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);
            mFirebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);
            remoteConfigStartTrace.stop();
            Trace remoteConfigFetchTrace = FirebasePerformance.getInstance().newTrace("remoteConfig_fetch");
            remoteConfigFetchTrace.start();
            mFirebaseRemoteConfig.fetchAndActivate().addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    boolean updated = task.getResult();
                    if (updated) {
                        Sentry.setTag("remote_config_fetched", "true");
                    } else {
                        Sentry.setTag("remote_config_fetched", "false");
                    }
                    mFirebaseRemoteConfig.activate();
                }
            });
            remoteConfigFetchTrace.stop();

            // Determine if we're using custom CSS.
            useCustomCSS = mFirebaseRemoteConfig.getBoolean("use_custom_css");
            if (useCustomCSS) {
                Sentry.setTag("custom_css", "true");
            }

            // Determine if dark theme is on.
            boolean isDarkThemeOn = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            if (isDarkThemeOn) {
                Sentry.setTag("dark_mode_on", " true");
            }

            // Set up our window, status bar, and toolbar.
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.status_bar_background_color));
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
            toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.toolbar_background_color));

            // Check if we're using private DNS and watch DNS type over time to change visual indicator.
            ConnectivityManager connectivityManager = (ConnectivityManager) this.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            Network network = connectivityManager.getActiveNetwork();
            LinkProperties linkProperties = connectivityManager.getLinkProperties(network);
            updateVisualIndicator(linkProperties, networkInfo, getApplicationContext());
            connectivityManager.registerNetworkCallback(new NetworkRequest.Builder().build(), new ConnectivityManager.NetworkCallback() {
                @Override
                public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
                    super.onLinkPropertiesChanged(network, linkProperties);
                    updateVisualIndicator(linkProperties, networkInfo, getApplicationContext());
                }
            });

            // Let us touch the visual indicator to open an explanation.
            ImageView statusIcon = findViewById(R.id.connectionStatus);
            statusIcon.setOnClickListener(v -> {
                Bundle bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "help_icon");
                Intent helpIntent = new Intent(v.getContext(), help.class);
                startActivity(helpIntent);
            });

            // Provision our web view.
            provisionWebView(getString(R.string.main_url), isDarkThemeOn, useCustomCSS);
        } catch (Exception e) {
            Sentry.captureException(e);
            FirebaseCrashlytics.getInstance().recordException(e);
        } finally {
            MainActivity_create_transaction.finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean isDarkThemeOn = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)  == Configuration.UI_MODE_NIGHT_YES;
        Bundle bundle = new Bundle();
        if (item.getItemId() == R.id.refreshNextDNS) {
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "refresh_icon");
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
            provisionWebView(getString(R.string.main_url), isDarkThemeOn, useCustomCSS);
            return true;
        }
        if (item.getItemId() == R.id.help) {
            Intent troubleshootingIntent = new Intent(this, troubleshooting.class);
            startActivity(troubleshootingIntent);
            return true;
        }
        if (item.getItemId() ==  R.id.settings) {
            Intent settingsIntent = new Intent(this, settings.class);
            startActivity(settingsIntent);
            return true;
        }
        return super.onContextItemSelected(item);
    }

    @SuppressWarnings("deprecation")
    @AddTrace(name = "replace_css")
    public void replaceCSS( String url, boolean isDarkThemeOn) {
        ITransaction replace_css_transaction = Sentry.startTransaction("replaceCSS()", "MainActivity");
        try {
            if (isDarkThemeOn) {
                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public WebResourceResponse shouldInterceptRequest(final WebView view, String url) {
                        if (url.contains(".css")) {
                            return getCssWebResourceResponseFromAsset();
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
                            FirebaseCrashlytics.getInstance().recordException(e);
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
            FirebaseCrashlytics.getInstance().recordException(e);
        } finally {
            replace_css_transaction.finish();
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @SuppressWarnings("unused")
    @AddTrace(name = "MainActivity_provision_web_view")
    public void provisionWebView(String url, Boolean isDarkThemeOn, Boolean useCustomCSS) {
        ITransaction MainActivity_provision_web_view_transaction = Sentry.startTransaction("provisionWebView()", "MainActivity");
        try {
            webView = findViewById(R.id.mWebview);
            webView.setWebChromeClient(new WebChromeClient());
            webView.setWebViewClient(new WebViewClient() {
                public void onPageError() {

                }
            });
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setDomStorageEnabled(true);
            webView.getSettings().setAppCacheEnabled(true);
            webView.getSettings().setAppCachePath(String.valueOf(getApplicationContext().getCacheDir()));
            webView.getSettings().setDatabaseEnabled(true);
            webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            WebSettings webSettings = webView.getSettings();
            webSettings.setAllowContentAccess(true);
            webSettings.setUseWideViewPort(true);
            webSettings.setAppCachePath(getApplicationContext().getCacheDir().toString());
            webSettings.setAppCacheEnabled(true);
            webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);

            // If we're using custom CSS, don't use the built in dark mode.
            if (useCustomCSS) {
                replaceCSS(url, isDarkThemeOn);
            } else {
                ISpan force_dark_mode_span = MainActivity_provision_web_view_transaction.startChild("force_dark_mode");
                int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
                    if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
                        Sentry.setTag("force_dark_mode_strategy_supported", "true");
                        WebSettingsCompat.setForceDarkStrategy(webView.getSettings(), WebSettingsCompat.DARK_STRATEGY_PREFER_WEB_THEME_OVER_USER_AGENT_DARKENING);
                    }
                    if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                        Sentry.setTag("force_dark_mode_supported", "true");
                        WebSettingsCompat.setForceDark(webView.getSettings(), WebSettingsCompat.FORCE_DARK_ON);
                    }
                }
                webView.loadUrl(url);
                force_dark_mode_span.finish();
            }
        } catch (Exception e) {
            Sentry.captureException(e);
            FirebaseCrashlytics.getInstance().recordException(e);
        } finally {
            MainActivity_provision_web_view_transaction.finish();
        }
    }

    @AddTrace(name = "update_visual_indicator")
    public void updateVisualIndicator(LinkProperties linkProperties, NetworkInfo networkInfo, Context context) {
        ITransaction update_visual_indicator_transaction = Sentry.startTransaction("updateVisualIndicator()", "help");
        try {
            if (networkInfo != null) {
                if (linkProperties.isPrivateDnsActive()) {
                    if (linkProperties.getPrivateDnsServerName() != null) {
                        // If we're connected to NextDNS, show green.
                        if (linkProperties.getPrivateDnsServerName().contains("nextdns")) {
                            ImageView connectionStatus = findViewById(R.id.connectionStatus);
                            connectionStatus.setImageResource(R.drawable.success);
                            connectionStatus.setColorFilter(ContextCompat.getColor(context, R.color.green));
                            Sentry.setTag("private_dns", "nextdns");
                        } else {
                            // If we're connected to private DNS but not NextDNS, show yellow.
                            ImageView connectionStatus = findViewById(R.id.connectionStatus);
                            connectionStatus.setImageResource(R.drawable.success);
                            connectionStatus.setColorFilter(ContextCompat.getColor(context, R.color.yellow));
                            Sentry.setTag("private_dns", "private");
                        }
                    } else {
                        // If we're connected to private DNS but not NextDNS, show yellow.
                        ImageView connectionStatus = findViewById(R.id.connectionStatus);
                        connectionStatus.setImageResource(R.drawable.success);
                        connectionStatus.setColorFilter(ContextCompat.getColor(context, R.color.yellow));
                        Sentry.setTag("private_dns", "private");
                    }
                } else {
                    // If we're not using private DNS, show red.
                    ImageView connectionStatus = findViewById(R.id.connectionStatus);
                    connectionStatus.setImageResource(R.drawable.failure);
                    connectionStatus.setColorFilter(ContextCompat.getColor(context, R.color.red));
                    Sentry.setTag("private_dns", "insecure");
                }
            } else {
                // If we have no internet connection, show gray.
                ImageView connectionStatus = findViewById(R.id.connectionStatus);
                connectionStatus.setImageResource(R.drawable.failure);
                connectionStatus.setColorFilter(ContextCompat.getColor(context, R.color.gray));
                Sentry.setTag("private_dns", "no_connection");
            }
        } catch (Exception e) {
            Sentry.captureException(e);
            FirebaseCrashlytics.getInstance().recordException(e);
        } finally {
            update_visual_indicator_transaction.finish();
        }
    }
}