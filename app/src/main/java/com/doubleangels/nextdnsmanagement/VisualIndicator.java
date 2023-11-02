package com.doubleangels.nextdnsmanagement;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkRequest;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.doubleangels.nextdnsmanagement.checktest.TestApi;
import com.doubleangels.nextdnsmanagement.checktest.TestClient;
import com.google.gson.JsonObject;

import io.sentry.ITransaction;
import io.sentry.Sentry;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VisualIndicator {
    // Method to update the visual indicator based on LinkProperties and context
    public void updateVisualIndicator(LinkProperties linkProperties, AppCompatActivity activity, Context context) {
        // Start a Sentry transaction to monitor this method
        ITransaction updateVisualIndicatorTransaction = Sentry.startTransaction("VisualIndicator_updateVisualIndicator()", "VisualIndicator");
        try {
            // Find the connection status view (ImageView)
            ImageView connectionStatus = activity.findViewById(R.id.connectionStatus);
            // Get the private DNS server name from LinkProperties
            String privateDnsServerName = linkProperties != null ? linkProperties.getPrivateDnsServerName() : null;

            // Set a click listener to open the HelpActivity when the connection status is clicked
            connectionStatus.setOnClickListener(v -> {
                Intent helpIntent = new Intent(activity, HelpActivity.class);
                activity.startActivity(helpIntent);
            });

            // Check if private DNS is active
            if (linkProperties != null && linkProperties.isPrivateDnsActive()) {
                // If private DNS is active, check the private DNS server name
                if (privateDnsServerName != null) {
                    if (privateDnsServerName.contains("nextdns")) {
                        // Display a success icon and set the color to green for NextDNS
                        connectionStatus.setImageResource(R.drawable.success);
                        connectionStatus.setColorFilter(ContextCompat.getColor(context, R.color.green));
                    } else {
                        // Display a success icon and set the color to yellow for other private DNS
                        connectionStatus.setImageResource(R.drawable.success);
                        connectionStatus.setColorFilter(ContextCompat.getColor(context, R.color.yellow));
                    }
                } else {
                    // Display a success icon and set the color to yellow if private DNS server name is not available
                    connectionStatus.setImageResource(R.drawable.success);
                    connectionStatus.setColorFilter(ContextCompat.getColor(context, R.color.yellow));
                }
            } else {
                // Display a failure icon and set the color to red if private DNS is not active
                connectionStatus.setImageResource(R.drawable.failure);
                connectionStatus.setColorFilter(ContextCompat.getColor(context, R.color.red));
            }
        } catch (Exception e) {
            // Capture and report any exceptions to Sentry
            Sentry.captureException(e);
        } finally {
            // Finish the Sentry transaction
            updateVisualIndicatorTransaction.finish();
        }
        // Check inherited DNS after updating the visual indicator
        checkInheritedDNS(context, activity);
    }

    // Method to initiate the visual indicator
    public void initiateVisualIndicator(AppCompatActivity activity, Context context) {
        // Start a Sentry transaction to monitor this method
        ITransaction initiateVisualIndicatorTransaction = Sentry.startTransaction("VisualIndicator_initiateVisualIndicator()", "VisualIndicator");

        // Get the ConnectivityManager from the context
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        // Get the active network and its LinkProperties
        Network network = connectivityManager.getActiveNetwork();
        LinkProperties linkProperties = connectivityManager.getLinkProperties(network);
        // Update the visual indicator based on LinkProperties
        updateVisualIndicator(linkProperties, activity, context);

        // Register a network callback to monitor changes in LinkProperties
        connectivityManager.registerNetworkCallback(new NetworkRequest.Builder().build(), new ConnectivityManager.NetworkCallback() {
            @Override
            public void onLinkPropertiesChanged(@NonNull Network network, @NonNull LinkProperties linkProperties) {
                super.onLinkPropertiesChanged(network, linkProperties);
                // Update the visual indicator when LinkProperties change
                updateVisualIndicator(linkProperties, activity, context);
            }
        });

        // Finish the Sentry transaction
        initiateVisualIndicatorTransaction.finish();
    }

    // Method to check inherited DNS settings
    private void checkInheritedDNS(Context context, AppCompatActivity activity) {
        // Create a TestApi instance to make API calls for DNS checking
        TestApi nextdnsApi = TestClient.getBaseClient(context).create(TestApi.class);
        Call<JsonObject> responseCall = nextdnsApi.getResponse();

        // Enqueue the API call for asynchronous execution
        responseCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                JsonObject testResponse = response.body();
                if (testResponse != null) {
                    // Get the NextDNS status from the API response
                    String nextdnsStatus = testResponse.get(context.getString(R.string.nextdns_status)).getAsString();
                    if (nextdnsStatus != null && nextdnsStatus.equalsIgnoreCase(context.getString(R.string.using_nextdns_status))) {
                        // If NextDNS is used, get the protocol from the API response
                        String nextdnsProtocol = testResponse.get(context.getString(R.string.nextdns_protocol)).getAsString();
                        // Find the connection status view (ImageView)
                        ImageView connectionStatus = activity.findViewById(R.id.connectionStatus);
                        // Get an array of secure protocols from resources
                        String[] secureProtocols = context.getResources().getStringArray(R.array.secure_protocols);
                        boolean isSecure = false;
                        // Check if the NextDNS protocol is in the list of secure protocols
                        for (String s : secureProtocols) {
                            if (nextdnsProtocol.equalsIgnoreCase(s)) {
                                isSecure = true;
                                break;
                            }
                        }
                        // Set the appropriate icon and color based on whether the protocol is secure
                        connectionStatus.setImageResource(isSecure ? R.drawable.success : R.drawable.failure);
                        connectionStatus.setColorFilter(ContextCompat.getColor(context, isSecure ? R.color.green : R.color.orange));
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                // Capture and report any exceptions to Sentry in case of API call failure
                Sentry.captureException(t);
            }
        });
    }
}
