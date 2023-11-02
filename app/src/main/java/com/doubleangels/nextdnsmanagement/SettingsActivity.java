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
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import java.util.Objects;

import io.sentry.ITransaction;
import io.sentry.Sentry;

public class SettingsActivity extends AppCompatActivity {
    public DarkModeHandler darkModeHandler = new DarkModeHandler();
    public Boolean darkNavigation;

    public static final String DARK_NAVIGATION = "dark_navigation";
    public static final String OVERRIDE_DARK_MODE = "override_dark_mode";
    public static final String MANUAL_DARK_MODE = "manual_dark_mode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ITransaction settingsCreateTransaction = Sentry.startTransaction("settings_onCreate()", "SettingsActivity");
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_settings);

            initializeViews(savedInstanceState);
            setActionBar();
            setVisualIndicator();
        } catch (Exception e) {
            Sentry.captureException(e);
        } finally {
            settingsCreateTransaction.finish();
        }
    }

    private void initializeViews(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commitNow();
        }
    }

    private void setActionBar() {
        androidx.appcompat.app.ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        darkNavigation = sharedPreferences.getBoolean(DARK_NAVIGATION, false);
        setWindowAndToolbar(darkNavigation);
    }

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
    }

    private void setVisualIndicator() {
        VisualIndicator visualIndicator = new VisualIndicator();
        visualIndicator.initiateVisualIndicator(this, getApplicationContext());
    }

    @Override
    protected void onResume() {
        super.onResume();
        darkModeHandler.handleDarkMode(this);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            setupButton("whitelist_domain_1_button", R.string.whitelist_domain_1);
            setupButton("whitelist_domain_2_button", R.string.whitelist_domain_2);
            setupButton("privacy_policy_button", R.string.privacy_policy_url);
            setupButton("author_button", R.string.author_url);
            setupButton("github_button", R.string.github_url);

            String versionName = BuildConfig.VERSION_NAME;
            Preference versionPreference = findPreference("version");
            if (versionPreference != null) {
                versionPreference.setSummary(versionName);
            }
        }

        private void setupButton(String buttonKey, int textResource) {
            Preference button = findPreference(buttonKey);
            if (button != null) {
                button.setOnPreferenceClickListener(preference -> {
                    ClipboardManager clipboardManager = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    CharSequence copiedText = getString(textResource);
                    ClipData copiedData = ClipData.newPlainText("text", copiedText);
                    clipboardManager.setPrimaryClip(copiedData);

                    Toast.makeText(getContext(), "Text copied!", Toast.LENGTH_SHORT).show();
                    if (buttonKey.equals("privacy_policy_button") || buttonKey.equals("author_button") || buttonKey.equals("github_button")) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(textResource)));
                        startActivity(intent);
                    }
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
