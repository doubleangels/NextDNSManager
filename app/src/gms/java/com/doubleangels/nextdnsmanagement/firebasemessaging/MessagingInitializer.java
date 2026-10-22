package com.doubleangels.nextdnsmanagement.firebasemessaging;

import android.content.Context;

import com.doubleangels.nextdnsmanagement.sentry.SentryManager;
import com.doubleangels.nextdnsmanagement.sharedpreferences.SharedPreferencesManager;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Initializes Firebase in the given context, retrieves the FCM token,
 * and subscribes the device to a default topic ("general").
 */
public final class MessagingInitializer {

    private static final String KEY_FCM_TOKEN = "fcmToken";

    // MainActivity.onCreate() (and therefore this initializer) can run more than
    // once per process lifetime (recreation, returning to a still-warm process).
    // Re-requesting a token/registration on every call can trip FCM's
    // TOO_MANY_REGISTRATIONS rate limit, so only do real work once per process.
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    /**
     * Lets callers skip spinning a background thread for a call that would just
     * no-op anyway (see {@link #INITIALIZED}).
     */
    public static boolean isInitialized() {
        return INITIALIZED.get();
    }

    public static void initialize(Context context) {
        if (!INITIALIZED.compareAndSet(false, true)) {
            return;
        }

        try {
            SharedPreferencesManager.init(context);
            FirebaseApp.initializeApp(context);
            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    reportFailure(task.getException(), "Fetching FCM registration token failed");
                    return;
                }
                String token = task.getResult();
                if (token == null) {
                    SentryManager.captureMessage("FCM token is null");
                    return;
                }
                SharedPreferencesManager.putString(KEY_FCM_TOKEN, token);
                FirebaseMessaging.getInstance().subscribeToTopic("general").addOnCompleteListener(topicTask -> {
                    if (topicTask.isSuccessful()) {
                        SentryManager.captureMessage("Subscribed to topic 'general'");
                    } else {
                        reportFailure(topicTask.getException(), "Topic subscription failed");
                    }
                });
            });
        } catch (Exception e) {
            SentryManager.captureException(e);
        }
    }

    /**
     * Transient Play Services failures (e.g. TOO_MANY_REGISTRATIONS) are filtered
     * out by {@link SentryManager#captureException}.
     */
    private static void reportFailure(Exception error, String fallbackMessage) {
        if (error != null) {
            SentryManager.captureException(error);
        } else {
            SentryManager.captureMessage(fallbackMessage);
        }
    }
}
