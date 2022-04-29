package com.doubleangels.nextdnsmanagement;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.perf.metrics.AddTrace;

import java.util.Objects;

import io.sentry.ISpan;
import io.sentry.ITransaction;
import io.sentry.Sentry;

public class test extends AppCompatActivity {

    @Override
    @AddTrace(name = "test_create")
    protected void onCreate(Bundle savedInstanceState) {
        ITransaction test_create_transaction = Sentry.startTransaction("onCreate()", "test");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

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

            // Check if we're using private DNS and watch DNS type over time to change visual indicator.
            ConnectivityManager connectivityManager = (ConnectivityManager) this.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            Network network = connectivityManager.getActiveNetwork();
            LinkProperties linkProperties = connectivityManager.getLinkProperties(network);
            updateVisualIndicator(linkProperties, getApplicationContext());
            connectivityManager.registerNetworkCallback(new NetworkRequest.Builder().build(), new ConnectivityManager.NetworkCallback() {
                @Override
                public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
                    super.onLinkPropertiesChanged(network, linkProperties);
                    updateVisualIndicator(linkProperties, getApplicationContext());
                }
            });

            // Set up our button.
            ImageView statusIcon = findViewById(R.id.connectionStatus);
            statusIcon.setOnClickListener(v -> {
                Intent helpIntent = new Intent(v.getContext(), help.class);
                startActivity(helpIntent);
            });

            // Provision our web view.
            provisionWebView(getString(R.string.test_url));
        } catch (Exception e) {
            captureExceptionAndFeedback(e);
        } finally {
            test_create_transaction.finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_back_only, menu);
        return true;
    }

    @SuppressLint("SetJavaScriptEnabled")
    @SuppressWarnings("unused")
    @AddTrace(name = "test_provision_web_view")
    public void provisionWebView(String url) {
        ITransaction test_provision_web_view_transaction = Sentry.startTransaction("provisionWebView()", "test");
        try {
            WebView webView = findViewById(R.id.mWebview);
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

            // Use the built in dark mode.
            ISpan force_dark_mode_span = test_provision_web_view_transaction.startChild("force_dark_mode");
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
        } catch (Exception e) {
            captureExceptionAndFeedback(e);
        } finally {
            test_provision_web_view_transaction.finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.back) {
            Intent mainIntent = new Intent(this, MainActivity.class);
            startActivity(mainIntent);
        }
        return super.onContextItemSelected(item);
    }

    @AddTrace(name = "update_visual_indicator")
    public void updateVisualIndicator(LinkProperties linkProperties, Context context) {
        ITransaction update_visual_indicator_transaction = Sentry.startTransaction("updateVisualIndicator()", "test");
        try {
            if (linkProperties != null) {
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
            captureExceptionAndFeedback(e);
        } finally {
            update_visual_indicator_transaction.finish();
        }
    }

    @SuppressWarnings("deprecation")
    public void captureExceptionAndFeedback(Exception exception) {
        ITransaction capture_exception_and_feedback_transaction = Sentry.startTransaction("captureExceptionAndFeedback()", "MainActivity");
        try {
            // Generate our snackbar used to ask the user if they want to make feedback.
            Snackbar snackbar = Snackbar.make(this.getWindow().getDecorView().getRootView(), "Error occurred! Share feedback?", Snackbar.LENGTH_LONG);

            // If user wants to provide feedback, send them to the feedback activity.
            snackbar.setAction("SHARE", view -> {
                int LAUNCH_SECOND_ACTIVITY = 1;
                Intent feedbackIntent = new Intent(this, feedback.class);
                Bundle bundle = new Bundle();
                bundle.putSerializable("e", exception);
                feedbackIntent.putExtras(bundle);
                this.startActivityForResult(feedbackIntent, LAUNCH_SECOND_ACTIVITY);
            });

            // If snackbar is dismissed on its own, proceed with normal error report.
            snackbar.addCallback(new Snackbar.Callback() {
                @Override
                public void onDismissed(Snackbar snackbar, int event) {
                    if (event == Snackbar.Callback.DISMISS_EVENT_TIMEOUT) {
                        Sentry.captureException(exception);
                        FirebaseCrashlytics.getInstance().recordException(exception);
                    }
                }
            });
            snackbar.show();
        } catch (Exception e) {
            captureException(e);
        } finally {
            capture_exception_and_feedback_transaction.finish();
        }
    }

    public void captureException(Exception exception) {
        Sentry.captureException(exception);
        FirebaseCrashlytics.getInstance().recordException(exception);
    }
}