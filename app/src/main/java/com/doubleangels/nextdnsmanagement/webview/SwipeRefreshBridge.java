package com.doubleangels.nextdnsmanagement.webview;

import android.webkit.WebView;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import java.util.Collections;
import java.util.Set;

/**
 * Lets the NextDNS dashboard pause pull-to-refresh while the user scrolls a
 * horizontal tab strip or a modal.
 * <p>
 * Uses {@link WebViewCompat#addWebMessageListener} instead of
 * {@code addJavascriptInterface}: the bridge object is only injected into frames
 * whose origin is {@link #ALLOWED_ORIGIN}, so third-party iframes and any other
 * page the WebView ends up on cannot see or call it. Messages are plain strings
 * from a fixed set, and anything else is ignored.
 */
public final class SwipeRefreshBridge {

    /** Name of the JavaScript object exposed to the page. */
    static final String OBJECT_NAME = "NdmsBridge";
    static final String MESSAGE_DISABLE = "swipe-refresh:disable";
    static final String MESSAGE_ENABLE = "swipe-refresh:enable";

    private static final String ALLOWED_ORIGIN = "https://my.nextdns.io";
    private static final Set<String> ALLOWED_ORIGIN_RULES = Collections.singleton(ALLOWED_ORIGIN);

    private SwipeRefreshBridge() {
    }

    /**
     * Installs the bridge on a WebView. Must be called at most once per WebView,
     * before the page loads.
     *
     * @return false when the installed WebView is too old to support message
     *         listeners; pull-to-refresh then simply stays enabled.
     */
    public static boolean attach(WebView webView, SwipeRefreshLayout swipeRefreshLayout) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            return false;
        }
        WebViewCompat.addWebMessageListener(webView, OBJECT_NAME, ALLOWED_ORIGIN_RULES,
                (view, message, sourceOrigin, isMainFrame, replyProxy) -> {
                    // The origin rule already limits injection; re-check so a
                    // misconfigured rule can never widen what is accepted.
                    if (!isMainFrame || !ALLOWED_ORIGIN.equals(sourceOrigin.toString())) {
                        return;
                    }
                    String data = message.getData();
                    if (MESSAGE_DISABLE.equals(data)) {
                        swipeRefreshLayout.setEnabled(false);
                    } else if (MESSAGE_ENABLE.equals(data)) {
                        swipeRefreshLayout.setEnabled(true);
                    }
                });
        return true;
    }

    /**
     * Removes the bridge so it is not injected into further page loads.
     */
    public static void detach(WebView webView) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            WebViewCompat.removeWebMessageListener(webView, OBJECT_NAME);
        }
    }
}
