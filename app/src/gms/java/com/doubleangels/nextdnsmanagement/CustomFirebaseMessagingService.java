package com.doubleangels.nextdnsmanagement;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.doubleangels.nextdnsmanagement.sentry.SentryManager;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Service handling Firebase Cloud Messaging events. It receives incoming
 * messages,
 * extracts the notification data, and displays notifications. It also logs
 * errors via Sentry.
 */
public class CustomFirebaseMessagingService extends FirebaseMessagingService {

    // Notification channel constants
    private static final String CHANNEL_ID = "general";
    private static final String CHANNEL_NAME = "General";

    // Sentry manager instance for capturing errors
    private SentryManager sentryManager;

    /**
     * Called when the service is created. Initializes the SentryManager and the
     * notification channel (created once here rather than on every message).
     */
    @Override
    public void onCreate() {
        super.onCreate();
        sentryManager = new SentryManager(this);
        try {
            NotificationManager notificationManager = (NotificationManager) getSystemService(
                    Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH);
                notificationManager.createNotificationChannel(channel);
            } else {
                sentryManager.captureMessage("NotificationManager is null");
            }
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
    }

    /**
     * Called when a new message is received from Firebase Cloud Messaging.
     * Extracts notification details from the message and displays a notification.
     *
     * @param remoteMessage The received RemoteMessage.
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        try {
            // Default title and message in case none is provided
            String title = "This notification did not include a title.";
            String messageBody = "This notification did not include a message body.";

            // Attempt to extract title and body from the notification payload
            try {
                if (remoteMessage.getNotification() != null) {
                    if (remoteMessage.getNotification().getTitle() != null) {
                        title = remoteMessage.getNotification().getTitle();
                    }
                    if (remoteMessage.getNotification().getBody() != null) {
                        messageBody = remoteMessage.getNotification().getBody();
                    }
                }
            } catch (Exception e) {
                // Capture exception if an error occurs while processing the notification
                // payload
                sentryManager.captureException(e);
            }

            // Attempt to override with data payload if available
            try {
                if (!remoteMessage.getData().isEmpty()) {
                    if (remoteMessage.getData().containsKey("title")) {
                        title = remoteMessage.getData().get("title");
                    }
                    if (remoteMessage.getData().containsKey("message")) {
                        messageBody = remoteMessage.getData().get("message");
                    }
                }
            } catch (Exception e) {
                // Capture exception if an error occurs while processing the data payload
                sentryManager.captureException(e);
            }

            // Create an intent to open MainActivity when the notification is clicked
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this,
                    (title + messageBody).hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            // Build the notification
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(title)
                    .setContentText(messageBody)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent);

            // Get the NotificationManager system service
            NotificationManager notificationManager = (NotificationManager) getSystemService(
                    Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                // Show the notification (the channel is created once in onCreate())
                try {
                    int notificationId = (title + messageBody).hashCode();
                    notificationManager.notify(notificationId, notificationBuilder.build());
                } catch (Exception e) {
                    // Capture any exception that occurs while displaying the notification
                    sentryManager.captureException(e);
                }
            } else {
                // Log a message if the NotificationManager is not available
                sentryManager.captureMessage("NotificationManager is null");
            }
        } catch (Exception e) {
            // Capture any general exception during message processing
            sentryManager.captureException(e);
        }
    }

    /**
     * Called when a new Firebase Cloud Messaging token is generated.
     * Logs the new token for debugging purposes.
     *
     * @param token The new FCM token.
     */
    @Override
    public void onNewToken(@NonNull String token) {
        try {
            super.onNewToken(token);
            // Log the new token using Sentry
            sentryManager.captureMessage("New FCM token received");
        } catch (Exception e) {
            // Capture any exception that occurs while handling the new token
            sentryManager.captureException(e);
        }
    }
}
