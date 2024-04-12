package com.doubleangels.nextdnsmanagement.sentry;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import io.sentry.Sentry;

public class SentryManager {

    private final Context context;
    public String TAG = "NextDNS Manager Logging";
    public SharedPreferences sharedPreferences;

    public SentryManager(Context context) {
        this.context = context;
    }

    public void captureException(Exception e) {
        if (isEnabled()) {
            Sentry.captureException(e);
            Log.e(TAG, "Got error:", e);
        } else {
            Log.e(TAG, "Got error:", e);
        }
    }

    public void captureMessage(String message) {
        if (isEnabled()) {
            Sentry.addBreadcrumb(message);
            Log.d(TAG, message);
        } else {
            Log.d(TAG, message);
        }

    }

    public void setTag(String key, String value) {
        if (isEnabled()) {
            Sentry.setTag(key, value);
        }
    }

    public boolean isEnabled() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean("sentry_enable", false);
    }
}