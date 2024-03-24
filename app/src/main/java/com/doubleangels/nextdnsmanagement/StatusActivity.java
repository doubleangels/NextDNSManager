package com.doubleangels.nextdnsmanagement;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import com.doubleangels.nextdnsmanagement.protocoltest.VisualIndicator;
import com.doubleangels.nextdnsmanagement.sentry.SentryInitializer;
import com.doubleangels.nextdnsmanagement.sentry.SentryManager;

import java.util.Locale;

public class StatusActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);
        SentryManager sentryManager = new SentryManager(this);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        try {
            if (sentryManager.isSentryEnabled()) {
                SentryInitializer sentryInitializer = new SentryInitializer();
                sentryInitializer.execute(this);
            }
            setupLanguage();
            setupDarkMode(sharedPreferences);
            setupVisualIndicator(sentryManager);
        } catch (Exception e) {
            sentryManager.captureExceptionIfEnabled(e);
        }
    }

    private void setupLanguage() {
        Resources resources = getResources();
        Configuration configuration = resources.getConfiguration();
        Locale appLocale = configuration.getLocales().get(0);
        if (appLocale != null) {
            Locale.setDefault(appLocale);
            configuration.setLocale(appLocale);
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        }
    }

    private void setupDarkMode(SharedPreferences sharedPreferences) {
        String darkModeOverride = sharedPreferences.getString("darkmode_override", "match");
        int defaultNightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        if (darkModeOverride.contains("on")) {
            defaultNightMode = AppCompatDelegate.MODE_NIGHT_YES;
        } else if (darkModeOverride.contains("off")) {
            defaultNightMode = AppCompatDelegate.MODE_NIGHT_NO;
        }
        AppCompatDelegate.setDefaultNightMode(defaultNightMode);
    }


    private void setupVisualIndicator(SentryManager sentryManager) {
        try {
            new VisualIndicator(this).initiateVisualIndicator(this, getApplicationContext());
        } catch (Exception e) {
            sentryManager.captureExceptionIfEnabled(e);
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
