package com.doubleangels.nextdnsmanagement.protocol;

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
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.doubleangels.nextdnsmanagement.R;
import com.doubleangels.nextdnsmanagement.sentry.SentryManager;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;

import javax.net.ssl.SSLException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.http2.ConnectionShutdownException;

public class VisualIndicator {

    // SentryManager instance for error tracking
    private final SentryManager sentryManager;
    // OkHttpClient instance for making HTTP requests
    private final OkHttpClient httpClient;
    // ConnectivityManager instance for network-related operations
    private ConnectivityManager connectivityManager;
    // NetworkCallback instance for monitoring network changes
    private ConnectivityManager.NetworkCallback networkCallback;
    // Constructor to initialize VisualIndicator with context
    public VisualIndicator(Context context) {
        this.sentryManager = new SentryManager(context);
        this.httpClient = new OkHttpClient();
    }

    // Method to initialize VisualIndicator
    public void initialize(Context context, LifecycleOwner lifecycleOwner, AppCompatActivity activity) {
        // Get ConnectivityManager service
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        // Build a network request
        NetworkRequest networkRequest = new NetworkRequest.Builder().build();
        // Get active network
        Network network = connectivityManager.getActiveNetwork();
        // If network is null, return
        if (network == null) {
            return;
        }
        // Get link properties for the active network and update visual indicator
        LinkProperties linkProperties = connectivityManager.getLinkProperties(network);
        update(linkProperties, activity, context);
        // Set up network callback to monitor network changes
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onLinkPropertiesChanged(@NonNull Network network, @NonNull LinkProperties linkProperties) {
                super.onLinkPropertiesChanged(network, linkProperties);
                // Update visual indicator on link properties change
                update(linkProperties, activity, context);
            }
        };
        // Register network callback
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
        // Add observer to lifecycle to unregister network callback on destroy
        lifecycleOwner.getLifecycle().addObserver(new NetworkConnectivityObserver());
    }

    // Custom LifecycleObserver class to unregister network callback on destroy
    private class NetworkConnectivityObserver implements DefaultLifecycleObserver {
        @Override
        public void onDestroy(@NonNull LifecycleOwner owner) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
    }

    // Method to update visual indicator based on link properties
    public void update(@Nullable LinkProperties linkProperties, AppCompatActivity activity, Context context) {
        try {
            // If link properties are null, set connection status to failure
            if (linkProperties == null) {
                setConnectionStatus(activity.findViewById(R.id.connectionStatus), R.drawable.failure, R.color.red, context);
                checkInheritedDNS(context, activity);
                return;
            }
            // Get connection status views
            ImageView connectionStatus = activity.findViewById(R.id.connectionStatus);
            // Determine status drawable and color based on private DNS status
            int statusDrawable = linkProperties.isPrivateDnsActive()
                    ? (R.drawable.success)
                    : R.drawable.failure;
            int statusColor = linkProperties.isPrivateDnsActive()
                    ? (linkProperties.getPrivateDnsServerName() != null && linkProperties.getPrivateDnsServerName().contains("nextdns")
                    ? R.color.green : R.color.yellow)
                    : R.color.red;
            // Set connection status based on drawable and color
            setConnectionStatus(connectionStatus, statusDrawable, statusColor, context);
            // Check inherited DNS and update visual indicator
            checkInheritedDNS(context, activity);
        } catch (Exception e) {
            // Catch and log exceptions
            sentryManager.captureException(e);
        }
    }

    // Method to check inherited DNS
    public void checkInheritedDNS(Context context, AppCompatActivity activity) {
        // Build HTTP request to test NextDNS connection
        Request request = new Request.Builder()
                .url("https://test.nextdns.io")
                .header("Accept", "application/json")
                .header("Cache-Control", "no-cache")
                .build();

        // Execute asynchronous HTTP request
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    // If response is not successful, capture message and return
                    if (!response.isSuccessful()) {
                        sentryManager.captureMessage("Response was not successful.");
                        response.close();
                        return;
                    }
                    // Parse JSON response
                    assert response.body() != null;
                    JsonObject testResponse = JsonParser.parseString(response.body().string().trim()).getAsJsonObject();
                    // Get keys and values for NextDNS status and protocol
                    String nextDnsStatusKey = context.getString(R.string.nextdns_status);
                    String nextDnsProtocolKey = context.getString(R.string.nextdns_protocol);
                    String usingNextDnsStatusValue = context.getString(R.string.using_nextdns_status);
                    String[] secureProtocols = context.getResources().getStringArray(R.array.secure_protocols);
                    String nextDNSStatus = testResponse.getAsJsonPrimitive(nextDnsStatusKey).getAsString();
                    // If not using NextDNS, return
                    if (!usingNextDnsStatusValue.equalsIgnoreCase(nextDNSStatus)) {
                        response.close();
                        return;
                    }
                    // Check if NextDNS protocol is secure
                    String nextdnsProtocol = testResponse.getAsJsonPrimitive(nextDnsProtocolKey).getAsString();
                    boolean isSecure = Arrays.asList(secureProtocols).contains(nextdnsProtocol);
                    // Update connection status based on protocol
                    ImageView connectionStatus = activity.findViewById(R.id.connectionStatus);
                    if (connectionStatus != null) {
                        connectionStatus.setImageResource(isSecure ? R.drawable.success : R.drawable.failure);
                        connectionStatus.setColorFilter(ContextCompat.getColor(context, isSecure ? R.color.green : R.color.orange));
                    }
                    response.close();
                } catch (Exception e) {
                    // Catch network errors
                    catchNetworkErrors(e);
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // Catch network errors
                catchNetworkErrors(e);
            }
        });
    }

    // Method to set connection status drawable and color
    private void setConnectionStatus(ImageView connectionStatus, int drawableResId, int colorResId, Context context) {
        connectionStatus.setImageResource(drawableResId);
        connectionStatus.setColorFilter(ContextCompat.getColor(context, colorResId));
    }

    // Method to catch and handle network errors
    private void catchNetworkErrors(@NonNull Exception e) {
        // Check type of network exception and capture message or exception
        if (e instanceof UnknownHostException ||
                e instanceof SocketTimeoutException ||
                e instanceof SocketException ||
                e instanceof SSLException ||
                e instanceof ConnectionShutdownException) {
            sentryManager.captureMessage("Network exception captured: " + e);
        } else {
            sentryManager.captureException(e);
        }
    }
}
