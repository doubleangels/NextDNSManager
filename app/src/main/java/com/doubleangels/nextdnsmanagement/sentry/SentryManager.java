package com.doubleangels.nextdnsmanagement.sentry;

import android.content.ActivityNotFoundException;
import android.util.Log;

import com.doubleangels.nextdnsmanagement.sharedpreferences.SharedPreferencesManager;

import java.io.EOFException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;

import io.sentry.Sentry;
import io.sentry.SentryOptions;
import okhttp3.internal.http2.ConnectionShutdownException;

/**
 * Static helper for Sentry error logging. Every call checks the user's opt-in
 * preference before sending anything, and transient infrastructure failures are
 * only logged locally.
 * <p>
 * This class is intentionally stateless: holding an instance in an Activity field
 * meant callbacks that outlived the Activity could hit a nulled-out reference and
 * crash while trying to report a different error.
 */
public final class SentryManager {

    private static final String TAG = "SentryManager";

    private static final AtomicBoolean FILTERED_UNCAUGHT_HANDLER_INSTALLED = new AtomicBoolean(false);

    /**
     * Exception types that are never sent to Sentry. Subclasses are listed
     * explicitly because Sentry's own type filter matches exact classes only.
     */
    private static final List<Class<? extends Exception>> IGNORED_ERRORS = Arrays.asList(
            ActivityNotFoundException.class,
            ConnectException.class,
            EOFException.class,
            SecurityException.class,
            UnknownHostException.class,
            SocketTimeoutException.class,
            SocketException.class,
            SSLException.class,
            SSLHandshakeException.class,
            ConnectionShutdownException.class);

    /**
     * Firebase Messaging / Installations errors that reflect device or Play
     * Services state, not app bugs. Matched case-sensitively.
     */
    private static final List<String> TRANSIENT_FIREBASE_MESSAGING_ERRORS = Arrays.asList(
            "SERVICE_NOT_AVAILABLE",
            "FIS_AUTH_ERROR",
            "MISSING_INSTANCEID_SERVICE",
            "TOO_MANY_REGISTRATIONS",
            "Firebase Installations Service is unavailable",
            "TIMEOUT");

    /**
     * Transient network errors from OkHttp or connectivity checks. Matched
     * against the lower-cased message.
     */
    private static final List<String> TRANSIENT_NETWORK_ERROR_MESSAGES = Arrays.asList(
            "canceled",
            "cancel",
            "port out of range",
            "exhausted all routes",
            "failed to connect",
            "timeout",
            "unexpected end of stream",
            "stream was reset",
            "connection reset",
            "broken pipe",
            "connection closed",
            "socket closed",
            "settings preface not received",
            "authenticate with proxy",
            "read timed out",
            "can only download http/https");

    private SentryManager() {
    }

    /**
     * Returns true if the throwable, or anything in its cause chain, is a known
     * transient failure that should not be reported.
     */
    public static boolean isIgnored(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            for (Class<? extends Exception> ignored : IGNORED_ERRORS) {
                if (ignored.isInstance(current)) {
                    return true;
                }
            }
            if (matchesIgnoredMessage(current.getMessage())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * Returns true when an exception message matches a known transient pattern.
     */
    public static boolean matchesIgnoredMessage(String message) {
        if (message == null || message.isEmpty()) {
            return false;
        }
        for (String error : TRANSIENT_FIREBASE_MESSAGING_ERRORS) {
            if (message.contains(error)) {
                return true;
            }
        }
        String lower = message.toLowerCase(Locale.ROOT);
        for (String pattern : TRANSIENT_NETWORK_ERROR_MESSAGES) {
            if (lower.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Swallows transient infrastructure failures on background threads instead of
     * forwarding them to Sentry's default uncaught handler.
     */
    public static void installFilteredUncaughtExceptionHandler() {
        if (!FILTERED_UNCAUGHT_HANDLER_INSTALLED.compareAndSet(false, true)) {
            return;
        }
        Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            if (isIgnored(throwable)) {
                Log.w(TAG, "Ignored uncaught exception on thread " + thread.getName(), throwable);
                return;
            }
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
            }
        });
    }

    /**
     * Registers ignored exception types with the Sentry SDK so auto-captured errors
     * (e.g. from OkHttp instrumentation) are dropped before transmission.
     */
    public static void registerIgnoredExceptionTypes(SentryOptions options) {
        for (Class<? extends Exception> ignored : IGNORED_ERRORS) {
            options.addIgnoredExceptionForType(ignored);
        }
    }

    /**
     * Sends the exception to Sentry when the user opted in and it is not a known
     * transient failure. Always logs locally.
     */
    public static void captureException(Throwable e) {
        if (isIgnored(e)) {
            Log.e(TAG, "Ignored error:", e);
            return;
        }
        if (isEnabled()) {
            Sentry.captureException(e);
        }
        Log.e(TAG, "Got error:", e);
    }

    /**
     * Records the message as a Sentry breadcrumb when the user opted in. Always
     * logs locally.
     */
    public static void captureMessage(String message) {
        if (isEnabled()) {
            Sentry.addBreadcrumb(message);
        }
        Log.d(TAG, message);
    }

    /**
     * Returns true if the user enabled Sentry reporting in settings.
     */
    public static boolean isEnabled() {
        if (!SharedPreferencesManager.isInitialized()) {
            return false;
        }
        try {
            return SharedPreferencesManager.getBoolean("sentry_enable", false);
        } catch (Exception e) {
            Log.e(TAG, "Error checking Sentry enable status", e);
            return false;
        }
    }
}
