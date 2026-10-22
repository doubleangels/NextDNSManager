package com.doubleangels.nextdnsmanagement;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import com.doubleangels.nextdnsmanagement.biometriclock.BiometricLock;
import com.doubleangels.nextdnsmanagement.sentry.SentryInitializer;
import com.doubleangels.nextdnsmanagement.sentry.SentryManager;
import com.doubleangels.nextdnsmanagement.sharedpreferences.SecurePreferenceDataStore;
import com.doubleangels.nextdnsmanagement.sharedpreferences.SharedPreferencesManager;
import com.doubleangels.nextdnsmanagement.utils.ExternalLinkHandler;
import com.doubleangels.nextdnsmanagement.utils.InsetsHelper;

/**
 * Activity for application settings. It initializes shared preferences, sets up
 * error logging (Sentry),
 * configures dark mode settings, and loads the SettingsFragment.
 */
public class SettingsActivity extends BaseActivity {

    /**
     * Called when the activity is created. Sets up shared preferences, error
     * logging,
     * dark mode configuration, and initializes the settings views.
     *
     * @param savedInstanceState Bundle containing saved state data, if any.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setupInsets();

        if (SharedPreferencesManager.isInitialized()) {
            finishSettingsStartup();
        } else {
            AppStartupHelper.initializePreferencesAsync(this, null, this::finishSettingsStartup);
        }
    }

    private void finishSettingsStartup() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        try {
            applyDarkMode(SharedPreferencesManager.getString("dark_mode", "match"));
        } catch (Exception e) {
            SentryManager.captureException(e);
        }
        try {
            initializeViews();
        } catch (Exception e) {
            SentryManager.captureException(e);
        }
    }

    private void setupInsets() {
        View root = findViewById(R.id.root);
        InsetsHelper.installOnRoot(root);
        View settingsContainer = findViewById(R.id.settings);
        InsetsHelper.applySystemBarPadding(settingsContainer);
    }

    /**
     * Initializes the settings fragment view.
     */
    private void initializeViews() {
        // Replace the container with the SettingsFragment
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
    }

    /**
     * Fragment for displaying and handling settings preferences.
     */
    public static class SettingsFragment extends PreferenceFragmentCompat {

        /**
         * Called during fragment creation to initialize the preference hierarchy from
         * an XML resource.
         *
         * @param savedInstanceState Saved state, if any.
         * @param rootKey            The key of the preference hierarchy.
         */
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            if (!SharedPreferencesManager.isInitialized()) {
                return;
            }
            // Must be set before inflating so the switches read the encrypted store
            // rather than a plaintext default SharedPreferences file.
            getPreferenceManager().setPreferenceDataStore(new SecurePreferenceDataStore());
            try {
                // Load preferences from the XML resource
                setPreferencesFromResource(R.xml.root_preferences, rootKey);
            } catch (Exception e) {
                SentryManager.captureException(e);
            }
            try {
                // Set the initial visibility of Sentry-related preferences
                setInitialSentryVisibility(SharedPreferencesManager.getBoolean("sentry_enable", false));
            } catch (Exception e) {
                SentryManager.captureException(e);
            }
            try {
                // Retrieve references to various preferences
                SwitchPreference sentryEnablePreference = findPreference("sentry_enable");
                SwitchPreference appLockPreference = findPreference("app_lock_enable");
                ListPreference darkModePreference = findPreference("dark_mode");
                final BiometricLock biometricLock = new BiometricLock((AppCompatActivity) requireContext());

                // Hide app lock when storage is not encrypted or biometrics are unavailable
                if (!SharedPreferencesManager.isUsingEncryptedStorage() || !biometricLock.canAuthenticate()) {
                    setPreferenceVisibility("applock", false);
                }
                // Set up change listeners for each preference
                if (sentryEnablePreference != null) {
                    setupSentryChangeListener(sentryEnablePreference);
                }
                if (appLockPreference != null) {
                    setupAppLockChangeListener(appLockPreference);
                }
                if (darkModePreference != null) {
                    setupDarkModeChangeListener(darkModePreference);
                }
                // Setup buttons that perform various actions
                setupButton("whitelist_domain_1_button", R.string.whitelist_domain_1);
                setupButton("whitelist_domain_2_button", R.string.whitelist_domain_2);
                setupButton("sentry_info_button", R.string.sentry_info_url);
                setupButtonForIntent("author_button");
                setupButton("feedback_button", R.string.tally_feedback_url);
                setupButton("github_button", R.string.github_url);
                setupButton("github_issue_button", R.string.github_issues_url);
                setupButton("donation_button", R.string.donation_url);
                setupButton("redact_button", R.string.redact_url);
                setupButton("tally_issues_button", R.string.tally_issues_url);
                setupButton("translate_button", R.string.tally_incorrect_translation_url);
                setupButton("privacy_policy_button", R.string.privacy_policy_url);
                setupButton("nextdns_privacy_policy_button", R.string.nextdns_privacy_policy_url);
                setupButton("nextdns_user_agreement_button", R.string.nextdns_user_agreement_url);
                setupButtonForIntent("permission_button");
                setupButton("version_button", R.string.versions_url);
                // Set the version name as the summary for the version preference
                String versionName = BuildConfig.VERSION_NAME;
                Preference versionPreference = findPreference("version_button");
                if (versionPreference != null) {
                    versionPreference.setSummary(versionName);
                }
            } catch (Exception e) {
                SentryManager.captureException(e);
            }
        }

        /**
         * Sets the initial visibility of Sentry-related preferences.
         *
         * @param visibility The visibility flag for Sentry preferences.
         */
        private void setInitialSentryVisibility(Boolean visibility) {
            setPreferenceVisibility("whitelist_domains", visibility);
            setPreferenceVisibility("whitelist_domain_1_button", visibility);
            setPreferenceVisibility("whitelist_domain_2_button", visibility);
        }

        /**
         * Sets the visibility for a given preference key.
         *
         * @param key        The key of the preference.
         * @param visibility The visibility flag.
         */
        private void setPreferenceVisibility(String key, Boolean visibility) {
            Preference preference = findPreference(key);
            if (preference != null) {
                preference.setVisible(visibility);
            }
        }

        /**
         * Sets up a button preference that performs an action when clicked.
         *
         * @param buttonKey    The key for the button preference.
         * @param textResource The text resource to use for the action.
         */
        private void setupButton(String buttonKey, int textResource) {
            Preference button = findPreference(buttonKey);
            if (button != null) {
                button.setOnPreferenceClickListener(preference -> {
                    try {
                        // If the button is for whitelisting a domain, copy the text to clipboard
                        if ("whitelist_domain_1_button".equals(buttonKey)
                                || "whitelist_domain_2_button".equals(buttonKey)) {
                            ClipboardManager clipboardManager = (ClipboardManager) requireContext()
                                    .getSystemService(Context.CLIPBOARD_SERVICE);
                            CharSequence copiedText = getString(textResource);
                            ClipData copiedData = ClipData.newPlainText("text", copiedText);
                            clipboardManager.setPrimaryClip(copiedData);
                            Toast.makeText(getContext(), R.string.text_copied, Toast.LENGTH_SHORT).show();
                        } else {
                            ExternalLinkHandler.openExternalLink(requireContext(),
                                    Uri.parse(getString(textResource)));
                        }
                    } catch (Exception e) {
                        SentryManager.captureException(e);
                    }
                    return true;
                });
            }
        }

        /**
         * Sets up a button preference that starts a new activity based on its key.
         *
         * @param buttonKey The key for the button preference.
         */
        private void setupButtonForIntent(String buttonKey) {
            Preference button = findPreference(buttonKey);
            if (button != null) {
                button.setOnPreferenceClickListener(preference -> {
                    try {
                        // Launch the appropriate activity based on the button key
                        if ("author_button".equals(buttonKey)) {
                            requireActivity().startActivity(
                                    new Intent(requireActivity(), AuthorActivity.class));
                        } else if ("permission_button".equals(buttonKey)) {
                            requireActivity().startActivity(
                                    new Intent(requireActivity(), PermissionActivity.class));
                        }
                    } catch (SecurityException e) {
                        Toast.makeText(getContext(), R.string.activity_open_security_error,
                                Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        SentryManager.captureException(e);
                    }
                    return true;
                });
            }
        }

        /**
         * Sets up a listener to handle changes to the dark mode preference.
         *
         * @param setting The dark mode list preference.
         */
        private void setupDarkModeChangeListener(ListPreference setting) {
            setting.setOnPreferenceChangeListener((preference, newValue) -> {
                try {
                    // Log and save the new dark mode setting
                    SentryManager.captureMessage("Dark mode set to " + newValue + ".");
                    SharedPreferencesManager.putString("dark_mode", newValue.toString());
                    // Apply the dark mode change immediately
                    applyDarkMode(newValue.toString());
                } catch (Exception e) {
                    SentryManager.captureException(e);
                }
                return true;
            });
        }

        /**
         * Sets up a listener to handle changes to the app lock preference.
         * Requires biometric authentication to disable app lock for security.
         *
         * @param setting The app lock switch preference.
         */
        private void setupAppLockChangeListener(SwitchPreference setting) {
            setting.setOnPreferenceChangeListener((preference, newValue) -> {
                try {
                    boolean newValueBoolean = (Boolean) newValue;
                    boolean currentValue = SharedPreferencesManager.getBoolean("app_lock_enable", false);

                    // Require biometric authentication to enable or disable app lock
                    if (currentValue != newValueBoolean) {
                        final BiometricLock biometricLock = new BiometricLock((AppCompatActivity) requireContext());
                        if (biometricLock.canAuthenticate()) {
                            String promptTitle = newValueBoolean
                                    ? getString(R.string.enable_app_lock_title)
                                    : getString(R.string.disable_app_lock_title);
                            String promptSubtitle = newValueBoolean
                                    ? getString(R.string.enable_app_lock_subtitle)
                                    : getString(R.string.disable_app_lock_subtitle);
                            biometricLock.showPrompt(
                                    promptTitle,
                                    promptSubtitle,
                                    getString(R.string.app_lock_confirm_action),
                                    new BiometricLock.BiometricLockCallback() {
                                        @Override
                                        public void onAuthenticationSucceeded() {
                                            SharedPreferencesManager.putBoolean("app_lock_enable", newValueBoolean);
                                            setting.setChecked(newValueBoolean);
                                            SentryManager.captureMessage(
                                                    "App lock set to " + newValueBoolean + " after biometric authentication.");
                                        }

                                        @Override
                                        public void onAuthenticationError(int errorCode, String error) {
                                            setting.setChecked(currentValue);
                                            SentryManager.captureMessage(
                                                    "App lock change failed - authentication error: " + error);
                                        }

                                        @Override
                                        public void onAuthenticationFailed() {
                                            setting.setChecked(currentValue);
                                            SentryManager.captureMessage(
                                                    "App lock change failed - authentication failed");
                                        }
                                    });
                        } else {
                            setting.setChecked(currentValue);
                            SentryManager.captureMessage(
                                    "Cannot change app lock - biometric authentication not available");
                        }
                        return false;
                    }
                } catch (Exception e) {
                    SentryManager.captureException(e);
                }
                return true;
            });
        }

        /**
         * Sets up a listener to handle changes to the Sentry preference.
         *
         * @param switchPreference The Sentry switch preference.
         */
        private void setupSentryChangeListener(SwitchPreference switchPreference) {
            switchPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                try {
                    boolean isEnabled = (boolean) newValue;
                    SentryManager.captureMessage("Sentry set to " + isEnabled + ".");
                    SharedPreferencesManager.putBoolean("sentry_enable", isEnabled);
                    // Start or stop reporting right away instead of on next launch,
                    // so opting out stops crash reports immediately.
                    SentryInitializer.applyPreference(requireContext(), isEnabled);
                    setInitialSentryVisibility(isEnabled);
                } catch (Exception e) {
                    SentryManager.captureException(e);
                }
                return true;
            });
        }
    }
}
