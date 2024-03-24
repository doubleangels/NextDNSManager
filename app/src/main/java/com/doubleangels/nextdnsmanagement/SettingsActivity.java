package com.doubleangels.nextdnsmanagement;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
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
import com.doubleangels.nextdnsmanagement.sentry.SentryInitializer;
import com.doubleangels.nextdnsmanagement.sentry.SentryManager;
import com.jakewharton.processphoenix.ProcessPhoenix;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

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
            setupLanguage();
            setupDarkMode(sharedPreferences);
            setupVisualIndicator(sentryManager);
            initializeViews();
        } catch (Exception e) {
            sentryManager.captureExceptionIfEnabled(e);
        }
    }

    private void setupLanguage() {
        Resources resources = getResources();
        Configuration configuration = resources.getConfiguration();
        Locale appLocale = configuration.getLocales().get(0);
        if (appLocale != null) {
            Locale.setDefault(appLocale);
            configuration.setLocale(appLocale);
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        }
    }

    private void initializeViews() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commitNow();
    }

    private void setupDarkMode(SharedPreferences sharedPreferences) {
        String darkModeOverride = sharedPreferences.getString("darkmode_override", "match");
        int defaultNightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        if (darkModeOverride.contains("on")) {
            defaultNightMode = AppCompatDelegate.MODE_NIGHT_YES;
        } else if (darkModeOverride.contains("off")) {
            defaultNightMode = AppCompatDelegate.MODE_NIGHT_NO;
        }
        AppCompatDelegate.setDefaultNightMode(defaultNightMode);
    }

    private void setupVisualIndicator(SentryManager sentryManager) {
        try {
            new VisualIndicator(this).initiateVisualIndicator(this, getApplicationContext());
        } catch (Exception e) {
            sentryManager.captureExceptionIfEnabled(e);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
            ListPreference darkModePreference = findPreference("darkmode_override");
            SwitchPreference sentryEnablePreference = findPreference("sentry_enable");
            assert darkModePreference != null;
            setupDarkModeChangeListener(darkModePreference, sharedPreferences);
            setupSentryChangeListener(sentryEnablePreference, sharedPreferences);
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
                    if (buttonKey.startsWith("whitelist_domain")) {
                        copyToClipboard(textResource);
                    } else {
                        openLink(textResource);
                    }
                    return true;
                });
            }
        }

        private void copyToClipboard(int textResource) {
            ClipboardManager clipboardManager = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            CharSequence copiedText = getString(textResource);
            ClipData copiedData = ClipData.newPlainText("text", copiedText);
            clipboardManager.setPrimaryClip(copiedData);
            Toast.makeText(getContext(), "Text copied!", Toast.LENGTH_SHORT).show();
        }

        private void openLink(int textResource) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(textResource)));
            startActivity(intent);
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
                    boolean isVisible= (boolean) newValue;
                    SharedPreferences.Editor preferenceEdit = sharedPreferences.edit();
                    preferenceEdit.putString("sentry_enable", String.valueOf(isVisible));
                    preferenceEdit.apply();
                    return true;
                });
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