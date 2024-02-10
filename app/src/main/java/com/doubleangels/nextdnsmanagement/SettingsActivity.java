package com.doubleangels.nextdnsmanagement;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.doubleangels.nextdnsmanagement.protocoltest.VisualIndicator;

import java.util.Locale;
import java.util.Objects;

import io.sentry.ITransaction;
import io.sentry.Sentry;

public class SettingsActivity extends AppCompatActivity {
    public static final String SELECTED_LANGUAGE = "selected_language";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Start a Sentry transaction for the 'onCreate' method
        ITransaction settingsCreateTransaction = Sentry.startTransaction("settings_onCreate()", "SettingsActivity");
        try {
            setupUI();
        } catch (Exception e) {
            Sentry.captureException(e); // Capture and report any exceptions to Sentry
        } finally {
            settingsCreateTransaction.finish(); // Finish the transaction
        }
    }

    private void setupUI() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);

        // Set up shared preferences
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Apply dark mode.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        // Set up selected language.
        setupSelectedLanguage(sharedPreferences);

        initializeViews(); // Initialize the views for the settings
        setVisualIndicator(); // Set the visual connection status indicator
    }

    private void setupSelectedLanguage(SharedPreferences sharedPreferences) {
        String selectedLanguage = sharedPreferences.getString(SettingsActivity.SELECTED_LANGUAGE, "en");
        Sentry.setTag("locale", selectedLanguage);
        Locale appLocale = determineLocale(selectedLanguage);
        Locale.setDefault(appLocale);
        Configuration appConfig = new Configuration();
        appConfig.locale = appLocale;
        getResources().updateConfiguration(appConfig, getResources().getDisplayMetrics());
    }

    private Locale determineLocale(String selectedLanguage) {
        if (selectedLanguage.contains("es")) {
            return new Locale(selectedLanguage, "ES");
        } else if (selectedLanguage.contains("zh")) {
            return new Locale(selectedLanguage, "HANS");
        } else if (selectedLanguage.contains("pt")) {
            return new Locale(selectedLanguage, "BR");
        }else if (selectedLanguage.contains("sv")) {
            return new Locale(selectedLanguage, "SE");
        } else {
            return new Locale(selectedLanguage);
        }
    }

    private void initializeViews() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commitNow();
    }

    private void setVisualIndicator() {
        try {
            VisualIndicator visualIndicator = new VisualIndicator();
            visualIndicator.initiateVisualIndicator(this, getApplicationContext());
        } catch (Exception e) {
            Sentry.captureException(e); // Capture and report any exceptions to Sentry
        }
    }

    private void restartActivity() {
        Intent intent = getIntent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        finish(); // Finish the current activity
        startActivity(intent); // Start a new instance of the activity with updated language settings
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        private final Preference.OnPreferenceChangeListener languageChangeListener = (preference, newValue) -> {
            // Handle preference change
            ((SettingsActivity) requireActivity()).restartActivity();
            return true;
        };


        private void setupButton(String buttonKey, int textResource) {
            Preference button = findPreference(buttonKey);
            if (button != null) {
                button.setOnPreferenceClickListener(preference -> {
                    try {
                        if ("whitelist_domain_1_button".equals(buttonKey) || "whitelist_domain_2_button".equals(buttonKey)) {
                            // Copy the text to the clipboard
                            ClipboardManager clipboardManager = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                            CharSequence copiedText = getString(textResource);
                            ClipData copiedData = ClipData.newPlainText("text", copiedText);
                            clipboardManager.setPrimaryClip(copiedData);
                            Toast.makeText(getContext(), "Text copied!", Toast.LENGTH_SHORT).show();
                        } else {
                            // Open the link
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(textResource)));
                            startActivity(intent);
                        }
                    } catch (Exception e) {
                        Sentry.captureException(e); // Capture and report any exceptions to Sentry
                    }
                    return true;
                });
            }
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            // Set up the preference change listener
            setUpPreferenceChangeListener(languageChangeListener);

            // Set up buttons
            setupButton("whitelist_domain_1_button", R.string.whitelist_domain_1);
            setupButton("whitelist_domain_2_button", R.string.whitelist_domain_2);
            setupButton("privacy_policy_button", R.string.privacy_policy_url);
            setupButton("author_button", R.string.author_url);
            setupButton("github_button", R.string.github_url);
            setupButton("donation_button", R.string.donation_link);

            String versionName = BuildConfig.VERSION_NAME;
            Preference versionPreference = findPreference("version");
            if (versionPreference != null) {
                versionPreference.setSummary(versionName);
            }
        }
        private void setUpPreferenceChangeListener(Preference.OnPreferenceChangeListener listener) {
            Preference preference = findPreference(SettingsActivity.SELECTED_LANGUAGE);
            if (preference != null) {
                preference.setOnPreferenceChangeListener(listener);
            }
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
        return super.onContextItemSelected(item);
    }
}