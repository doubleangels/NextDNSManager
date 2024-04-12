package com.doubleangels.nextdnsmanagement;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.LifecycleOwner;

import com.doubleangels.nextdnsmanagement.protocoltest.VisualIndicator;
import com.doubleangels.nextdnsmanagement.sentry.SentryInitializer;
import com.doubleangels.nextdnsmanagement.sentry.SentryManager;

import java.util.Locale;
import java.util.Objects;

public class StatusActivity extends AppCompatActivity {

    public SentryManager sentryManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);
        sentryManager = new SentryManager(this);
        SharedPreferences sharedPreferences = this.getSharedPreferences("preferences", Context.MODE_PRIVATE);
        try {
            if (sentryManager.isEnabled()) {
                SentryInitializer.initialize(this);
            }
            setupToolbarForActivity();
            String appLocale = setupLanguageForActivity();
            sentryManager.captureMessage("Using locale: " + appLocale);
            setupDarkModeForActivity(sharedPreferences);
            setupVisualIndicatorForActivity(sentryManager, this);
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
    }

    private void setupToolbarForActivity() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
    }

    private String setupLanguageForActivity() {
        Configuration config = getResources().getConfiguration();
        Locale appLocale = config.getLocales().get(0);
        Locale.setDefault(appLocale);
        Configuration newConfig = new Configuration(config);
        newConfig.setLocale(appLocale);
        new ContextThemeWrapper(getBaseContext(), R.style.AppTheme).applyOverrideConfiguration(newConfig);
        return appLocale.getLanguage();
    }

    private void setupDarkModeForActivity(SharedPreferences sharedPreferences) {
        String darkMode = sharedPreferences.getString("dark_mode", "match");
        if (darkMode.contains("match")) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        } else if (darkMode.contains("on")) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }


    private void setupVisualIndicatorForActivity(SentryManager sentryManager, LifecycleOwner lifecycleOwner) {
        try {
            new VisualIndicator(this).initialize(this, lifecycleOwner, this);
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu_back_only, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.back) {
            Intent mainIntent = new Intent(this, MainActivity.class);
            startActivity(mainIntent);
        }
        return super.onOptionsItemSelected(item);
    }
}
