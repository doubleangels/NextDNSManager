package com.doubleangels.nextdnsmanagement;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Objects;

import io.sentry.ITransaction;
import io.sentry.Sentry;

public class settings extends AppCompatActivity {
    public DarkModeHandler darkModeHandler = new DarkModeHandler();
    public static final String ENABLE_SENTRY = "enable_sentry";
    public static final String OVERRIDE_DARK_MODE = "override_dark_mode";
    public static final String MANUAL_DARK_MODE = "manual_dark_mode";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ITransaction settings_create_transaction = Sentry.startTransaction("settings_onCreate()", "settings");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        try {
            // Set up our settings fragment.
            if (savedInstanceState == null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.settings, new SettingsFragment())
                        .commit();
            }
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
            }

            // Set up our window, status bar, and toolbar.
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.status_bar_background_color));
            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
            toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.toolbar_background_color));

            // Set up the visual indicator.
            VisualIndicator visualIndicator = new VisualIndicator();
            visualIndicator.initiateVisualIndicator(this, getApplicationContext());

            // Let us touch the visual indicator to open an explanation.
            ImageView statusIcon = findViewById(R.id.connectionStatus);
            statusIcon.setOnClickListener(v -> {
                Intent helpIntent = new Intent(v.getContext(), help.class);
                startActivity(helpIntent);
            });
        } catch (Exception e) {
            Sentry.captureException(e);
        } finally {
            settings_create_transaction.finish();
        }
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

            // Set up settings buttons.
            Preference whitelist1Button = getPreferenceManager().findPreference("whitelist_domain_1_button");
            whitelist1Button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    ClipboardManager clipboardManager = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    CharSequence copiedText = getString(R.string.whitelist_domain_1);
                    ClipData copiedData = ClipData.newPlainText("text", copiedText);
                    clipboardManager.setPrimaryClip(copiedData);
                    Sentry.addBreadcrumb("Whitelist domain 1 copied to clipboard");
                    Toast.makeText(getContext(), "Text copied!",
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
            Preference whitelist2Button = getPreferenceManager().findPreference("whitelist_domain_2_button");
            whitelist2Button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    ClipboardManager clipboardManager = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    CharSequence copiedText = getString(R.string.whitelist_domain_2);
                    ClipData copiedData = ClipData.newPlainText("text", copiedText);
                    clipboardManager.setPrimaryClip(copiedData);
                    Sentry.addBreadcrumb("Whitelist domain 2 copied to clipboard");
                    Toast.makeText(getContext(), "Text copied!",
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
            Preference privacyButton = getPreferenceManager().findPreference("privacy_policy_button");
            privacyButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent githubIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.privacy_policy_url)));
                    Sentry.addBreadcrumb("Visited privacy policy");
                    startActivity(githubIntent);
                    return true;
                }
            });
            Preference authorButton = getPreferenceManager().findPreference("author_button");
            authorButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent authorIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.author_url)));
                    Sentry.addBreadcrumb("Visited personal webpage");
                    startActivity(authorIntent);
                    return true;
                }
            });
            Preference githubButton = getPreferenceManager().findPreference("github_button");
            githubButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent githubIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_url)));
                    Sentry.addBreadcrumb("Visited Github repository");
                    startActivity(githubIntent);
                    return true;
                }
            });
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