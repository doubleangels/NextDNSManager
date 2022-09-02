package com.doubleangels.nextdnsmanagement;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkRequest;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import io.sentry.ITransaction;
import io.sentry.Sentry;

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
        } catch (Exception e) {
            captureException(e);
        } finally {
            update_visual_indicator_transaction.finish();
        }
    }

    public void initiateVisualIndicator(Activity activity, Context context) {
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
    }

    public void captureException(Exception exception) {
        Sentry.captureException(exception);
    }
}
