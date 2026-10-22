package com.doubleangels.nextdnsmanagement;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.doubleangels.nextdnsmanagement.sentry.SentryManager;
import com.doubleangels.nextdnsmanagement.utils.ExternalLinkHandler;
import com.doubleangels.nextdnsmanagement.utils.InsetsHelper;
import com.doubleangels.nextdnsmanagement.webview.WebViewInteractionScript;

/**
 * Activity that handles ping functionality by loading two web pages in separate
 * WebViews.
 */
public class PingActivity extends BaseActivity {

    private WebView webView;
    private WebView webView2;
    private boolean webView2Initialized;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ping);
        setupInsets();

        try {
            setupWebViewsForActivity(getString(R.string.ping_url), getString(R.string.test_url));
        } catch (Exception e) {
            SentryManager.captureException(e);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
            webView.pauseTimers();
        }
        if (webView2 != null) {
            webView2.onPause();
            webView2.pauseTimers();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
            webView.resumeTimers();
        }
        if (webView2 != null) {
            webView2.onResume();
            webView2.resumeTimers();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            destroyWebView(webView);
            webView = null;
            destroyWebView(webView2);
            webView2 = null;
        } catch (Exception e) {
            SentryManager.captureException(e);
        }
    }

    private void setupInsets() {
        View root = findViewById(R.id.root);
        InsetsHelper.installOnRoot(root);
        InsetsHelper.applySystemBarPadding(root);
    }

    private void setupWebViewsForActivity(String url1, String url2) {
        try {
            webView = findViewById(R.id.webView);
            webView2 = findViewById(R.id.webView2);
            if (webView == null || webView2 == null) {
                throw new IllegalStateException("Ping WebViews not found in layout");
            }
            setupWebView(webView, url1, () -> {
                if (!webView2Initialized && webView2 != null) {
                    webView2Initialized = true;
                    setupWebView(webView2, url2, null);
                }
            });
        } catch (Exception e) {
            SentryManager.captureException(e);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView(WebView targetWebView, String url, Runnable onFirstPageFinished) {
        WebSettings settings = targetWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        targetWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (uri == null) {
                    return false;
                }
                if (ExternalLinkHandler.isNextDnsHost(uri)) {
                    return false;
                }
                return ExternalLinkHandler.openExternalLink(view.getContext(), view, uri);
            }

            @Override
            public void onPageFinished(WebView view, String pageUrl) {
                if (onFirstPageFinished != null) {
                    onFirstPageFinished.run();
                }
            }
        });
        targetWebView.loadUrl(url);
    }

    private void destroyWebView(WebView targetWebView) {
        if (targetWebView == null) {
            return;
        }
        targetWebView.evaluateJavascript(WebViewInteractionScript.DISCONNECT_SCRIPT, null);
        if (targetWebView.getParent() != null) {
            ((ViewGroup) targetWebView.getParent()).removeView(targetWebView);
        }
        targetWebView.setWebViewClient(new WebViewClient());
        targetWebView.destroy();
    }
}
