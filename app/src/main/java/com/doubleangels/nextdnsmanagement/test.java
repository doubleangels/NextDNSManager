package com.doubleangels.nextdnsmanagement;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
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
import java.io.InputStream;
import java.util.UUID;

public class test extends AppCompatActivity {

    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private FirebaseAnalytics mFirebaseAnalytics;
    private WebView webView;
    private Window window;
    private Toolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;
    private ImageView taskbarImage;
    private ImageView statusIcon;
    private String storedUniqueKey;
    private String uniqueKey;
    private Boolean isManualDarkThemeOnSub;
    private Boolean isDarkThemeOn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        try {
            final SharedPreferences sharedPreferences = getSharedPreferences("publicResolverSharedPreferences", MODE_PRIVATE);
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
            FirebaseCrashlytics.getInstance().setCustomKey("status_bar_background_color", mFirebaseRemoteConfig.getString("status_bar_background_color"));
            window.setStatusBarColor(Color.parseColor(mFirebaseRemoteConfig.getString("status_bar_background_color")));
            toolbar =(Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            FirebaseCrashlytics.getInstance().setCustomKey("toolbar_background_color", mFirebaseRemoteConfig.getString("toolbar_background_color"));
            toolbar.setBackgroundColor(Color.parseColor(mFirebaseRemoteConfig.getString("toolbar_background_color")));
            getSupportActionBar().setDisplayShowTitleEnabled(mFirebaseRemoteConfig.getBoolean("show_title"));

            boolean isDarkThemeOnSub = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            isManualDarkThemeOnSub = sharedPreferences.getBoolean("manualDarkMode", false);
            if (isDarkThemeOnSub || isManualDarkThemeOnSub) {
                isDarkThemeOn = true;
            } else {
                isDarkThemeOn = false;
            }
            FirebaseCrashlytics.getInstance().setCustomKey("isDarkThemeOn", isDarkThemeOn);
            if (isDarkThemeOn) {
                taskbarImage = (ImageView) findViewById(R.id.taskbarImage);
                taskbarImage.setImageResource(R.drawable.taskbar_dark);
                FirebaseCrashlytics.getInstance().setCustomKey("toolbar_dark_mode_background_color", mFirebaseRemoteConfig.getString("toolbar_dark_mode_background_color"));
                toolbar.setBackgroundColor(Color.parseColor(mFirebaseRemoteConfig.getString("toolbar_dark_mode_background_color")));
            } else {
                taskbarImage = (ImageView) findViewById(R.id.taskbarImage);
                taskbarImage.setImageResource(R.drawable.taskbar_light);
            }

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

            provisionWebView();
            if (isDarkThemeOn) {
                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        injectCSS("test.css");
                        super.onPageFinished(view, url);
                    }
                });
                webView.loadUrl("https://test.nextdns.io");
            } else {
                webView.loadUrl("https://test.nextdns.io");
            }

            swipeRefresh = findViewById(R.id.swipeRefresh);
            swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    Bundle bundle = new Bundle();
                    bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "swipe_to_refresh_webview");
                    mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
                    webView.reload();
                    swipeRefresh.setRefreshing(false);
                }
            });
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

    @AddTrace(name = "inject_test_css", enabled = true /* optional */)
    private void injectCSS(String fileName) {
        try {
            InputStream inputStream = getAssets().open(fileName);
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            inputStream.close();
            String encoded = Base64.encodeToString(buffer, Base64.NO_WRAP);
            webView.loadUrl("javascript:(function() {" +
                    "var parent = document.getElementsByTagName('head').item(0);" +
                    "var style = document.createElement('style');" +
                    "style.type = 'text/css';" +
                    "style.innerHTML = window.atob('" + encoded + "');" +
                    "parent.appendChild(style)" +
                    "})()");
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    @AddTrace(name = "provision_web_view", enabled = true /* optional */)
    public void provisionWebView() {
        try {
            webView =(WebView) findViewById(R.id.mWebview);
            WebSettings webSettings = webView.getSettings();
            webSettings.setAllowContentAccess(true);
            webSettings.setUseWideViewPort(true);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setBuiltInZoomControls(true);
            webView.getSettings().setDisplayZoomControls(true);
            webView.getSettings().setDomStorageEnabled(true);
            webView.getSettings().setAppCachePath("/data/data" + getPackageName() + "/cache");
            webView.getSettings().setSaveFormData(true);
            webView.getSettings().setDatabaseEnabled(true);
            webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
            webView.setWebViewClient(new WebViewClient() {
                                         public void onPageFinished(WebView view, String url) {
                                             CookieManager.getInstance().flush();
                                         }
                                     }
            );
            FirebaseCrashlytics.getInstance().setCustomKey("accepts_third_party_cookies", CookieManager.getInstance().acceptThirdPartyCookies(webView));
            FirebaseCrashlytics.getInstance().setCustomKey("accepts_file_scheme_cookies", CookieManager.getInstance().allowFileSchemeCookies());
            FirebaseCrashlytics.getInstance().setCustomKey("has_cookies", CookieManager.getInstance().hasCookies());
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
                bundle.putString("id", "back");
                mFirebaseAnalytics.logEvent("toolbar_action", bundle);
                Intent mainIntent = new Intent(this, MainActivity.class);
                startActivity(mainIntent);
            default:
                return super.onContextItemSelected(item);
        }
    }

    @AddTrace(name = "update_visual_indicator", enabled = true /* optional */)
    public void updateVisualIndicator(LinkProperties linkProperties) {
        try {
            if (linkProperties.isPrivateDnsActive()) {
                if (linkProperties.getPrivateDnsServerName() != null) {
                    if (linkProperties.getPrivateDnsServerName().contains("nextdns")) {
                        ImageView connectionStatus = (ImageView) findViewById(R.id.connectionStatus);
                        connectionStatus.setImageResource(R.drawable.success);
                        connectionStatus.setColorFilter(getResources().getColor(R.color.green));
                    } else {
                        ImageView connectionStatus = (ImageView) findViewById(R.id.connectionStatus);
                        connectionStatus.setImageResource(R.drawable.success);
                        connectionStatus.setColorFilter(getResources().getColor(R.color.yellow));
                    }
                } else {
                    ImageView connectionStatus = (ImageView) findViewById(R.id.connectionStatus);
                    connectionStatus.setImageResource(R.drawable.success);
                    connectionStatus.setColorFilter(getResources().getColor(R.color.yellow));
                }
            } else {
                ImageView connectionStatus = (ImageView) findViewById(R.id.connectionStatus);
                connectionStatus.setImageResource(R.drawable.failure);
                connectionStatus.setColorFilter(getResources().getColor(R.color.red));
            }
        } catch (Exception e) {
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }
}