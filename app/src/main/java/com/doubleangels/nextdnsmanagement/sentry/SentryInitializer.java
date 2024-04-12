package com.doubleangels.nextdnsmanagement.sentry;

import android.content.Context;
import io.sentry.android.core.SentryAndroid;

public class SentryInitializer {

    // Method to initialize Sentry for error tracking
    public static void initialize(Context context) {
        // Start a new thread to initialize Sentry asynchronously
        new Thread(() -> {
            // Initialize SentryAndroid with the provided context and configuration options
            SentryAndroid.init(context, options -> {
                // Set the Data Source Name (DSN) for Sentry
                options.setDsn("https://8b52cc2148b94716a69c9a4f0c0b4513@o244019.ingest.us.sentry.io/6270764");
                // Enable tracing to capture performance data
                options.setEnableTracing(true);
                // Enable automatic breadcrumbs for better error context
                options.enableAllAutoBreadcrumbs(true);
                // Attach screenshots to captured events
                options.setAttachScreenshot(true);
                // Attach view hierarchy information to captured events
                options.setAttachViewHierarchy(true);
                // Set the sample rate for tracing to capture all traces
                options.setTracesSampleRate(1.0);
                // Enable profiling of application startup
                options.setEnableAppStartProfiling(true);
                // Enable capturing of ANR (Application Not Responding) events
                options.setAnrEnabled(true);
                // Collect additional context data with captured events
                options.setCollectAdditionalContext(true);
                // Enable tracking of frames for performance monitoring
                options.setEnableFramesTracking(true);
            });
        }).start(); // Start the thread
    }
}
