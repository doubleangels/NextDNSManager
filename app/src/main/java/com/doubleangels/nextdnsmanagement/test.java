package com.doubleangels.nextdnsmanagement;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.AddTrace;
import com.google.firebase.perf.metrics.Trace;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import java.util.UUID;

public class test extends AppCompatActivity {

    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private FirebaseAnalytics mFirebaseAnalytics;
    private WebView webView;
    private Window window;
    private Toolbar toolbar;
    private ImageView statusIcon;
    private String storedUniqueKey;
    private String uniqueKey;
    private Boolean isManualDisableAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        try {
            final SharedPreferences sharedPreferences = getSharedPreferences("mainSharedPreferences", MODE_PRIVATE);
            isManualDisableAnalytics = sharedPreferences.getBoolean("manualDisableAnalytics", false);
            storedUniqueKey = sharedPreferences.getString("uuid", "defaultValue");
            if (storedUniqueKey.contains("defaultValue")) {
                uniqueKey = UUID.randomUUID().toString();
                sharedPreferences.edit().putString("uuid", uniqueKey).apply();
                FirebaseCrashlytics.getInstance().setUserId(uniqueKey);
                FirebaseCrashlytics.getInstance().log("Set UUID to: " + uniqueKey);
            } else {
                uniqueKey = sharedPreferences.getString("uuid", "defaultValue");
                FirebaseCrashlytics.getInstance().setUserId(uniqueKey);
                FirebaseCrashlytics.getInstance().log("Set UUID to: " + uniqueKey);
            }
            mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
            if (isManualDisableAnalytics) {
                mFirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(true);
            }

            Trace remoteConfigStartTrace = FirebasePerformance.getInstance().newTrace("remoteConfig_setup");
            remoteConfigStartTrace.start();
            mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
            FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(1800).build();
            mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);
            mFirebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);
            remoteConfigStartTrace.stop();

            window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            Trace remoteConfigFetchTrace = FirebasePerformance.getInstance().newTrace("remoteConfig_fetch");
            remoteConfigFetchTrace.start();
            mFirebaseRemoteConfig.fetchAndActivate().addOnCompleteListener(this, new OnCompleteListener<Boolean>() {
                @Override
                public void onComplete(@NonNull Task<Boolean> task) {
                    if (task.isSuccessful()) {
                        boolean updated = task.getResult();
                        FirebaseCrashlytics.getInstance().log("Remote config fetch succeeded: " + updated);
                        mFirebaseRemoteConfig.activate();
                    }
                }
            });
            remoteConfigFetchTrace.stop();
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.status_bar_background_color));
            toolbar =(Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.toolbar_background_color));

            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            Network network = connectivityManager.getActiveNetwork();
            LinkProperties linkProperties = connectivityManager.getLinkProperties(network);
            updateVisualIndicator(linkProperties);
            if (connectivityManager != null) {
                connectivityManager.registerNetworkCallback(new NetworkRequest.Builder().build(), new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
                        super.onLinkPropertiesChanged(network, linkProperties);
                        updateVisualIndicator(linkProperties);
                    }
                });
            }

            statusIcon = (ImageView) findViewById(R.id.connectionStatus);
            statusIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Bundle bundle = new Bundle();
                    bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "help_icon");
                    mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
                    Intent helpIntent = new Intent(v.getContext(), help.class);
                    startActivity(helpIntent);
                }
            });

            provisionWebView("https://test.nextdns.io");
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    @Override
    @AddTrace(name = "test_inflate", enabled = true /* optional */)
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_back_only, menu);
        return true;
    }

    @AddTrace(name = "provision_web_view", enabled = true /* optional */)
    public void provisionWebView(String url) {
        try {
            webView =(WebView) findViewById(R.id.mWebview);
            webView.getSettings().setPluginState(WebSettings.PluginState.ON);
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

            int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
                if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
                    FirebaseCrashlytics.getInstance().setCustomKey("force_dark_strategy_supported", true);
                    WebSettingsCompat.setForceDarkStrategy(webView.getSettings(), WebSettingsCompat.DARK_STRATEGY_PREFER_WEB_THEME_OVER_USER_AGENT_DARKENING);
                }
                if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                    FirebaseCrashlytics.getInstance().setCustomKey("force_dark_supported", true);
                    WebSettingsCompat.setForceDark(webView.getSettings(), WebSettingsCompat.FORCE_DARK_ON);
                }
            }
            webView.loadUrl(url);
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    @Override
    @AddTrace(name = "test_switch", enabled = true /* optional */)
    public boolean onOptionsItemSelected(MenuItem item) {
        Bundle bundle = new Bundle();
        switch (item.getItemId()) {
            case R.id.back:
                if (isManualDisableAnalytics) {
                    bundle.putString("id", "back");
                    mFirebaseAnalytics.logEvent("toolbar_action", bundle);
                }
                Intent mainIntent = new Intent(this, MainActivity.class);
                startActivity(mainIntent);
            default:
                return super.onContextItemSelected(item);
        }
    }

    @AddTrace(name = "update_visual_indicator", enabled = true /* optional */)
    public void updateVisualIndicator(LinkProperties linkProperties) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                if (linkProperties.isPrivateDnsActive()) {
                    if (linkProperties.getPrivateDnsServerName() != null) {
                        if (linkProperties.getPrivateDnsServerName().contains("nextdns")) {
                            ImageView connectionStatus = (ImageView) findViewById(R.id.connectionStatus);
                            connectionStatus.setImageResource(R.drawable.success);
                            connectionStatus.setColorFilter(getResources().getColor(R.color.green));
                            FirebaseCrashlytics.getInstance().log("Set connection status to NextDNS.");
                        } else {
                            ImageView connectionStatus = (ImageView) findViewById(R.id.connectionStatus);
                            connectionStatus.setImageResource(R.drawable.success);
                            connectionStatus.setColorFilter(getResources().getColor(R.color.yellow));
                            FirebaseCrashlytics.getInstance().log("Set connection status to private DNS.");
                        }
                    } else {
                        ImageView connectionStatus = (ImageView) findViewById(R.id.connectionStatus);
                        connectionStatus.setImageResource(R.drawable.success);
                        connectionStatus.setColorFilter(getResources().getColor(R.color.yellow));
                        FirebaseCrashlytics.getInstance().log("Set connection status to private DNS.");
                    }
                } else {
                    ImageView connectionStatus = (ImageView) findViewById(R.id.connectionStatus);
                    connectionStatus.setImageResource(R.drawable.failure);
                    connectionStatus.setColorFilter(getResources().getColor(R.color.red));
                    FirebaseCrashlytics.getInstance().log("Set connection status to insecure DNS.");
                }
            } else {
                ImageView connectionStatus = (ImageView) findViewById(R.id.connectionStatus);
                connectionStatus.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }
}