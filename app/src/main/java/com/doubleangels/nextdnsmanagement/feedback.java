package com.doubleangels.nextdnsmanagement;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.perf.metrics.AddTrace;

import java.util.Objects;

import io.sentry.ITransaction;
import io.sentry.Sentry;
import io.sentry.UserFeedback;
import io.sentry.protocol.SentryId;

public class feedback extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ITransaction feedback_create_transaction = Sentry.startTransaction("onCreate()", "feedback");
        setContentView(R.layout.activity_feedback);

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

            // Check if we're using private DNS and watch DNS type over time to change visual indicator.
            ConnectivityManager connectivityManager = (ConnectivityManager) this.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            Network network = connectivityManager.getActiveNetwork();
            LinkProperties linkProperties = connectivityManager.getLinkProperties(network);
            updateVisualIndicator(linkProperties, getApplicationContext());
            connectivityManager.registerNetworkCallback(new NetworkRequest.Builder().build(), new ConnectivityManager.NetworkCallback() {
                @Override
                public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
                    super.onLinkPropertiesChanged(network, linkProperties);
                    updateVisualIndicator(linkProperties, getApplicationContext());
                }
            });

            // Get our exception from whatever activity sent us here.
            Bundle bundle = getIntent().getExtras();
            Throwable exception = (Throwable) bundle.getSerializable("e");

            // Get feedback comments and submit them along with the error when submit button is pressed.
            Button feedbackSubmitButton = findViewById(R.id.feedbackButton);
            feedbackSubmitButton.setOnClickListener(v -> {
                EditText feedbackTextView = findViewById(R.id.feedbackTextView);
                String feedbackString = feedbackTextView.getText().toString();
                SentryId sentryID = Sentry.captureException(exception);
                UserFeedback userFeedback = new UserFeedback(sentryID);
                userFeedback.setComments(feedbackString);
                Sentry.captureUserFeedback(userFeedback);
                FirebaseCrashlytics.getInstance().recordException(exception);
                finish();
            });
        } catch (Exception e) {
            captureException(e);
        } finally {
            feedback_create_transaction.finish();
        }
    }

    @AddTrace(name = "update_visual_indicator")
    public void updateVisualIndicator(LinkProperties linkProperties, Context context) {
        ITransaction update_visual_indicator_transaction = Sentry.startTransaction("updateVisualIndicator()", "feedback");
        try {
            if (linkProperties != null) {
                if (linkProperties.isPrivateDnsActive()) {
                    if (linkProperties.getPrivateDnsServerName() != null) {
                        // If we're connected to NextDNS, show green.
                        if (linkProperties.getPrivateDnsServerName().contains("nextdns")) {
                            ImageView connectionStatus = findViewById(R.id.connectionStatus);
                            connectionStatus.setImageResource(R.drawable.success);
                            connectionStatus.setColorFilter(ContextCompat.getColor(context, R.color.green));
                            Sentry.setTag("private_dns", "nextdns");
                        } else {
                            // If we're connected to private DNS but not NextDNS, show yellow.
                            ImageView connectionStatus = findViewById(R.id.connectionStatus);
                            connectionStatus.setImageResource(R.drawable.success);
                            connectionStatus.setColorFilter(ContextCompat.getColor(context, R.color.yellow));
                            Sentry.setTag("private_dns", "private");
                        }
                    } else {
                        // If we're connected to private DNS but not NextDNS, show yellow.
                        ImageView connectionStatus = findViewById(R.id.connectionStatus);
                        connectionStatus.setImageResource(R.drawable.success);
                        connectionStatus.setColorFilter(ContextCompat.getColor(context, R.color.yellow));
                        Sentry.setTag("private_dns", "private");
                    }
                } else {
                    // If we're not using private DNS, show red.
                    ImageView connectionStatus = findViewById(R.id.connectionStatus);
                    connectionStatus.setImageResource(R.drawable.failure);
                    connectionStatus.setColorFilter(ContextCompat.getColor(context, R.color.red));
                    Sentry.setTag("private_dns", "insecure");
                }
            } else {
                // If we have no internet connection, show gray.
                ImageView connectionStatus = findViewById(R.id.connectionStatus);
                connectionStatus.setImageResource(R.drawable.failure);
                connectionStatus.setColorFilter(ContextCompat.getColor(context, R.color.gray));
                Sentry.setTag("private_dns", "no_connection");
            }
        } catch (Exception e) {
            captureException(e);
        } finally {
            update_visual_indicator_transaction.finish();
        }
    }

    public void captureException(Exception exception) {
        Sentry.captureException(exception);
        FirebaseCrashlytics.getInstance().recordException(exception);
    }
}