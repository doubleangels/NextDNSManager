package com.doubleangels.nextdnsmanagement;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.perf.metrics.AddTrace;

import java.util.Objects;

import io.sentry.ITransaction;
import io.sentry.Sentry;

public class troubleshooting extends AppCompatActivity {

    public ExceptionHandler exceptionHandler = new ExceptionHandler();

    @Override
    @AddTrace(name = "troubleshooting_create")
    protected void onCreate(Bundle savedInstanceState) {
        ITransaction troubleshooting_create_transaction = Sentry.startTransaction("onCreate()", "troubleshooting");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_troubleshooting);

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
                Bundle bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "help_icon");
                Intent helpIntent = new Intent(v.getContext(), help.class);
                startActivity(helpIntent);
            });

            // Set up our various buttons.
            Button clearCacheButton = findViewById(R.id.clearCacheButton);
            clearCacheButton.setOnClickListener(v -> {
                Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Sentry.setTag("cleared_cache", "true");
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            });
            ImageView troubleshootingGithub = findViewById(R.id.helpGithubImageView);
            troubleshootingGithub.setOnClickListener(v -> {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.setData(Uri.parse(getString(R.string.github_url)));
                startActivity(intent);
            });
        } catch (Exception e) {
            exceptionHandler.captureExceptionAndFeedback(e, this);
        } finally {
            troubleshooting_create_transaction.finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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