package com.doubleangels.nextdnsmanagement.sentry;

import android.content.Context;

import com.doubleangels.nextdnsmanagement.BuildConfig;

import io.sentry.Hint;
import io.sentry.Sentry;
import io.sentry.SentryEvent;
import io.sentry.TypeCheckHint;
import io.sentry.android.core.SentryAndroid;
import io.sentry.protocol.SentryException;
import io.sentry.protocol.SentryStackFrame;
import io.sentry.protocol.SentryStackTrace;
import io.sentry.protocol.SentryThread;

import java.util.List;

import okhttp3.Request;

/**
 * A utility class for initializing Sentry in the application. It sets various
 * configuration options
 * for better error monitoring, performance tracing, and diagnostic
 * capabilities.
 */
public final class SentryInitializer {

    private SentryInitializer() {
    }

    /**
     * Initializes Sentry, providing DSN (Data Source Name), release version,
     * and other advanced configuration options for capturing and tracking
     * application performance and errors.
     *
     * @param context The application or activity context needed for Sentry's
     *                initialization.
     */
    public static synchronized void initialize(Context context) {
        if (Sentry.isEnabled()) {
            return;
        }
        SentryAndroid.init(context.getApplicationContext(), options -> {
            // The DSN (Data Source Name) for your Sentry project:
            // This tells Sentry where to send the error data
            options.setDsn("https://8b52cc2148b94716a69c9a4f0c0b4513@o244019.ingest.us.sentry.io/6270764");

            // Set the release version in Sentry to match the app's version name
            // Useful for tracking errors that occur across different app releases
            options.setRelease(BuildConfig.VERSION_NAME);

            // Enable all auto-breadcrumbs. This helps capture UI events,
            // network calls, and more to provide context around errors
            options.enableAllAutoBreadcrumbs(true);

            // Attach a screenshot of the user's view hierarchy at the time of the error
            // This helps visualize the state of the UI when the crash or error occurred
            options.setAttachScreenshot(true);

            // Attach the full view hierarchy for debugging layout or view-related issues
            options.setAttachViewHierarchy(true);

            // Sample 20% of performance transactions to reduce overhead.
            options.setTracesSampleRate(0.2);

            // Enables application start profiling, which measures cold/warm start
            // performance
            options.setEnableAppStartProfiling(true);

            // Enables ANR (Application Not Responding) detection to capture when the app
            // has blocked the UI thread for too long
            options.setAnrEnabled(true);

            // Allows collection of additional context such as environment info or other
            // device data
            options.setCollectAdditionalContext(true);

            // Enables frames tracking for performance monitoring
            // Tracks dropped or slow frames to measure UI responsiveness
            options.setEnableFramesTracking(true);

            // Check if the device is rooted, adding extra context for debugging certain
            // crashes
            // that may be more likely on rooted devices
            options.setEnableRootCheck(true);

            SentryManager.registerIgnoredExceptionTypes(options);

            // Filter out SentryHttpClientException thrown by SentryOkHttpInterceptor
            // when NextDNS or other APIs return 5xx errors (e.g., 504).
            // These are server-side availability issues, not app crashes.
            options.setBeforeSend((event, hint) -> {
                if (shouldDropEvent(event, hint)) {
                    return null;
                }
                return event;
            });

        });
    }

    /**
     * Initializes or shuts down Sentry to match the user's opt-in preference, so
     * toggling the setting takes effect without restarting the app.
     */
    public static synchronized void applyPreference(Context context, boolean enabled) {
        if (enabled) {
            initialize(context);
        } else if (Sentry.isEnabled()) {
            Sentry.close();
        }
    }

    private static boolean shouldDropEvent(SentryEvent event, Hint hint) {
        if (event.getThrowable() != null) {
            if (event.getThrowable().getClass().getSimpleName().equals("SentryHttpClientException")) {
                return true;
            }
            if (SentryManager.isIgnored(event.getThrowable())) {
                return true;
            }
            if (isFirebaseInfrastructureFailure(event)) {
                return true;
            }
            if (isIgnoredInfrastructureWithoutAppFrames(event)) {
                return true;
            }
        }
        if (matchesIgnoredSerializedExceptions(event)) {
            return true;
        }
        return isNextDnsConnectivityProbe(event, hint);
    }

    private static boolean matchesIgnoredSerializedExceptions(SentryEvent event) {
        if (event.getMessage() != null && SentryManager.matchesIgnoredMessage(event.getMessage().getFormatted())) {
            return true;
        }
        List<SentryException> exceptions = event.getExceptions();
        if (exceptions == null) {
            return false;
        }
        for (SentryException exception : exceptions) {
            if (exception != null && SentryManager.matchesIgnoredMessage(exception.getValue())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Drops auto-captured Firebase / Play Services messaging failures that never
     * touch application code.
     */
    private static boolean isFirebaseInfrastructureFailure(SentryEvent event) {
        Throwable throwable = event.getThrowable();
        if (throwable == null) {
            return false;
        }
        if (!SentryManager.isIgnored(throwable)) {
            return false;
        }
        return stackTraceContainsModule(event, "com.google.firebase.messaging")
                || stackTraceContainsModule(event, "com.google.android.gms.cloudmessaging")
                || stackTraceContainsModule(event, "com.google.firebase.installations");
    }

    /**
     * Drops ignored network/DNS failures that only appear in third-party stack frames
     * (e.g. OkHttp worker threads for the connectivity probe).
     */
    private static boolean isIgnoredInfrastructureWithoutAppFrames(SentryEvent event) {
        Throwable throwable = event.getThrowable();
        if (throwable == null || !SentryManager.isIgnored(throwable)) {
            return false;
        }
        return !stackTraceContainsModule(event, "com.doubleangels.nextdnsmanagement");
    }

    private static boolean stackTraceContainsModule(SentryEvent event, String modulePrefix) {
        List<SentryThread> threads = event.getThreads();
        if (threads != null) {
            for (SentryThread thread : threads) {
                if (stackTraceContainsModule(thread.getStacktrace(), modulePrefix)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean stackTraceContainsModule(SentryStackTrace stackTrace, String modulePrefix) {
        if (stackTrace == null || stackTrace.getFrames() == null) {
            return false;
        }
        for (SentryStackFrame frame : stackTrace.getFrames()) {
            String module = frame.getModule();
            if (module != null && module.startsWith(modulePrefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Drops auto-captured OkHttp errors from the NextDNS connectivity check endpoint.
     * DNS/network failures there are expected when offline, on VPN, or without NextDNS configured.
     */
    private static boolean isNextDnsConnectivityProbe(SentryEvent event, Hint hint) {
        Object okHttpRequest = hint.get(TypeCheckHint.OKHTTP_REQUEST);
        if (okHttpRequest instanceof Request request) {
            if (isTestNextDnsHost(request.url().host())) {
                return true;
            }
        }

        Throwable throwable = event.getThrowable();
        while (throwable != null) {
            String message = throwable.getMessage();
            if (message != null && message.contains("test.nextdns.io")) {
                return true;
            }
            throwable = throwable.getCause();
        }
        return false;
    }

    private static boolean isTestNextDnsHost(String host) {
        return host != null && (host.equals("test.nextdns.io") || host.endsWith(".test.nextdns.io"));
    }
}
