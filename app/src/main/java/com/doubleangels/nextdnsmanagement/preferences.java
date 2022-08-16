package com.doubleangels.nextdnsmanagement;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import java.util.Objects;

import io.sentry.ITransaction;
import io.sentry.Sentry;

public class preferences extends AppCompatActivity {

    public ExceptionHandler exceptionHandler = new ExceptionHandler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ITransaction preferences_create_transaction = Sentry.startTransaction("onCreate()", "preferences");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

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
            ImageView whitelistDomain1ImageView = findViewById(R.id.whitelistDomain1ImageView);
            TextView whitelistDomain1TextView = findViewById(R.id.whitelistDomain1TextView);
            whitelistDomain1ImageView.setOnClickListener(v -> copyURL(whitelistDomain1TextView));

            ImageView whitelistDomain2ImageView = findViewById(R.id.whitelistDomain2ImageView);
            TextView whitelistDomain2TextView = findViewById(R.id.whitelistDomain2TextView);
            whitelistDomain2ImageView.setOnClickListener(v -> copyURL(whitelistDomain2TextView));

            ImageView whitelistDomain3ImageView = findViewById(R.id.whitelistDomain3ImageView);
            TextView whitelistDomain3TextView = findViewById(R.id.whitelistDomain3TextView);
            whitelistDomain3ImageView.setOnClickListener(v -> copyURL(whitelistDomain3TextView));
        } catch (Exception e) {
            exceptionHandler.captureExceptionAndFeedback(e, this);
        } finally {
            preferences_create_transaction.finish();
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

    public void copyURL(TextView textView) {
        ITransaction preferences_copy_url_transaction = Sentry.startTransaction("copyURL()", "settings");
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("whitelist_url", textView.getText());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Copied!", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            exceptionHandler.captureExceptionAndFeedback(e, this);
        } finally {
            preferences_copy_url_transaction.finish();
        }
    }
}