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
        try {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            boolean overrideDarkMode = sharedPreferences.getBoolean(OVERRIDE_DARK_MODE, false);
            boolean manualDarkMode = sharedPreferences.getBoolean(MANUAL_DARK_MODE, false);
            boolean darkNavigation = sharedPreferences.getBoolean(DARK_NAVIGATION, false);
            boolean isDarkModeOn;

            if (overrideDarkMode) {
                isDarkModeOn = manualDarkMode;
            } else {
                isDarkModeOn = (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            }

            if (darkNavigation) {
                isDarkModeOn = true;
            }

            AppCompatDelegate.setDefaultNightMode(isDarkModeOn ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        } catch (Exception e) {
            Sentry.captureException(e);
        }
    }
}
