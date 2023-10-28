// Import statements for required libraries and classes.
package com.doubleangels.nextdnsmanagement;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import java.util.Objects;

import io.sentry.ITransaction;
import io.sentry.Sentry;

// Definition of the HelpActivity class, which extends AppCompatActivity.
public class HelpActivity extends AppCompatActivity {
    private final DarkModeHandler darkModeHandler = new DarkModeHandler();

    // Method called when the activity is created.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Start a Sentry transaction to track this method's execution.
        ITransaction helpCreateTransaction = Sentry.startTransaction("help_onCreate()", "HelpActivity");
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

        // Set up a click listener for the status icon.
        ImageView statusIcon = findViewById(R.id.connectionStatus);
        statusIcon.setOnClickListener(v -> {
            // Create an intent to open the HelpActivity again when the status icon is clicked.
            Intent helpIntent = new Intent(v.getContext(), HelpActivity.class);
            startActivity(helpIntent);
        });

        // Finish the Sentry transaction for this method.
        helpCreateTransaction.finish();
    }

    // Method to configure the window appearance based on darkNavigation setting.
    private void setupWindow(boolean darkNavigation) {
        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);

        if (darkNavigation) {
            int colorId = R.color.darkgray;
            window.setStatusBarColor(ContextCompat.getColor(this, colorId));
            window.setNavigationBarColor(ContextCompat.getColor(this, colorId));
            toolbar.setBackgroundColor(ContextCompat.getColor(this, colorId));
            Sentry.setTag("dark_navigation", "true");
        } else {
            int colorId = R.color.blue;
            toolbar.setBackgroundColor(ContextCompat.getColor(this, colorId));
            Sentry.setTag("dark_navigation", "false");
        }
    }

    // Method called when the activity is resumed.
    @Override
    protected void onResume() {
        super.onResume();
        // Handle the dark mode settings using the DarkModeHandler.
        darkModeHandler.handleDarkMode(this);
    }

    // Method to create the options menu.
    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu_back_only, menu);
        return true;
    }

    // Method to handle menu item selection.
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
