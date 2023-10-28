package com.doubleangels.nextdnsmanagement;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import io.sentry.Sentry;

public class DarkModeHandler {
    public Boolean overrideDarkMode;
    public Boolean manualDarkMode;
    public Boolean isDarkModeOn;
    public Boolean darkNavigation;
    public void handleDarkMode(Context context) {
        try {
            // Get shared preferences.
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

            // Set up white text when dark navigation is enabled on light theme.
            darkNavigation = sharedPreferences.getBoolean(settings.DARK_NAVIGATION, false);
            if (darkNavigation) {
                isDarkModeOn = true;
                isDarkModeOn = (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)  == Configuration.UI_MODE_NIGHT_YES;
            }

            // Set up dark mode.
            manualDarkMode = sharedPreferences.getBoolean(settings.MANUAL_DARK_MODE, false);
            darkNavigation = sharedPreferences.getBoolean(settings.DARK_NAVIGATION, false);

            if (overrideDarkMode) {
                isDarkModeOn = manualDarkMode;
                Sentry.setTag("overridden_dark_mode", "true");
                Sentry.addBreadcrumb("Turned on override for dark mode");
            } else {
                isDarkModeOn = (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)  == Configuration.UI_MODE_NIGHT_YES;
                Sentry.setTag("overridden_dark_mode", "false");
                Sentry.addBreadcrumb("Turned off override for dark mode");
            }

            if (isDarkModeOn) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                Sentry.setTag("manual_dark_mode", "true");
                Sentry.addBreadcrumb("Set manual dark mode to true");
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                Sentry.setTag("manual_dark_mode", "false");
                Sentry.addBreadcrumb("Set manual dark mode to false");
            }
        } catch (Exception e) {
            Sentry.captureException(e);
        }
    }
}
