package com.doubleangels.nextdnsmanagement.webview;

import android.app.Activity;
import android.content.Context;
import android.webkit.JavascriptInterface;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.doubleangels.nextdnsmanagement.sentry.SentryManager;

import java.lang.ref.WeakReference;

/** @noinspection unused*/
public class WebAppInterface {
    private final WeakReference<Context> contextRef;
    private final WeakReference<SwipeRefreshLayout> swipeRefreshLayoutRef;

    public WebAppInterface(Context context, SwipeRefreshLayout swipeRefreshLayout) {
        this.contextRef = new WeakReference<>(context);
        this.swipeRefreshLayoutRef = new WeakReference<>(swipeRefreshLayout);
    }

    /**
     * Called from JavaScript to enable or disable the swipe refresh.
     */
    @JavascriptInterface
    public void setSwipeRefreshEnabled(final boolean enabled) {
        try {
            Context context = contextRef.get();
            SwipeRefreshLayout swipeRefreshLayout = swipeRefreshLayoutRef.get();

            if (context == null || swipeRefreshLayout == null) {
                // References have been garbage collected, safe to ignore
                return;
            }

            if (context instanceof Activity) {
                ((Activity) context).runOnUiThread(() -> {
                    SwipeRefreshLayout layout = swipeRefreshLayoutRef.get();
                    if (layout != null) {
                        layout.setEnabled(enabled);
                    }
                });
            } else {
                SentryManager.captureMessage("Context is not an Activity instance. Cannot update swipe refresh.");
            }
        } catch (Exception e) {
            SentryManager.captureException(e);
        }
    }
}
