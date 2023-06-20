package com.doubleangels.nextdnsmanagement;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkRequest;
import android.widget.ImageView;

import androidx.annotation.NonNull;
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
    public void updateVisualIndicator(LinkProperties linkProperties, Activity activity, Context context) {
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
                        } else {
                            // If we're connected to private DNS but not NextDNS, show yellow.
                            ImageView connectionStatus = activity.findViewById(R.id.connectionStatus);
                            connectionStatus.setImageResource(R.drawable.success);
                            connectionStatus.setColorFilter(ContextCompat.getColor(context, R.color.yellow));
                            Sentry.setTag("private_dns", "private");
                        }
                    } else {
                        // If we're connected to private DNS but not NextDNS, show yellow.
                        ImageView connectionStatus = activity.findViewById(R.id.connectionStatus);
                        connectionStatus.setImageResource(R.drawable.success);
                        connectionStatus.setColorFilter(ContextCompat.getColor(context, R.color.yellow));
                        Sentry.setTag("private_dns", "private");
                    }
                } else {
                    // If we're not using private DNS, show red.
                    ImageView connectionStatus = activity.findViewById(R.id.connectionStatus);
                    connectionStatus.setImageResource(R.drawable.failure);
                    connectionStatus.setColorFilter(ContextCompat.getColor(context, R.color.red));
                    Sentry.setTag("private_dns", "insecure");
                }
            } else {
                // If we have no internet connection, show gray.
                ImageView connectionStatus = activity.findViewById(R.id.connectionStatus);
                connectionStatus.setImageResource(R.drawable.failure);
                connectionStatus.setColorFilter(ContextCompat.getColor(context, R.color.gray));
                Sentry.setTag("private_dns", "no_connection");
            }
            checkInheritedDNS(context, activity);
        } catch (Exception e) {
            captureException(e);
        } finally {
            update_visual_indicator_transaction.finish();
        }
    }

    public void initiateVisualIndicator(Activity activity, Context context) {
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
                updateVisualIndicator(linkProperties, activity, context);
            }
        });
        initiate_visual_indicator_transaction.finish();
    }

    public void captureException(Throwable exception) {
        Sentry.captureException(exception);
    }

    private void checkInheritedDNS(Context c, Activity activity) {
        TestApi nextdnsApi = TestClient.getBaseClient(c).create(TestApi.class);
        Call<JsonObject> responseCall = nextdnsApi.getResponse();
        responseCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                JsonObject testResponse = response.body();
                if (testResponse != null) {
                    String nextdns_status = testResponse.get(c.getString(R.string.nextdns_status)).getAsString();
                    if (nextdns_status != null && nextdns_status.toUpperCase().equals(c.getString(R.string.using_nextdns_status))) {
                        String nextdns_protocol = testResponse.get(c.getString(R.string.nextdns_protocol)).getAsString();
                        Sentry.setTag("inherited_protocol", nextdns_protocol);
                        ImageView connectionStatus = activity.findViewById(R.id.connectionStatus);
                        for (String s : c.getResources().getStringArray(R.array.secure_protocols)) {
                            if (nextdns_protocol.toUpperCase().equals(s)) {
                                connectionStatus.setImageResource(R.drawable.success);
                                connectionStatus.setColorFilter(ContextCompat.getColor(c, R.color.green));
                                Sentry.setTag("inherited_nextdns", "secure");
                                return;
                            }
                        }
                        connectionStatus.setImageResource(R.drawable.failure);
                        connectionStatus.setColorFilter(ContextCompat.getColor(c, R.color.orange));
                        Sentry.setTag("inherited_nextdns", "insecure");
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                captureException(t);
            }
        });
    }
}
