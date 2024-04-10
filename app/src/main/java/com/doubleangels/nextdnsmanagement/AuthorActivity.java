package com.doubleangels.nextdnsmanagement;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.LifecycleOwner;

import com.doubleangels.nextdnsmanagement.protocoltest.VisualIndicator;
import com.doubleangels.nextdnsmanagement.sentry.SentryInitializer;
import com.doubleangels.nextdnsmanagement.sentry.SentryManager;

import java.util.Locale;

public class AuthorActivity extends AppCompatActivity {

    public SentryManager sentryManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_author);
        sentryManager = new SentryManager(this);
        SharedPreferences sharedPreferences = this.getSharedPreferences("preferences", Context.MODE_PRIVATE);
        try {
            if (sentryManager.isSentryEnabled()) {
                SentryInitializer.initialize(this);
            }
            setupToolbar();
            setupLanguage();
            setupDarkMode(sharedPreferences);
            setupVisualIndicator(sentryManager, this);
            setupPersonalLinks(sentryManager);
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }
        ImageView imageView = findViewById(R.id.connectionStatus);
        imageView.setOnClickListener(v -> startActivity(new Intent(this, StatusActivity.class)));
    }

    /** @noinspection deprecation*/
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

    private void setupVisualIndicator(SentryManager sentryManager, LifecycleOwner lifecycleOwner) {
        try {
            new VisualIndicator(this).initiateVisualIndicator(this, lifecycleOwner, this);
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
    }

    public void setupPersonalLinks(SentryManager sentryManager) {
        try {
            ImageView githubButton = findViewById(R.id.githubImageView);
            ImageView emailButton = findViewById(R.id.emailImageView);
            ImageView websiteButton = findViewById(R.id.websiteImageView);
            githubButton.setOnClickListener(view -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_profile_url)));
                startActivity(intent);
            });

            emailButton.setOnClickListener(view -> {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                emailIntent.setData(Uri.parse("mailto:nextdns@doubleangels.com"));
                startActivity(Intent.createChooser(emailIntent, "Send Email"));
            });

            websiteButton.setOnClickListener(view -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.author_url)));
                startActivity(intent);
            });
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_back_only, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.back) {
            Intent mainIntent = new Intent(this, SettingsActivity.class);
            startActivity(mainIntent);
        }
        return super.onContextItemSelected(item);
    }
}
