package com.doubleangels.nextdnsmanagement;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

/**
 * Base activity that enables edge-to-edge display with brand-aware system bar styling.
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(
                this,
                SystemBarStyle.auto(
                        ContextCompat.getColor(this, R.color.main),
                        ContextCompat.getColor(this, R.color.main_dark)),
                SystemBarStyle.auto(
                        ContextCompat.getColor(this, R.color.white),
                        ContextCompat.getColor(this, R.color.main_dark)));
        super.onCreate(savedInstanceState);
    }

    /**
     * Applies a dark mode preference value ("match", "on", or anything else for
     * off) app-wide.
     */
    protected static void applyDarkMode(String darkMode) {
        int mode;
        if ("match".equals(darkMode)) {
            mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        } else if ("on".equals(darkMode)) {
            mode = AppCompatDelegate.MODE_NIGHT_YES;
        } else {
            mode = AppCompatDelegate.MODE_NIGHT_NO;
        }
        AppCompatDelegate.setDefaultNightMode(mode);
    }
}
