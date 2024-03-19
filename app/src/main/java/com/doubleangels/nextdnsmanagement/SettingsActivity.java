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
import androidx.appcompat.widget.Toolbar;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.doubleangels.nextdnsmanagement.protocoltest.VisualIndicator;
import com.jakewharton.processphoenix.ProcessPhoenix;

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
        ITransaction settingsCreateTransaction = Sentry.startTransaction("SettingsActivity_onCreate()", "SettingsActivity");
        SharedPreferences sharedPreferences = getSharedPreferences("preferences", MODE_PRIVATE);
        try {
            setupToolbar();
            setupLanguage();
            setupDarkMode(sharedPreferences);
            initializeViews();
            setupVisualIndicator();
        } catch (Exception e) {
            Sentry.captureException(e);
        } finally {
            settingsCreateTransaction.finish();
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
    }

    private void setupLanguage() {
        String appLocaleString = getResources().getConfiguration().getLocales().get(0).toString();
        String appLocaleStringResult = appLocaleString.split("_")[0];
        Locale appLocale = Locale.forLanguageTag(appLocaleStringResult);
        Locale.setDefault(appLocale);
        Configuration appConfig = new Configuration();
        appConfig.locale = appLocale;
        getResources().updateConfiguration(appConfig, getResources().getDisplayMetrics());
    }

    private void initializeViews() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commitNow();
    }

    private void setupDarkMode(SharedPreferences sharedPreferences) {
        String darkModeOverride = sharedPreferences.getString("darkmode_override", "match");
        Sentry.addBreadcrumb("Got string " + darkModeOverride + "from sharedPreferences.");
        if (darkModeOverride.contains("match")) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        } else if (darkModeOverride.contains("on")) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void setupVisualIndicator() {
        try {
            VisualIndicator visualIndicator = new VisualIndicator();
            visualIndicator.initiateVisualIndicator(this, getApplicationContext());
        } catch (Exception e) {
            Sentry.captureException(e);
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
            assert darkModePreference != null;
            setupDarkModeChangeListener(darkModePreference, sharedPreferences);
            setupPreferenceChangeListener(languageChangeListener);
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
                    try {
                        if ("whitelist_domain_1_button".equals(buttonKey) || "whitelist_domain_2_button".equals(buttonKey)) {
                            ClipboardManager clipboardManager = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                            CharSequence copiedText = getString(textResource);
                            ClipData copiedData = ClipData.newPlainText("text", copiedText);
                            clipboardManager.setPrimaryClip(copiedData);
                            Sentry.addBreadcrumb("Copied whitelist domain to clipboard.");
                            Toast.makeText(getContext(), "Text copied!", Toast.LENGTH_SHORT).show();
                        } else {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(textResource)));
                            Sentry.addBreadcrumb("Visiting " + getString(textResource) + ".");
                            startActivity(intent);
                        }
                    } catch (Exception e) {
                        Sentry.captureException(e);
                    }
                    return true;
                });
            }
        }

        private void setupDarkModeChangeListener(ListPreference setting, SharedPreferences sharedPreferences) {
            setting.setOnPreferenceChangeListener((preference, newValue) -> {
                SharedPreferences.Editor preferenceEdit = sharedPreferences.edit();
                preferenceEdit.putString("darkmode_override", newValue.toString());
                preferenceEdit.apply();
                Sentry.addBreadcrumb("Wrote string " + newValue + " to sharedPreferences.");
                ProcessPhoenix.triggerRebirth(requireContext());
                return true;
            });
        }

        private void setupPreferenceChangeListener(Preference.OnPreferenceChangeListener setting) {
            Preference preference = findPreference(SettingsActivity.SELECTED_LANGUAGE);
            if (preference != null) {
                Sentry.addBreadcrumb("Set language to " + setting.toString() + ".");
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