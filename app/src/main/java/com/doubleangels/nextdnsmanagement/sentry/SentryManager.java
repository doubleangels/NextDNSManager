package com.doubleangels.nextdnsmanagement.sentry;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import io.sentry.Sentry;

public class SentryManager {

    private final Context context;
    public String TAG = "NextDNS Manager Logging";

    public SentryManager(Context context) {
        this.context = context;
    }

    public void captureException(Exception e) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("preferences", Context.MODE_PRIVATE);
        boolean sentryEnabled = sharedPreferences.getBoolean("sentry_enable", false);
        if (sentryEnabled) {
            Sentry.captureException(e);
            Log.e(TAG, "Got error:", e);
        } else {
            Log.e(TAG, "Got error:", e);
        }
    }

    public void captureMessage(String message) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("preferences", Context.MODE_PRIVATE);
        boolean sentryEnabled = sharedPreferences.getBoolean("sentry_enable", false);
        if (sentryEnabled) {
            Sentry.addBreadcrumb(message);
            Log.d(TAG, message);
        } else {
            Log.d(TAG, message);
        }

    }

    public boolean isSentryEnabled() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean("sentry_enable", false);
    }
}
