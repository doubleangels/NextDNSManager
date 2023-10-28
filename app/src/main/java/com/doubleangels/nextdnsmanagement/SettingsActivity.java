// Import statements for required libraries and classes.
package com.doubleangels.nextdnsmanagement;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import java.util.Objects;

import io.sentry.ITransaction;
import io.sentry.Sentry;

// Definition of the SettingsActivity class, which extends AppCompatActivity.
public class SettingsActivity extends AppCompatActivity {
    public DarkModeHandler darkModeHandler = new DarkModeHandler();
    public Boolean darkNavigation;

    // Definition of constants for shared preferences keys.
    public static final String DARK_NAVIGATION = "dark_navigation";
    public static final String OVERRIDE_DARK_MODE = "override_dark_mode";
    public static final String MANUAL_DARK_MODE = "manual_dark_mode";

    // Method called when the activity is created.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ITransaction settingsCreateTransaction = Sentry.startTransaction("settings_onCreate()", "SettingsActivity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        try {
            initializeViews(savedInstanceState);
            setActionBar();
            setVisualIndicator();
        } catch (Exception e) {
            Sentry.captureException(e);
        } finally {
            settingsCreateTransaction.finish();
        }
    }

    // Method to initialize views and preferences.
    private void initializeViews(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
    }

    // Method to set up the action bar and window styles.
    private void setActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        darkNavigation = sharedPreferences.getBoolean(DARK_NAVIGATION, false);
        setWindowAndToolbar(darkNavigation);
    }

    // Method to set window and toolbar styles based on dark navigation.
    private void setWindowAndToolbar(boolean isDark) {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        int statusBarColor;
        int toolbarColor;

        if (isDark) {
            statusBarColor = ContextCompat.getColor(this, R.color.darkgray);
            toolbarColor = ContextCompat.getColor(this, R.color.darkgray);
        } else {
            statusBarColor = ContextCompat.getColor(this, R.color.blue);
            toolbarColor = ContextCompat.getColor(this, R.color.blue);
        }

        window.setStatusBarColor(statusBarColor);
        window.setNavigationBarColor(statusBarColor);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        toolbar.setBackgroundColor(toolbarColor);
        Sentry.setTag("dark_navigation", String.valueOf(isDark));
    }

    // Method to set up a visual indicator.
    private void setVisualIndicator() {
        VisualIndicator visualIndicator = new VisualIndicator();
        visualIndicator.initiateVisualIndicator(this, getApplicationContext());
    }

    // Method called when the activity is resumed.
    @Override
    protected void onResume() {
        super.onResume();
        darkModeHandler.handleDarkMode(this);
    }

    // Nested SettingsFragment class to handle preferences.
    public static class SettingsFragment extends PreferenceFragmentCompat {
        // Method to create preferences from a resource and set up buttons.
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            setupButton("whitelist_domain_1_button", R.string.whitelist_domain_1);
            setupButton("whitelist_domain_2_button", R.string.whitelist_domain_2);
            setupButton("privacy_policy_button", R.string.privacy_policy_url);
            setupButton("author_button", R.string.author_url);
            setupButton("github_button", R.string.github_url);

            // Set the version name.
            String versionName = BuildConfig.VERSION_NAME;
            Preference versionPreference = findPreference("version");
            if (versionPreference != null) {
                versionPreference.setSummary(versionName);
            }
        }

        // Method to set up buttons and handle click events.
        private void setupButton(String buttonKey, int textResource) {
            Preference button = findPreference(buttonKey);
            if (button != null) {
                button.setOnPreferenceClickListener(preference -> {
                    ClipboardManager clipboardManager = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    CharSequence copiedText = getString(textResource);
                    ClipData copiedData = ClipData.newPlainText("text", copiedText);
                    clipboardManager.setPrimaryClip(copiedData);
                    Sentry.addBreadcrumb(buttonKey + " copied to clipboard");
                    Toast.makeText(getContext(), "Text copied!", Toast.LENGTH_SHORT).show();
                    if (buttonKey.equals("privacy_policy_button") || buttonKey.equals("author_button") || buttonKey.equals("github_button")) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(textResource)));
                        Sentry.addBreadcrumb("Visited " + buttonKey);
                        startActivity(intent);
                    }
                    return true;
                });
            }
        }
    }

    // Method to create the options menu.
    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu_back_only, menu);
        return true;
    }

    // Method to handle menu item selection, e.g., back button.
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.back) {
            Intent mainIntent = new Intent(this, MainActivity.class);
            startActivity(mainIntent);
        }
        return super.onContextItemSelected(item);
    }
}
