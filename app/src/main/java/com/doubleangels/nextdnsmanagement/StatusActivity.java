package com.doubleangels.nextdnsmanagement;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import com.doubleangels.nextdnsmanagement.protocoltest.VisualIndicator;

import java.util.Locale;
import java.util.Objects;

import io.sentry.ITransaction;
import io.sentry.Sentry;

public class StatusActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);

        // Start a Sentry transaction for the 'onCreate' method
        ITransaction helpCreateTransaction = Sentry.startTransaction("help_onCreate()", "StatusActivity");
        try {
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);

            // Set up selected language.
            String appLocaleString = getResources().getConfiguration().getLocales().get(0).toString();
            String appLocaleStringResult = appLocaleString.split("_")[0];
            Locale appLocale = Locale.forLanguageTag(appLocaleStringResult);
            Locale.setDefault(appLocale);
            Configuration appConfig = new Configuration();
            appConfig.locale = appLocale;
            getResources().updateConfiguration(appConfig, getResources().getDisplayMetrics());

            // Load user's preference for dark mode and set it
            int systemDarkMode = AppCompatDelegate.getDefaultNightMode();
            Sentry.setTag("dark_mode", systemDarkMode == AppCompatDelegate.MODE_NIGHT_YES ? "yes" : "no");
            AppCompatDelegate.setDefaultNightMode(systemDarkMode);
            setVisualIndicator(); // Set the visual connection status indicator
        } catch (Exception e) {
            Sentry.captureException(e); // Capture and report any exceptions to Sentry
        } finally {
            helpCreateTransaction.finish(); // Finish the transaction
        }
    }

    private void setVisualIndicator() {
        try {
            VisualIndicator visualIndicator = new VisualIndicator();
            visualIndicator.initiateVisualIndicator(this, getApplicationContext());
        } catch (Exception e) {
            Sentry.captureException(e); // Capture and report any exceptions to Sentry
        }
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        // Inflate the menu for the activity
        getMenuInflater().inflate(R.menu.menu_back_only, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.back) {
            // Handle the 'back' menu item, navigate to the MainActivity
            Intent mainIntent = new Intent(this, MainActivity.class);
            startActivity(mainIntent);
        }
        return super.onOptionsItemSelected(item);
    }
}
