package com.doubleangels.nextdnsmanagement;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;
import io.sentry.Sentry;

public class DarkModeHandler {
    // Keys for shared preferences
    private static final String OVERRIDE_DARK_MODE = "override_dark_mode";
    private static final String MANUAL_DARK_MODE = "manual_dark_mode";
    private static final String DARK_NAVIGATION = "dark_navigation";

    // Method to handle dark mode settings
    public void handleDarkMode(Context context) {
        try {
            // Get the default shared preferences for the app
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

            // Retrieve settings from shared preferences
            boolean overrideDarkMode = sharedPreferences.getBoolean(OVERRIDE_DARK_MODE, false);
            boolean manualDarkMode = sharedPreferences.getBoolean(MANUAL_DARK_MODE, false);
            boolean darkNavigation = sharedPreferences.getBoolean(DARK_NAVIGATION, false);
            boolean isDarkModeOn;

            // Determine if dark mode should be enabled
            if (overrideDarkMode) {
                // If override is enabled, use manualDarkMode setting
                isDarkModeOn = manualDarkMode;
            } else {
                // If override is not enabled, check the system's night mode setting
                isDarkModeOn = (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            }

            // Check for dark navigation setting
            if (darkNavigation) {
                isDarkModeOn = true;
            }

            // Set the app's default night mode based on the final decision
            AppCompatDelegate.setDefaultNightMode(isDarkModeOn ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        } catch (Exception e) {
            // Capture and report any exceptions to Sentry for error tracking
            Sentry.captureException(e);
        }
    }
}
