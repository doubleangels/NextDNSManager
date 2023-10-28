// Import statements for required libraries and classes.
package com.doubleangels.nextdnsmanagement;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;
import io.sentry.Sentry;

// Definition of the DarkModeHandler class.
public class DarkModeHandler {
    // Constants for shared preferences keys.
    private static final String OVERRIDE_DARK_MODE = "override_dark_mode";
    private static final String MANUAL_DARK_MODE = "manual_dark_mode";
    private static final String DARK_NAVIGATION = "dark_navigation";

    // Method to handle the dark mode settings.
    public void handleDarkMode(Context context) {
        // Access the default shared preferences for the app.
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        // Retrieve the values of dark mode-related preferences.
        boolean overrideDarkMode = sharedPreferences.getBoolean(OVERRIDE_DARK_MODE, false);
        boolean manualDarkMode = sharedPreferences.getBoolean(MANUAL_DARK_MODE, false);
        boolean darkNavigation = sharedPreferences.getBoolean(DARK_NAVIGATION, false);
        boolean isDarkModeOn;

        // Determine the dark mode state based on user preferences.
        if (overrideDarkMode) {
            // If dark mode is overridden, use the manual setting.
            isDarkModeOn = manualDarkMode;
            Sentry.setTag("overridden_dark_mode", "true");
        } else {
            // If dark mode is not overridden, use the system-wide dark mode setting.
            isDarkModeOn = (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            Sentry.setTag("overridden_dark_mode", "false");
        }

        // Apply dark mode for navigation if specified.
        if (darkNavigation) {
            isDarkModeOn = true;
        }

        // Set the app's default night mode based on the determined state.
        AppCompatDelegate.setDefaultNightMode(isDarkModeOn ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        Sentry.setTag("manual_dark_mode", String.valueOf(isDarkModeOn));
    }
}
