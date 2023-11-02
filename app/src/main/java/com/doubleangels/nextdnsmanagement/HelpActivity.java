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
        // Start a Sentry transaction to track this method's execution.
        ITransaction helpCreateTransaction = Sentry.startTransaction("help_onCreate()", "HelpActivity");
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_help);

            // Access the app's shared preferences.
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            boolean darkNavigation = sharedPreferences.getBoolean(SettingsActivity.DARK_NAVIGATION, false);

            // Set up the window appearance based on darkNavigation setting.
            setupWindow(darkNavigation);

            // Initialize a visual indicator for the activity.
            VisualIndicator visualIndicator = new VisualIndicator();
            visualIndicator.initiateVisualIndicator(this, getApplicationContext());
        } catch (Exception e) {
            Sentry.captureException(e);
        } finally {
            // Finish the Sentry transaction for this method, whether there was an exception or not.
            helpCreateTransaction.finish();
        }
    }

    private void setupWindow(boolean darkNavigation) {
        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        Toolbar toolbar = findViewById(R.id.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);

        int colorId;
        if (darkNavigation) {
            colorId = R.color.darkgray;
            window.setStatusBarColor(ContextCompat.getColor(this, colorId));
            window.setNavigationBarColor(ContextCompat.getColor(this, colorId));
        } else {
            colorId = R.color.blue;
        }
        toolbar.setBackgroundColor(ContextCompat.getColor(this, colorId));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Handle the dark mode settings using the DarkModeHandler.
        darkModeHandler.handleDarkMode(this);
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu_back_only, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.back) {
            // Create an intent to navigate to the MainActivity when the "back" item is selected.
            Intent mainIntent = new Intent(this, MainActivity.class);
            startActivity(mainIntent);
        }
        return super.onContextItemSelected(item);
    }
}
