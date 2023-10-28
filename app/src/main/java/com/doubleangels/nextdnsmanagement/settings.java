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
import android.widget.ImageView;
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

public class settings extends AppCompatActivity {
    public DarkModeHandler darkModeHandler = new DarkModeHandler();
    public Boolean darkNavigation;
    public static final String DARK_NAVIGATION = "dark_navigation";
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

            // Get shared preferences.
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

            // Set up our window, status bar, and toolbar.
            darkNavigation = sharedPreferences.getBoolean(settings.DARK_NAVIGATION, false);
            if (darkNavigation) {
                Window window = this.getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.setStatusBarColor(ContextCompat.getColor(this, R.color.darkgray));
                window.setNavigationBarColor(ContextCompat.getColor(this, R.color.darkgray));
                Toolbar toolbar = findViewById(R.id.toolbar);
                setSupportActionBar(toolbar);
                Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
                toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.darkgray));
                Sentry.setTag("dark_navigation", "true");
                Sentry.addBreadcrumb("Turned on dark navigation");
            } else {
                Window window = this.getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                Toolbar toolbar = findViewById(R.id.toolbar);
                setSupportActionBar(toolbar);
                Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
                toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.blue));
                Sentry.setTag("dark_navigation", "false");
                Sentry.addBreadcrumb("Turned off dark navigation");
            }

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
            assert whitelist1Button != null;
            whitelist1Button.setOnPreferenceClickListener(preference -> {
                ClipboardManager clipboardManager = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                CharSequence copiedText = getString(R.string.whitelist_domain_1);
                ClipData copiedData = ClipData.newPlainText("text", copiedText);
                clipboardManager.setPrimaryClip(copiedData);
                Sentry.addBreadcrumb("Whitelist domain 1 copied to clipboard");
                Toast.makeText(getContext(), "Text copied!",
                        Toast.LENGTH_SHORT).show();
                return true;
            });
            Preference whitelist2Button = getPreferenceManager().findPreference("whitelist_domain_2_button");
            assert whitelist2Button != null;
            whitelist2Button.setOnPreferenceClickListener(preference -> {
                ClipboardManager clipboardManager = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                CharSequence copiedText = getString(R.string.whitelist_domain_2);
                ClipData copiedData = ClipData.newPlainText("text", copiedText);
                clipboardManager.setPrimaryClip(copiedData);
                Sentry.addBreadcrumb("Whitelist domain 2 copied to clipboard");
                Toast.makeText(getContext(), "Text copied!",
                        Toast.LENGTH_SHORT).show();
                return true;
            });
            Preference privacyButton = getPreferenceManager().findPreference("privacy_policy_button");
            assert privacyButton != null;
            privacyButton.setOnPreferenceClickListener(preference -> {
                Intent githubIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.privacy_policy_url)));
                Sentry.addBreadcrumb("Visited privacy policy");
                startActivity(githubIntent);
                return true;
            });
            Preference authorButton = getPreferenceManager().findPreference("author_button");
            assert authorButton != null;
            authorButton.setOnPreferenceClickListener(preference -> {
                Intent authorIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.author_url)));
                Sentry.addBreadcrumb("Visited personal webpage");
                startActivity(authorIntent);
                return true;
            });
            Preference githubButton = getPreferenceManager().findPreference("github_button");
            assert githubButton != null;
            githubButton.setOnPreferenceClickListener(preference -> {
                Intent githubIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_url)));
                Sentry.addBreadcrumb("Visited Github repository");
                startActivity(githubIntent);
                return true;
            });

            //Update version and build numbers.
            String versionName = BuildConfig.VERSION_NAME;
            Preference versionPreference = findPreference( "version" );
            assert versionPreference != null;
            versionPreference.setSummary(versionName);
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