package com.doubleangels.nextdnsmanagement;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import java.util.Objects;

import io.sentry.ITransaction;
import io.sentry.Sentry;

public class HelpActivity extends AppCompatActivity {
    private final DarkModeHandler darkModeHandler = new DarkModeHandler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Start a Sentry transaction to monitor this method
        ITransaction helpCreateTransaction = Sentry.startTransaction("help_onCreate()", "HelpActivity");
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_help);

            // Get shared preferences to check if dark navigation is enabled
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            boolean darkNavigation = sharedPreferences.getBoolean(SettingsActivity.DARK_NAVIGATION, false);

            // Set up the window appearance, including navigation bar and status bar color
            setupWindow(darkNavigation);

            // Initialize a visual indicator for the help activity
            VisualIndicator visualIndicator = new VisualIndicator();
            visualIndicator.initiateVisualIndicator(this, getApplicationContext());
        } catch (Exception e) {
            // Capture and report any exceptions to Sentry for error tracking
            Sentry.captureException(e);
        } finally {
            // Finish the Sentry transaction
            helpCreateTransaction.finish();
        }
    }

    private void setupWindow(boolean darkNavigation) {
        // Set up the window properties and appearance
        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        Toolbar toolbar = findViewById(R.id.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);

        int colorId;
        if (darkNavigation) {
            // Set colors for dark navigation mode
            colorId = R.color.darkgray;
            window.setStatusBarColor(ContextCompat.getColor(this, colorId));
            window.setNavigationBarColor(ContextCompat.getColor(this, colorId));
        } else {
            // Set colors for the default mode
            colorId = R.color.blue;
        }
        toolbar.setBackgroundColor(ContextCompat.getColor(this, colorId));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Handle dark mode settings when the activity is resumed
        darkModeHandler.handleDarkMode(this);
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        // Inflate the menu for this activity
        getMenuInflater().inflate(R.menu.menu_back_only, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.back) {
            // Handle the "back" menu item to navigate to the main activity
            Intent mainIntent = new Intent(this, MainActivity.class);
            startActivity(mainIntent);
        }
        return super.onOptionsItemSelected(item);
    }
}
