package com.doubleangels.nextdnsmanagement;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.doubleangels.nextdnsmanagement.sharedpreferences.SharedPreferencesManager;
import com.doubleangels.nextdnsmanagement.sentry.SentryManager;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runs encrypted SharedPreferences initialization off the main thread while the
 * system splash screen remains visible.
 */
public final class AppStartupHelper {

    public interface StartupCallback {
        void onPreferencesReady();
    }

    private AppStartupHelper() {
    }

    public static void initializePreferencesAsync(AppCompatActivity activity,
            SplashScreen splashScreen,
            StartupCallback callback) {
        final AtomicBoolean preferencesReady = new AtomicBoolean(false);
        if (splashScreen != null) {
            splashScreen.setKeepOnScreenCondition(() -> !preferencesReady.get());
        }

        new Thread(() -> {
            try {
                SharedPreferencesManager.init(activity.getApplicationContext());
            } catch (Exception e) {
                SentryManager.captureException(e);
            }
            activity.runOnUiThread(() -> {
                preferencesReady.set(true);
                callback.onPreferencesReady();
            });
        }, "prefs-init").start();
    }
}
