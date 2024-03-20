package com.doubleangels.nextdnsmanagement;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import com.doubleangels.nextdnsmanagement.protocoltest.VisualIndicator;
import com.doubleangels.nextdnsmanagement.sentrymanager.SentryInitializer;
import com.doubleangels.nextdnsmanagement.sentrymanager.SentryManager;
import com.jakewharton.processphoenix.ProcessPhoenix;

import java.util.Locale;
import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {

    public static final String SELECTED_LANGUAGE = "selected_language";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        SentryManager sentryManager = new SentryManager(this);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        try {
            if (sentryManager.isSentryEnabled()) {
                SentryInitializer sentryInitializer = new SentryInitializer();
                sentryInitializer.execute(this);
            }
            setupToolbar();
            setupLanguage();
            setupDarkMode(sharedPreferences);
            initializeViews();
            setupVisualIndicator(sentryManager);
        } catch (Exception e) {
            sentryManager.captureExceptionIfEnabled(e);
        }
    }

    private void setupToolbar() {
        setSupportActionBar(findViewById(R.id.toolbar));
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
    }

    private void setupLanguage() {
        Locale defaultLocale = getResources().getConfiguration().getLocales().get(0);
        Locale.setDefault(defaultLocale);
        Configuration config = new Configuration();
        config.setLocale(defaultLocale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }

    private void initializeViews() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commitNow();
    }

    private void setupDarkMode(SharedPreferences sharedPreferences) {
        String darkModeOverride = sharedPreferences.getString("darkmode_override", "match");
        if (darkModeOverride.contains("match")) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        } else if (darkModeOverride.contains("on")) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void setupVisualIndicator(SentryManager sentryManager) {
        try {
            VisualIndicator visualIndicator = new VisualIndicator(this);
            visualIndicator.initiateVisualIndicator(this, getApplicationContext());
        } catch (Exception e) {
            sentryManager.captureExceptionIfEnabled(e);
        }
    }

    private void restartActivity() {
        Intent intent = getIntent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        finish();
        startActivity(intent);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        private final Preference.OnPreferenceChangeListener languageChangeListener = (preference, newValue) -> {
            ((SettingsActivity) requireActivity()).restartActivity();
            return true;
        };

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            SharedPreferences sharedPreferences = requireContext().getSharedPreferences("preferences", Context.MODE_PRIVATE);
            ListPreference darkModePreference = findPreference("darkmode_override");
            SwitchPreference sentryEnablePreference = findPreference("sentry_enable");
            assert darkModePreference != null;
            setupDarkModeChangeListener(darkModePreference, sharedPreferences);
            setupSentryChangeListener(sentryEnablePreference, sharedPreferences);
            setupLanguageChangeListener(languageChangeListener);
            setupButton("whitelist_domain_1_button", R.string.whitelist_domain_1);
            setupButton("whitelist_domain_2_button", R.string.whitelist_domain_2);
            setupButton("author_button", R.string.author_url);
            setupButton("github_button", R.string.github_url);
            setupButton("github_issue_button", R.string.github_issues_url);
            setupButton("donation_button", R.string.donation_url);
            setupButton("translate_button", R.string.translate_url);
            setupButton("privacy_policy_button", R.string.privacy_policy_url);
            setupButton("nextdns_privacy_policy_button", R.string.nextdns_privacy_policy_url);
            setupButton("nextdns_user_agreement_button", R.string.nextdns_user_agreement_url);
            setupButton("sentry_info_button", R.string.sentry_info_url);
            setupButton("version_button", R.string.versions_url);
            String versionName = BuildConfig.VERSION_NAME;
            Preference versionPreference = findPreference("version_button");
            if (versionPreference != null) {
                versionPreference.setSummary(versionName);
            }
        }

        private void setupButton(String buttonKey, int textResource) {
            Preference button = findPreference(buttonKey);
            if (button != null) {
                button.setOnPreferenceClickListener(preference -> {
                    if ("whitelist_domain_1_button".equals(buttonKey) || "whitelist_domain_2_button".equals(buttonKey)) {
                        ClipboardManager clipboardManager = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                        CharSequence copiedText = getString(textResource);
                        ClipData copiedData = ClipData.newPlainText("text", copiedText);
                        clipboardManager.setPrimaryClip(copiedData);
                        Toast.makeText(getContext(), "Text copied!", Toast.LENGTH_SHORT).show();
                    } else {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(textResource)));
                        startActivity(intent);
                    }
                    return true;
                });
            }
        }

        private void setupDarkModeChangeListener(ListPreference setting, SharedPreferences sharedPreferences) {
            setting.setOnPreferenceChangeListener((preference, newValue) -> {
                sharedPreferences.edit().putString("darkmode_override", newValue.toString()).apply();
                ProcessPhoenix.triggerRebirth(requireContext());
                return true;
            });
        }

        private void setupSentryChangeListener(SwitchPreference switchPreference, SharedPreferences sharedPreferences) {
            if (switchPreference != null) {
                switchPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                    boolean isChecked = (boolean) newValue;
                    SharedPreferences.Editor preferenceEdit = sharedPreferences.edit();
                    preferenceEdit.putString("sentry_enable", String.valueOf(isChecked));
                    setPreferenceVisibility("whitelist_domains", isChecked);
                    setPreferenceVisibility("whitelist_domain_1_button", isChecked);
                    setPreferenceVisibility("whitelist_domain_2_button", isChecked);
                    preferenceEdit.apply();
                    return true;
                });
            }
        }

        private void setPreferenceVisibility(String key, boolean visible) {
            Preference preference = findPreference(key);
            if (preference != null) {
                preference.setVisible(visible);
            }
        }

        private void setupLanguageChangeListener(Preference.OnPreferenceChangeListener setting) {
            Preference preference = findPreference(SettingsActivity.SELECTED_LANGUAGE);
            if (preference != null) {
                preference.setOnPreferenceChangeListener(setting);
            }
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
        return super.onContextItemSelected(item);
    }
}