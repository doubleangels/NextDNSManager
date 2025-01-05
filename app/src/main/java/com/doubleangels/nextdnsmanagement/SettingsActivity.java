package com.doubleangels.nextdnsmanagement;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import com.doubleangels.nextdnsmanagement.protocol.VisualIndicator;
import com.doubleangels.nextdnsmanagement.sentry.SentryInitializer;
import com.doubleangels.nextdnsmanagement.sentry.SentryManager;

import java.util.Locale;
import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {

    // SentryManager instance for error tracking
    public SentryManager sentryManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize SentryManager for error tracking
        sentryManager = new SentryManager(this);
        // Get SharedPreferences for storing app preferences
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        try {
            // Check if Sentry is enabled and initialize it
            if (sentryManager.isEnabled()) {
                SentryInitializer.initialize(this);
            }
            // Setup toolbar
            setupToolbarForActivity();
            // Setup language/locale
            String appLocale = setupLanguageForActivity();
            sentryManager.captureMessage("Using locale: " + appLocale);
            // Setup dark mode
            setupDarkModeForActivity(sharedPreferences);
            // Initialize views (PreferenceFragment)
            initializeViews();
            // Setup visual indicator
            setupVisualIndicatorForActivity(sentryManager, this);
        } catch (Exception e) {
            // Catch and log exceptions
            sentryManager.captureException(e);
        }
    }

    // Setup toolbar for the activity
    private void setupToolbarForActivity() {
        setSupportActionBar(findViewById(R.id.toolbar));
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
    }

    // Setup language/locale for the activity
    private String setupLanguageForActivity() {
        Configuration config = getResources().getConfiguration();
        Locale appLocale = config.getLocales().get(0);
        Locale.setDefault(appLocale);
        Configuration newConfig = new Configuration(config);
        newConfig.setLocale(appLocale);
        new ContextThemeWrapper(getBaseContext(), R.style.AppTheme).applyOverrideConfiguration(newConfig);
        return appLocale.getLanguage();
    }

    // Setup dark mode for the activity based on user preferences
    private void setupDarkModeForActivity(SharedPreferences sharedPreferences) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            String darkMode = sharedPreferences.getString("dark_mode", "match");
            sentryManager.captureMessage("Dark mode setting: " + darkMode);
            if (darkMode.contains("match")) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            } else if (darkMode.contains("on")) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        }
    }

    // Setup visual indicator for the activity
    private void setupVisualIndicatorForActivity(SentryManager sentryManager, LifecycleOwner lifecycleOwner) {
        try {
            VisualIndicator visualIndicator = new VisualIndicator(this);
            visualIndicator.initialize(this, lifecycleOwner, this);
        } catch (Exception e) {
            // Catch and log exceptions
            sentryManager.captureException(e);
        }
    }

    // Initialize views (PreferenceFragment)
    private void initializeViews() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commitNow();
    }

    // Inner class representing the preference fragment
    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            // Load preferences from XML resource
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            // Get the default SharedPreferences
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());

            // Set initial visibility for certain preferences based on user settings
            setInitialSentryVisibility(sharedPreferences);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S_V2) {
                // Find the PreferenceCategory by its key
                PreferenceCategory darkModePreferenceCategory = findPreference("darkmode");
                if (darkModePreferenceCategory != null) {
                    darkModePreferenceCategory.setVisible(false);
                }
            } else {
                ListPreference darkModePreference = findPreference("dark_mode");
                if (darkModePreference != null) {
                    setupDarkModeChangeListener(darkModePreference, sharedPreferences);
                }
            }

            // Find preferences and set up listeners
            SwitchPreference sentryEnablePreference = findPreference("sentry_enable");
            if (sentryEnablePreference != null) {
                setupSentryChangeListener(sentryEnablePreference, sharedPreferences);
            }

            // Set up click listeners for various buttons
            setupButton("whitelist_domain_1_button", R.string.whitelist_domain_1);
            setupButton("whitelist_domain_2_button", R.string.whitelist_domain_2);
            setupButton("sentry_info_button", R.string.sentry_info_url);
            setupButtonForIntent("author_button");
            setupButton("github_button", R.string.github_url);
            setupButton("github_issue_button", R.string.github_issues_url);
            setupButton("donation_button", R.string.donation_url);
            setupButton("translate_button", R.string.translate_url);
            setupButton("privacy_policy_button", R.string.privacy_policy_url);
            setupButton("nextdns_privacy_policy_button", R.string.nextdns_privacy_policy_url);
            setupButton("nextdns_user_agreement_button", R.string.nextdns_user_agreement_url);
            setupButtonForIntent("permission_button");
            setupButton("version_button", R.string.versions_url);
            // Set version name as summary for version button
            String versionName = BuildConfig.VERSION_NAME;
            Preference versionPreference = findPreference("version_button");
            if (versionPreference != null) {
                versionPreference.setSummary(versionName);
            }
        }

        // Set initial visibility for Sentry-related preferences
        private void setInitialSentryVisibility(SharedPreferences sharedPreferences) {
            boolean visibility = sharedPreferences.getBoolean("sentry_enable", false);
            setPreferenceVisibility("whitelist_domains", visibility);
            setPreferenceVisibility("whitelist_domain_1_button", visibility);
            setPreferenceVisibility("whitelist_domain_2_button", visibility);
        }

        // Set up click listener for a button preference
        private void setupButton(String buttonKey, int textResource) {
            Preference button = findPreference(buttonKey);
            assert button != null;
            button.setOnPreferenceClickListener(preference -> {
                if ("whitelist_domain_1_button".equals(buttonKey) || "whitelist_domain_2_button".equals(buttonKey)) {
                    // Copy text to clipboard for whitelist buttons
                    ClipboardManager clipboardManager = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    CharSequence copiedText = getString(textResource);
                    ClipData copiedData = ClipData.newPlainText("text", copiedText);
                    clipboardManager.setPrimaryClip(copiedData);
                    Toast.makeText(getContext(), "Text copied!", Toast.LENGTH_SHORT).show();
                } else {
                    // Open URL in browser for other buttons
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(textResource)));
                    startActivity(intent);
                }
                return true;
            });
        }

        // Set up click listener for a button preference that starts an intent
        private void setupButtonForIntent(String buttonKey) {
            Preference button = findPreference(buttonKey);
            assert button != null;
            button.setOnPreferenceClickListener(preference -> {
                if ("author_button".equals(buttonKey)) {
                    Intent intent = new Intent(getContext(), AuthorActivity.class);
                    startActivity(intent);
                }
                if ("permission_button".equals(buttonKey)) {
                    Intent intent = new Intent(getContext(), PermissionActivity.class);
                    startActivity(intent);
                }
                return true;
            });
        }

        // Set up listener for dark mode preference changes
        private void setupDarkModeChangeListener(ListPreference setting, SharedPreferences sharedPreferences) {
            setting.setOnPreferenceChangeListener((preference, newValue) -> {
                Log.i("Output","Output: " + newValue.toString());
                // Store new value in SharedPreferences
                sharedPreferences.edit().putString("dark_mode", newValue.toString()).apply();
                // Restart activity to apply changes
                requireActivity().recreate();
                return true;
            });
        }

        // Set up listener for Sentry enable/disable preference changes
        private void setupSentryChangeListener(SwitchPreference switchPreference, SharedPreferences sharedPreferences) {
            if (switchPreference != null) {
                switchPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                    boolean isEnabled = (boolean) newValue;
                    // Store new value in SharedPreferences and adjust visibility of related preferences
                    SharedPreferences.Editor preferenceEdit = sharedPreferences.edit();
                    preferenceEdit.putBoolean("sentry_enable", isEnabled);
                    setPreferenceVisibility("whitelist_domains", isEnabled);
                    setPreferenceVisibility("whitelist_domain_1_button", isEnabled);
                    setPreferenceVisibility("whitelist_domain_2_button", isEnabled);
                    preferenceEdit.apply();
                    return true;
                });
            }
        }

        // Set visibility of a preference
        private void setPreferenceVisibility(String key, Boolean visibility) {
            Preference preference = findPreference(key);
            if (preference != null) {
                preference.setVisible(visibility);
            }
        }
    }

    // Inflate menu for the activity
    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu_back_only, menu);
        return true;
    }

    // Handle menu item selection
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.back) {
            // Navigate back to MainActivity
            Intent mainIntent = new Intent(this, MainActivity.class);
            startActivity(mainIntent);
        }
        return super.onContextItemSelected(item);
    }
}
