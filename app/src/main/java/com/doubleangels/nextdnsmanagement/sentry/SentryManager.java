package com.doubleangels.nextdnsmanagement.sentry;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import io.sentry.Sentry;

public class SentryManager {

    // Context reference for accessing resources and preferences
    private final Context context;
    // Tag for logging messages
    public String TAG = "NextDNS Manager Logging";
    // SharedPreferences instance for accessing app preferences
    public SharedPreferences sharedPreferences;
    // Constructor to initialize the SentryManager with a context
    public SentryManager(Context context) {
        this.context = context;
    }
    // Method to capture and log exceptions
    public void captureException(Exception e) {
        // Check if Sentry is enabled
        if (isEnabled()) {
            // Capture the exception with Sentry
            Sentry.captureException(e);
            // Log the exception with TAG for debugging
            Log.e(TAG, "Got error:", e);
        } else {
            // Log the exception without capturing with Sentry
            Log.e(TAG, "Got error:", e);
        }
    }

    // Method to capture and log messages
    public void captureMessage(String message) {
        // Check if Sentry is enabled
        if (isEnabled()) {
            // Add a breadcrumb to the Sentry event
            Sentry.addBreadcrumb(message);
            // Log the message with TAG for debugging
            Log.d(TAG, message);
        } else {
            // Log the message without capturing with Sentry
            Log.d(TAG, message);
        }
    }

    // Method to check if Sentry is enabled in app preferences
    public boolean isEnabled() {
        // Get default SharedPreferences instance
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        // Retrieve the value of "sentry_enable" preference, default is false
        return sharedPreferences.getBoolean("sentry_enable", false);
    }
}
