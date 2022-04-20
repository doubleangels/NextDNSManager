package com.doubleangels.nextdnsmanagement;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

import io.sentry.ISpan;
import io.sentry.ITransaction;
import io.sentry.Sentry;

public class MainActivity extends AppCompatActivity {

    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private FirebaseAnalytics mFirebaseAnalytics;
    private WebView webView;
    private Boolean useCustomCSS;
    private Double cacheTime;

    @Override
    @AddTrace(name = "MainActivity_create")
    protected void onCreate(Bundle savedInstanceState) {
        ITransaction MainActivity_create_transaction = Sentry.startTransaction("onCreate()", "MainActivity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {

            final SharedPreferences sharedPreferences = getSharedPreferences("mainSharedPreferences", MODE_PRIVATE);
            boolean isManualDisableAnalytics = sharedPreferences.getBoolean("manualDisableAnalytics", false);
            String storedUniqueKey = sharedPreferences.getString("uuid", "defaultValue");
            String uniqueKey;
            if (storedUniqueKey.contains("defaultValue")) {
                uniqueKey = UUID.randomUUID().toString();
                sharedPreferences.edit().putString("uuid", uniqueKey).apply();
                FirebaseCrashlytics.getInstance().setUserId(uniqueKey);
                Sentry.setTag("uuid", uniqueKey);
                Sentry.setTag("uuid_set", "true");
                Sentry.setTag("uuid_new", "true");
            } else {
                uniqueKey = sharedPreferences.getString("uuid", "defaultValue");
                FirebaseCrashlytics.getInstance().setUserId(uniqueKey);
                Sentry.setTag("uuid", uniqueKey);
                Sentry.setTag("uuid_set", "true");
                Sentry.setTag("uuid_new", "false");
            }

            mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
            if (isManualDisableAnalytics) {
                FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(true);
                Sentry.setTag("analytics", "true");
            }

            Trace remoteConfigStartTrace = FirebasePerformance.getInstance().newTrace("remoteConfig_setup");
            remoteConfigStartTrace.start();
            mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
            FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(1800).build();
            mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);
            mFirebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);
            remoteConfigStartTrace.stop();

            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
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
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.status_bar_background_color));
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
            toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.toolbar_background_color));
            useCustomCSS = mFirebaseRemoteConfig.getBoolean("use_custom_css");
            if (useCustomCSS) {
                Sentry.setTag("custom_css", "true");
            }

            boolean isDarkThemeOn = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            if (isDarkThemeOn) {
                Sentry.setTag("dark_mode_on", " true");
            }

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

            ImageView statusIcon = findViewById(R.id.connectionStatus);
            statusIcon.setOnClickListener(v -> {
                Bundle bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "help_icon");
                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
                Intent helpIntent = new Intent(v.getContext(), help.class);
                startActivity(helpIntent);
            });

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
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
            webView.reload();
            return true;
        }
        if (item.getItemId() == R.id.pingNextDNS) {
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "ping_icon");
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
            Intent pingIntent = new Intent(this, ping.class);
            startActivity(pingIntent);
            return true;
        }
        if (item.getItemId() == R.id.testNextDNS) {
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "test_icon");
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
            Intent testIntent = new Intent(this, test.class);
            startActivity(testIntent);
            return true;
        }
        if (item.getItemId() == R.id.returnHome) {
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "home_icon");
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
            provisionWebView(getString(R.string.main_url), isDarkThemeOn, useCustomCSS);
            return true;
        }
        if (item.getItemId() == R.id.help) {
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "help_icon");
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
            Intent troubleshootingIntent = new Intent(this, troubleshooting.class);
            startActivity(troubleshootingIntent);
            return true;
        }
        if (item.getItemId() ==  R.id.settings) {
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "settings_icon");
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
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
                            File checkFile = new File(getCacheDir(), "nextdns.css");
                            cacheTime = mFirebaseRemoteConfig.getDouble("cache_time");
                            if (checkFile.exists()) {
                                long diff = new Date().getTime() - checkFile.lastModified();
                                if (diff > cacheTime * 86400000) {
                                    checkFile.delete();
                                    InputStream fileStream = new URL(getString(R.string.css_url)).openStream();
                                    File file = new File(getCacheDir(), "nextdns.css");
                                    writeStreamToFile(fileStream, file);
                                    FileInputStream fileInput = new FileInputStream(new File(getCacheDir(), "nextdns.css"));
                                    return getUtf8EncodedCssWebResourceResponse(fileInput);
                                } else {
                                    FileInputStream fileInput = new FileInputStream(new File(getCacheDir(), "nextdns.css"));
                                    return getUtf8EncodedCssWebResourceResponse(fileInput);
                                }
                            } else {
                                InputStream fileStream = new URL(getString(R.string.css_url)).openStream();
                                File file = new File(getCacheDir(), "nextdns.css");
                                writeStreamToFile(fileStream, file);
                                FileInputStream fileInput = new FileInputStream(new File(getCacheDir(), "nextdns.css"));
                                return getUtf8EncodedCssWebResourceResponse(fileInput);
                            }
                        } catch (Exception e) {
                            Sentry.captureException(e);
                            FirebaseCrashlytics.getInstance().recordException(e);
                            return null;
                        }
                    }

                    private WebResourceResponse getUtf8EncodedCssWebResourceResponse(InputStream fileStream) {
                        return new WebResourceResponse("text/css", "UTF-8", fileStream);
                    }

                    void writeStreamToFile(InputStream input, File file) {
                        ITransaction MainActivity_write_stream_to_file_transaction = Sentry.startTransaction("writeStreamToFile()", "MainActivity");
                        try {
                            try (OutputStream output = new FileOutputStream(file)) {
                                byte[] buffer = new byte[4 * 1024];
                                int read;
                                while ((read = input.read(buffer)) != -1) {
                                    output.write(buffer, 0, read);
                                }
                                output.flush();
                            }
                        } catch (Exception e) {
                            Sentry.captureException(e);
                            FirebaseCrashlytics.getInstance().recordException(e);
                        } finally {
                            MainActivity_write_stream_to_file_transaction.finish();
                        }
                    }
                });
            }
            webView.loadUrl(url);
        } catch (Exception e) {
            Sentry.captureException(e);
            FirebaseCrashlytics.getInstance().recordException(e);
        } finally {
            replace_css_transaction.finish();
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @AddTrace(name = "MainActivity_provision_web_view")
    public void provisionWebView(String url, Boolean isDarkThemeOn, Boolean useCustomCSS) {
        ITransaction MainActivity_provision_web_view_transaction = Sentry.startTransaction("provisionWebView()", "MainActivity");
        try {
            webView = findViewById(R.id.mWebview);
            webView.setWebChromeClient(new WebChromeClient());
            webView.setWebViewClient(new WebViewClient());
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
                        if (linkProperties.getPrivateDnsServerName().contains("nextdns")) {
                            ImageView connectionStatus = findViewById(R.id.connectionStatus);
                            connectionStatus.setImageResource(R.drawable.success);
                            connectionStatus.setColorFilter(ContextCompat.getColor(context, R.color.green));
                            Sentry.setTag("private_dns", "nextdns");
                        } else {
                            ImageView connectionStatus = findViewById(R.id.connectionStatus);
                            connectionStatus.setImageResource(R.drawable.success);
                            connectionStatus.setColorFilter(ContextCompat.getColor(context, R.color.yellow));
                            Sentry.setTag("private_dns", "private");
                        }
                    } else {
                        ImageView connectionStatus = findViewById(R.id.connectionStatus);
                        connectionStatus.setImageResource(R.drawable.success);
                        connectionStatus.setColorFilter(ContextCompat.getColor(context, R.color.yellow));
                        Sentry.setTag("private_dns", "private");
                    }
                } else {
                    ImageView connectionStatus = findViewById(R.id.connectionStatus);
                    connectionStatus.setImageResource(R.drawable.failure);
                    connectionStatus.setColorFilter(ContextCompat.getColor(context, R.color.red));
                    Sentry.setTag("private_dns", "insecure");
                }
            } else {
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