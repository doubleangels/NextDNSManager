package com.doubleangels.nextdnsmanagement;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import java.util.Objects;

import io.sentry.ITransaction;
import io.sentry.Sentry;

public class about extends AppCompatActivity {

    public DarkModeHandler darkModeHandler = new DarkModeHandler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ITransaction preferences_create_transaction = Sentry.startTransaction("onCreate()", "whitelist");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        try {
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

            // Set up our various buttons.
            TextView privacyPolicyTextView = findViewById(R.id.privacyPolicyTextView);
            privacyPolicyTextView.setOnClickListener(v -> {
                Intent githubIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.privacy_policy_url)));
                Sentry.addBreadcrumb("Visited privacy policy");
                startActivity(githubIntent);
            });
            TextView authorGithubTextView = findViewById(R.id.authorGithubTextView);
            authorGithubTextView.setOnClickListener(v -> {
                Intent githubIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.author_github_url)));
                Sentry.addBreadcrumb("Visited Github page");
                startActivity(githubIntent);
            });
            TextView authorWebsiteTextView = findViewById(R.id.authorWebsiteTextView);
            authorWebsiteTextView.setOnClickListener(v -> {
                Intent githubIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.author_website_url)));
                Sentry.addBreadcrumb("Visited personal website");
                startActivity(githubIntent);
            });

            // Show the version and build numbers.
            TextView versionNumber = findViewById(R.id.versionNumberTextView);
            String versionText = BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")";
            versionNumber.setText(versionText);
        } catch (Exception e) {
            Sentry.captureException(e);
        } finally {
            preferences_create_transaction.finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        darkModeHandler.handleDarkMode(this);
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