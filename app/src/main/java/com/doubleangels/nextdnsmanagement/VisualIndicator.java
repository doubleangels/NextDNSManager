// Import statements for required libraries and classes.
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

// Definition of the VisualIndicator class.
public class VisualIndicator {
    // Method to update the visual indicator based on link properties.
    public void updateVisualIndicator(LinkProperties linkProperties, AppCompatActivity activity, Context context) {
        ITransaction updateVisualIndicatorTransaction = Sentry.startTransaction("VisualIndicator_updateVisualIndicator()", "VisualIndicator");
        try {
            ImageView connectionStatus = activity.findViewById(R.id.connectionStatus);
            String privateDnsServerName = linkProperties != null ? linkProperties.getPrivateDnsServerName() : null;

            if (linkProperties != null && linkProperties.isPrivateDnsActive()) {
                if (privateDnsServerName != null) {
                    if (privateDnsServerName.contains("nextdns")) {
                        connectionStatus.setImageResource(R.drawable.success);
                        connectionStatus.setColorFilter(ContextCompat.getColor(context, R.color.green));
                        Sentry.setTag("private_dns", "nextdns");
                        Sentry.addBreadcrumb("Visual indicator shows NextDNS private DNS with a secure connection (DOT/DOH)");
                    } else {
                        connectionStatus.setImageResource(R.drawable.success);
                        connectionStatus.setColorFilter(ContextCompat.getColor(context, R.color.yellow));
                        Sentry.setTag("private_dns", "private");
                        Sentry.addBreadcrumb("Visual indicator shows private DNS, but not NextDNS");
                    }
                } else {
                    connectionStatus.setImageResource(R.drawable.success);
                    connectionStatus.setColorFilter(ContextCompat.getColor(context, R.color.yellow));
                    Sentry.setTag("private_dns", "private");
                    Sentry.addBreadcrumb("Visual indicator shows private DNS, but not NextDNS");
                }
            } else {
                connectionStatus.setImageResource(R.drawable.failure);
                connectionStatus.setColorFilter(ContextCompat.getColor(context, R.color.red));
                Sentry.setTag("private_dns", "insecure");
                Sentry.addBreadcrumb("Visual indicator shows no private DNS");
            }
        } catch (Exception e) {
            Sentry.captureException(e);
        } finally {
            updateVisualIndicatorTransaction.finish();
        }
        checkInheritedDNS(context, activity);
    }

    // Method to initiate the visual indicator.
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
                Sentry.addBreadcrumb("Link properties changed");
                updateVisualIndicator(linkProperties, activity, context);
            }
        });

        initiateVisualIndicatorTransaction.finish();
    }

    // Method to check for inherited DNS settings.
    private void checkInheritedDNS(Context context, AppCompatActivity activity) {
        TestApi nextdnsApi = TestClient.getBaseClient(context).create(TestApi.class);
        Call<JsonObject> responseCall = nextdnsApi.getResponse();

        responseCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                JsonObject testResponse = response.body();
                if (testResponse != null) {
                    String nextdnsStatus = testResponse.get(context.getString(R.string.nextdns_status)).getAsString();
                    if (nextdnsStatus != null && nextdnsStatus.equalsIgnoreCase(context.getString(R.string.using_nextdns_status))) {
                        String nextdnsProtocol = testResponse.get(context.getString(R.string.nextdns_protocol)).getAsString();
                        Sentry.setTag("inherited_protocol", nextdnsProtocol);
                        ImageView connectionStatus = activity.findViewById(R.id.connectionStatus);
                        String[] secureProtocols = context.getResources().getStringArray(R.array.secure_protocols);
                        boolean isSecure = false;
                        for (String s : secureProtocols) {
                            if (nextdnsProtocol.equalsIgnoreCase(s)) {
                                isSecure = true;
                                break;
                            }
                        }
                        connectionStatus.setImageResource(isSecure ? R.drawable.success : R.drawable.failure);
                        connectionStatus.setColorFilter(ContextCompat.getColor(context, isSecure ? R.color.green : R.color.orange));
                        Sentry.setTag("inherited_nextdns", isSecure ? "secure" : "insecure");
                        Sentry.addBreadcrumb("Visual indicator shows NextDNS private DNS with " + (isSecure ? "a secure" : "an insecure") + " connection (DOT/DOH)");
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
