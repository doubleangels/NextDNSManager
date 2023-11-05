package com.doubleangels.nextdnsmanagement;

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

import com.doubleangels.nextdnsmanagement.checktest.TestApi;
import com.doubleangels.nextdnsmanagement.checktest.TestClient;
import com.google.gson.JsonObject;

import io.sentry.ITransaction;
import io.sentry.Sentry;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VisualIndicator {
    // Update the visual indicator based on LinkProperties
    public void updateVisualIndicator(@Nullable LinkProperties linkProperties, AppCompatActivity activity, Context context) {
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
            Sentry.captureException(e);
        } finally {
            updateVisualIndicatorTransaction.finish();
        }
        checkInheritedDNS(context, activity);
    }

    // Initialize the visual indicator
    public void initiateVisualIndicator(AppCompatActivity activity, Context context) {
        ITransaction initiateVisualIndicatorTransaction = Sentry.startTransaction("VisualIndicator_initiateVisualIndicator()", "VisualIndicator");

        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network network = connectivityManager.getActiveNetwork();
        LinkProperties linkProperties = connectivityManager.getLinkProperties(network);
        updateVisualIndicator(linkProperties, activity, context);

        connectivityManager.registerNetworkCallback(new NetworkRequest.Builder().build(), new ConnectivityManager.NetworkCallback() {
            @Override
            public void onLinkPropertiesChanged(@NonNull Network network, @NonNull LinkProperties linkProperties) {
                super.onLinkPropertiesChanged(network, linkProperties);
                updateVisualIndicator(linkProperties, activity, context);
            }
        });

        initiateVisualIndicatorTransaction.finish();
    }

    // Check for inherited DNS settings
    private void checkInheritedDNS(Context context, AppCompatActivity activity) {
        TestApi nextdnsApi = TestClient.getBaseClient(context).create(TestApi.class);
        Call<JsonObject> responseCall = nextdnsApi.getResponse();
        responseCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (response.isSuccessful()) {
                    JsonObject testResponse = response.body();
                    if (testResponse != null) {
                        String nextdnsStatus = testResponse.get(context.getString(R.string.nextdns_status)).getAsString();
                        if (context.getString(R.string.using_nextdns_status).equalsIgnoreCase(nextdnsStatus)) {
                            String nextdnsProtocol = testResponse.get(context.getString(R.string.nextdns_protocol)).getAsString();
                            ImageView connectionStatus = activity.findViewById(R.id.connectionStatus);
                            String[] secureProtocols = context.getResources().getStringArray(R.array.secure_protocols);
                            boolean isSecure = false;
                            for (String s : secureProtocols) {
                                if (nextdnsProtocol.equalsIgnoreCase(s)) {
                                    isSecure = true;
                                    break;
                                }
                            }
                            if (connectionStatus != null) {
                                setConnectionStatus(connectionStatus, isSecure ? R.drawable.success : R.drawable.failure,
                                        isSecure ? R.color.green : R.color.orange, context);
                            }
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
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
