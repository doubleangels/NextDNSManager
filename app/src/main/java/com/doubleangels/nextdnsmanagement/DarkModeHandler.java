package com.doubleangels.nextdnsmanagement;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;
import io.sentry.Sentry;

public class DarkModeHandler {
    private static final String OVERRIDE_DARK_MODE = "override_dark_mode";
    private static final String MANUAL_DARK_MODE = "manual_dark_mode";
    private static final String DARK_NAVIGATION = "dark_navigation";

    public void handleDarkMode(Context context) {
        // Get shared preferences.
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        // Set up dark mode.
        boolean overrideDarkMode = sharedPreferences.getBoolean(OVERRIDE_DARK_MODE, false);
        boolean manualDarkMode = sharedPreferences.getBoolean(MANUAL_DARK_MODE, false);
        boolean darkNavigation = sharedPreferences.getBoolean(DARK_NAVIGATION, false);

        boolean isDarkModeOn;

        if (overrideDarkMode) {
            isDarkModeOn = manualDarkMode;
            Sentry.setTag("overridden_dark_mode", "true");
            Sentry.addBreadcrumb("Turned on override for dark mode");
        } else {
            isDarkModeOn = (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            Sentry.setTag("overridden_dark_mode", "false");
            Sentry.addBreadcrumb("Turned off override for dark mode");
        }

        // Set up white text if dark navigation is used on a light theme.
        if (darkNavigation) {
            isDarkModeOn = true;
        }

        AppCompatDelegate.setDefaultNightMode(isDarkModeOn ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        Sentry.setTag("manual_dark_mode", String.valueOf(isDarkModeOn));
        Sentry.addBreadcrumb("Set manual dark mode to " + isDarkModeOn);
    }
}
