package com.doubleangels.nextdnsmanagement.firebasemessaging;

import android.content.Context;
import android.util.Log;

import com.doubleangels.nextdnsmanagement.sentry.SentryManager;
import com.doubleangels.nextdnsmanagement.sharedpreferences.SharedPreferencesManager;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Initializes Firebase in the given context, retrieves the FCM token,
 * and subscribes the device to a default topic ("general").
 */
public class MessagingInitializer {

    private static final String TAG = "MessagingInitializer";
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
        SentryManager sentryManager = new SentryManager(context);

        try {
            if (!SharedPreferencesManager.isInitialized()) {
                SharedPreferencesManager.init(context);
            }
        } catch (Exception e) {
            sentryManager.captureException(e);
            return;
        }

        try {
            FirebaseApp.initializeApp(context);
        } catch (Exception e) {
            sentryManager.captureException(e);
            return;
        }

        try {
            FirebaseMessaging.getInstance().getToken()
                    .addOnCompleteListener(task -> {
                        try {
                            if (!task.isSuccessful()) {
                                Exception tokenError = task.getException();
                                if (tokenError != null) {
                                    if (SentryManager.isIgnored(tokenError)) {
                                        Log.w(TAG, "Transient Firebase Messaging error while fetching FCM token.",
                                                tokenError);
                                    } else {
                                        sentryManager.captureException(tokenError);
                                    }
                                } else {
                                    sentryManager.captureMessage("Fetching FCM registration token failed");
                                }
                                return;
                            }

                            String token = task.getResult();
                            if (token == null) {
                                sentryManager.captureMessage("FCM token is null");
                                return;
                            }

                            sentryManager.captureMessage("FCM token retrieved successfully");
                            SharedPreferencesManager.putString(KEY_FCM_TOKEN, token);

                            FirebaseMessaging.getInstance().subscribeToTopic("general")
                                    .addOnCompleteListener(task1 -> {
                                        try {
                                            if (!task1.isSuccessful()) {
                                                Exception topicError = task1.getException();
                                                if (topicError != null) {
                                                    if (SentryManager.isIgnored(topicError)) {
                                                        Log.w(TAG,
                                                                "Transient Firebase Messaging error while subscribing to topic.",
                                                                topicError);
                                                    } else {
                                                        sentryManager.captureException(topicError);
                                                    }
                                                } else {
                                                    sentryManager.captureMessage("Topic subscription failed");
                                                }
                                            } else {
                                                sentryManager.captureMessage("Subscribed to topic 'general'");
                                            }
                                        } catch (Exception ex) {
                                            sentryManager.captureException(ex);
                                        }
                                    });
                        } catch (Exception ex) {
                            sentryManager.captureException(ex);
                        }
                    });
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
    }
}
