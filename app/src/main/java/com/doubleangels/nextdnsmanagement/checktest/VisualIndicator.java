package com.doubleangels.nextdnsmanagement.checktest;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkRequest;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.doubleangels.nextdnsmanagement.R;
import com.google.gson.JsonObject;

import io.sentry.ITransaction;
import io.sentry.Sentry;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VisualIndicator {
    // Initialize the visual indicator
    public void initiateVisualIndicator(AppCompatActivity activity, Context context) {
        // Start a Sentry transaction for monitoring
        ITransaction initiateVisualIndicatorTransaction = Sentry.startTransaction("VisualIndicator_initiateVisualIndicator()", "VisualIndicator");

        // Get connectivity information
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network network = connectivityManager.getActiveNetwork();
        LinkProperties linkProperties = connectivityManager.getLinkProperties(network);

        // Update the visual indicator
        updateVisualIndicator(linkProperties, activity, context);

        // Register a network callback for changes
        connectivityManager.registerNetworkCallback(new NetworkRequest.Builder().build(), new ConnectivityManager.NetworkCallback() {
            @Override
            public void onLinkPropertiesChanged(@NonNull Network network, @NonNull LinkProperties linkProperties) {
                super.onLinkPropertiesChanged(network, linkProperties);
                updateVisualIndicator(linkProperties, activity, context);
            }
        });

        // Finish the Sentry transaction
        initiateVisualIndicatorTransaction.finish();
    }

    // Update the visual indicator based on LinkProperties
    public void updateVisualIndicator(@Nullable LinkProperties linkProperties, AppCompatActivity activity, Context context) {
        // Start a Sentry transaction for monitoring
        ITransaction updateVisualIndicatorTransaction = Sentry.startTransaction("VisualIndicator_updateVisualIndicator()", "VisualIndicator");
        try {
            ImageView connectionStatus = activity.findViewById(R.id.connectionStatus);
            if (connectionStatus != null) {
                if (linkProperties != null && linkProperties.isPrivateDnsActive()) {
                    String privateDnsServerName = linkProperties.getPrivateDnsServerName();
                    if (privateDnsServerName != null) {
                        if (privateDnsServerName.contains("nextdns")) {
                            setConnectionStatus(connectionStatus, R.drawable.success, R.color.green, context);
                        } else {
                            setConnectionStatus(connectionStatus, R.drawable.success, R.color.yellow, context);
                        }
                    } else {
                        setConnectionStatus(connectionStatus, R.drawable.success, R.color.yellow, context);
                    }
                } else {
                    setConnectionStatus(connectionStatus, R.drawable.failure, R.color.red, context);
                }
            }
        } catch (Exception e) {
            // Capture any exceptions with Sentry
            Sentry.captureException(e);
        } finally {
            // Finish the Sentry transaction
            updateVisualIndicatorTransaction.finish();
        }
        // Check for inherited DNS settings
        checkInheritedDNS(context, activity);
    }

    // Check for inherited DNS settings
    private void checkInheritedDNS(Context context, AppCompatActivity activity) {
        // Create an API client and make a network request
        TestApi nextdnsApi = TestClient.getBaseClient(context).create(TestApi.class);
        Call<JsonObject> responseCall = nextdnsApi.getResponse();
        responseCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject testResponse = response.body();
                    String nextdnsStatus = testResponse.get(context.getString(R.string.nextdns_status)).getAsString();

                    // Check if the DNS status indicates the use of NextDNS
                    if (context.getString(R.string.using_nextdns_status).equalsIgnoreCase(nextdnsStatus)) {
                        String nextdnsProtocol = testResponse.get(context.getString(R.string.nextdns_protocol)).getAsString();
                        ImageView connectionStatus = activity.findViewById(R.id.connectionStatus);
                        String[] secureProtocols = context.getResources().getStringArray(R.array.secure_protocols);
                        boolean isSecure = false;

                        // Check if the DNS protocol is considered secure
                        for (String s : secureProtocols) {
                            if (nextdnsProtocol.equalsIgnoreCase(s)) {
                                isSecure = true;
                                break;
                            }
                        }

                        if (connectionStatus != null) {
                            // Set the connection status in the ImageView
                            setConnectionStatus(connectionStatus, isSecure ? R.drawable.success : R.drawable.failure,
                                    isSecure ? R.color.green : R.color.orange, context);
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                // Capture any network request failures with Sentry
                Sentry.captureException(t);
            }
        });
    }

    // Set the connection status in the ImageView
    private void setConnectionStatus(ImageView connectionStatus, int drawableResId, int colorResId, Context context) {
        connectionStatus.setImageResource(drawableResId);
        connectionStatus.setColorFilter(ContextCompat.getColor(context, colorResId));
    }
}
