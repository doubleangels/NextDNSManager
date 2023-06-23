package com.doubleangels.nextdnsmanagement;

import android.content.Context;
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
    public void updateVisualIndicator(LinkProperties linkProperties, AppCompatActivity activity, Context context) {
        ITransaction update_visual_indicator_transaction = Sentry.startTransaction("VisualIndicator_updateVisualIndicator()", "VisualIndicator");
        try {
            if (linkProperties != null) {
                if (linkProperties.isPrivateDnsActive()) {
                    if (linkProperties.getPrivateDnsServerName() != null) {
                        // If we're connected to NextDNS, show green.
                        if (linkProperties.getPrivateDnsServerName().contains("nextdns")) {
                            ImageView connectionStatus = activity.findViewById(R.id.connectionStatus);
                            connectionStatus.setImageResource(R.drawable.success);
                            connectionStatus.setColorFilter(ContextCompat.getColor(context, R.color.green));
                            Sentry.setTag("private_dns", "nextdns");
                            Sentry.addBreadcrumb("Visual indicator shows NextDNS private DNS with a secure connection (DOT/DOH)");
                        } else {
                            // If we're connected to private DNS but not NextDNS, show yellow.
                            ImageView connectionStatus = activity.findViewById(R.id.connectionStatus);
                            connectionStatus.setImageResource(R.drawable.success);
                            connectionStatus.setColorFilter(ContextCompat.getColor(context, R.color.yellow));
                            Sentry.setTag("private_dns", "private");
                            Sentry.addBreadcrumb("Visual indicator shows private DNS, but not NextDNS");
                        }
                    } else {
                        // If we're connected to private DNS but not NextDNS, show yellow.
                        ImageView connectionStatus = activity.findViewById(R.id.connectionStatus);
                        connectionStatus.setImageResource(R.drawable.success);
                        connectionStatus.setColorFilter(ContextCompat.getColor(context, R.color.yellow));
                        Sentry.setTag("private_dns", "private");
                        Sentry.addBreadcrumb("Visual indicator shows private DNS, but not NextDNS");
                    }
                } else {
                    // If we're not using private DNS, show red.
                    ImageView connectionStatus = activity.findViewById(R.id.connectionStatus);
                    connectionStatus.setImageResource(R.drawable.failure);
                    connectionStatus.setColorFilter(ContextCompat.getColor(context, R.color.red));
                    Sentry.setTag("private_dns", "insecure");
                    Sentry.addBreadcrumb("Visual indicator shows no private DNS");
                }
            } else {
                // If we have no internet connection, show gray.
                ImageView connectionStatus = activity.findViewById(R.id.connectionStatus);
                connectionStatus.setImageResource(R.drawable.failure);
                connectionStatus.setColorFilter(ContextCompat.getColor(context, R.color.gray));
                Sentry.setTag("private_dns", "no_connection");
                Sentry.addBreadcrumb("Visual indicator shows no connection");
            }
            checkInheritedDNS(context, activity);
        } catch (Exception e) {
            Sentry.captureException(e);
        } finally {
            update_visual_indicator_transaction.finish();
        }
    }

    public void initiateVisualIndicator(AppCompatActivity activity, Context context) {
        ITransaction initiate_visual_indicator_transaction = Sentry.startTransaction("VisualIndicator_initiateVisualIndicator()", "VisualIndicator");
        // Check if we're using private DNS and watch DNS type over time to change visual indicator.
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network network = connectivityManager.getActiveNetwork();
        LinkProperties linkProperties = connectivityManager.getLinkProperties(network);
        updateVisualIndicator(linkProperties, activity, context);
        connectivityManager.registerNetworkCallback(new NetworkRequest.Builder().build(), new ConnectivityManager.NetworkCallback() {
            @Override
            public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
                super.onLinkPropertiesChanged(network, linkProperties);
                Sentry.addBreadcrumb("Link properties changed");
                updateVisualIndicator(linkProperties, activity, context);
            }
        });
        initiate_visual_indicator_transaction.finish();
    }

    private void checkInheritedDNS(Context context, AppCompatActivity activity) {
        TestApi nextdnsApi = TestClient.getBaseClient(context).create(TestApi.class);
        Call<JsonObject> responseCall = nextdnsApi.getResponse();

        responseCall.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                JsonObject testResponse = response.body();
                if (testResponse != null) {
                    String nextdns_status = testResponse.get(context.getString(R.string.nextdns_status)).getAsString();
                    if (nextdns_status != null && nextdns_status.toUpperCase().equals(context.getString(R.string.using_nextdns_status))) {
                        String nextdns_protocol = testResponse.get(context.getString(R.string.nextdns_protocol)).getAsString();
                        Sentry.setTag("inherited_protocol", nextdns_protocol);
                        ImageView connectionStatus = activity.findViewById(R.id.connectionStatus);
                        for (String s : context.getResources().getStringArray(R.array.secure_protocols)) {
                            if (nextdns_protocol.toUpperCase().equals(s)) {
                                connectionStatus.setImageResource(R.drawable.success);
                                connectionStatus.setColorFilter(ContextCompat.getColor(context, R.color.green));
                                Sentry.setTag("inherited_nextdns", "secure");
                                Sentry.addBreadcrumb("Visual indicator shows NextDNS private DNS with a secure connection (DOT/DOH)");
                                return;
                            }
                        }
                        connectionStatus.setImageResource(R.drawable.failure);
                        connectionStatus.setColorFilter(ContextCompat.getColor(context, R.color.orange));
                        Sentry.setTag("inherited_nextdns", "insecure");
                        Sentry.addBreadcrumb("Visual indicator shows NextDNS private DNS, but without a secure connection (DOT/DOH)");

                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                Sentry.captureException(t);
            }
        });
    }
}