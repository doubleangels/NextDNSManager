package com.doubleangels.nextdnsmanagement.sentrymanager;

import android.content.Context;
import android.content.SharedPreferences;

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
        }
    }

    public boolean isSentryEnabled() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean("sentry_enable", false);
    }
}
