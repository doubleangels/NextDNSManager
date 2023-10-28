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

public class HelpActivity extends AppCompatActivity {
    private final DarkModeHandler darkModeHandler = new DarkModeHandler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ITransaction helpCreateTransaction = Sentry.startTransaction("help_onCreate()", "HelpActivity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        // Get shared preferences.
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean darkNavigation = sharedPreferences.getBoolean(SettingsActivity.DARK_NAVIGATION, false);

        // Set up our window, status bar, and toolbar.
        setupWindow(darkNavigation);

        // Set up the visual indicator.
        VisualIndicator visualIndicator = new VisualIndicator();
        visualIndicator.initiateVisualIndicator(this, getApplicationContext());

        // Let us touch the visual indicator to open an explanation.
        ImageView statusIcon = findViewById(R.id.connectionStatus);
        statusIcon.setOnClickListener(v -> {
                    Intent helpIntent = new Intent(v.getContext(), HelpActivity.class);
                    startActivity(helpIntent);
                });

                helpCreateTransaction.finish();
    }

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
            Sentry.addBreadcrumb("Turned on dark navigation");
        } else {
            int colorId = R.color.blue;
            toolbar.setBackgroundColor(ContextCompat.getColor(this, colorId));
            Sentry.setTag("dark_navigation", "false");
            Sentry.addBreadcrumb("Turned off dark navigation");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
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
            Intent mainIntent = new Intent(this, MainActivity.class);
            startActivity(mainIntent);
        }
        return super.onContextItemSelected(item);
    }
}
