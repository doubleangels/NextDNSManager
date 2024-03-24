package com.doubleangels.nextdnsmanagement.sentry;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import io.sentry.Sentry;

public class SentryManager {

    private final Context context;

    public SentryManager(Context context) {
        this.context = context;
    }

    public void captureExceptionIfEnabled(Exception e) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean sentryEnabled = sharedPreferences.getBoolean("sentry_enable", false);
        if (sentryEnabled) {
            Sentry.captureException(e);
            Log.e("ERROR", "Got error:", e);
        } else {
            Log.e("ERROR", "Got error:", e);
        }
    }

    public boolean isSentryEnabled() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean("sentry_enable", false);
    }
}
