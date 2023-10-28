package com.doubleangels.nextdnsmanagement;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;
import io.sentry.Sentry;
import io.sentry.Breadcrumb;

public class DarkModeHandler {
    private static final String OVERRIDE_DARK_MODE = "override_dark_mode";
    private static final String MANUAL_DARK_MODE = "manual_dark_mode";
    private static final String DARK_NAVIGATION = "dark_navigation";

    public void handleDarkMode(Context context) {
        try {
            // Create a breadcrumb to track entering the 'handleDarkMode' method.
            Breadcrumb breadcrumb = new Breadcrumb();
            breadcrumb.setMessage("Entering handleDarkMode method");
            Sentry.addBreadcrumb(breadcrumb);

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            boolean overrideDarkMode = sharedPreferences.getBoolean(OVERRIDE_DARK_MODE, false);
            boolean manualDarkMode = sharedPreferences.getBoolean(MANUAL_DARK_MODE, false);
            boolean darkNavigation = sharedPreferences.getBoolean(DARK_NAVIGATION, false);
            boolean isDarkModeOn;

            if (overrideDarkMode) {
                isDarkModeOn = manualDarkMode;
                Sentry.setTag("overridden_dark_mode", "true");
            } else {
                isDarkModeOn = (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
                Sentry.setTag("overridden_dark_mode", "false");
            }

            if (darkNavigation) {
                isDarkModeOn = true;
            }

            AppCompatDelegate.setDefaultNightMode(isDarkModeOn ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

            // Add a breadcrumb to track the result of the dark mode calculation.
            Breadcrumb resultBreadcrumb = new Breadcrumb();
            resultBreadcrumb.setMessage("Dark mode set to " + isDarkModeOn);
            Sentry.addBreadcrumb(resultBreadcrumb);

            Sentry.setTag("manual_dark_mode", String.valueOf(isDarkModeOn));
        } catch (Exception e) {
            // Capture and report any exceptions to Sentry with a breadcrumb.
            Breadcrumb errorBreadcrumb = new Breadcrumb();
            errorBreadcrumb.setMessage("An exception occurred in handleDarkMode method");
            Sentry.addBreadcrumb(errorBreadcrumb);
            Sentry.captureException(e);
        }
    }
}
