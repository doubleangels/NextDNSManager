package com.doubleangels.nextdnsmanagement.sentry;

import android.content.Context;
import io.sentry.android.core.SentryAndroid;

public class SentryInitializer {

    public static void initialize(Context context) {
        new Thread(() -> SentryAndroid.init(context, options -> {
            options.setDsn("https://8b52cc2148b94716a69c9a4f0c0b4513@o244019.ingest.us.sentry.io/6270764");
            options.setEnableTracing(true);
            options.enableAllAutoBreadcrumbs(true);
            options.setAttachScreenshot(true);
            options.setAttachViewHierarchy(true);
            options.setEnableTracing(true);
            options.setTracesSampleRate(1.0);
            options.setEnableAppStartProfiling(true);
            options.setAnrEnabled(true);
            options.setAttachViewHierarchy(true);
            options.setEnableAppStartProfiling(true);
            options.setCollectAdditionalContext(true);
            options.setEnableFramesTracking(true);
        })).start();
    }
}