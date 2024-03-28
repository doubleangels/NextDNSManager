package com.doubleangels.nextdnsmanagement.protocoltest;

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

    private final SentryManager sentryManager;
    private final OkHttpClient httpClient;

    public VisualIndicator(Context context) {
        this.sentryManager = new SentryManager(context);
        this.httpClient = new OkHttpClient();
    }

    public void initiateVisualIndicator(AppCompatActivity activity, Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest networkRequest = new NetworkRequest.Builder().build();
        ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onLinkPropertiesChanged(@NonNull Network network, @NonNull LinkProperties linkProperties) {
                super.onLinkPropertiesChanged(network, linkProperties);
                updateVisualIndicator(linkProperties, activity, context);
            }
        };
        Network network = connectivityManager.getActiveNetwork();
        if (network != null) {
            LinkProperties linkProperties = connectivityManager.getLinkProperties(network);
            updateVisualIndicator(linkProperties, activity, context);
        }
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
    }

    public void updateVisualIndicator(@Nullable LinkProperties linkProperties, AppCompatActivity activity, Context context) {
        try {
            ImageView connectionStatus = activity.findViewById(R.id.connectionStatus);
            int statusDrawable;
            int statusColor;

            if (linkProperties != null && linkProperties.isPrivateDnsActive()) {
                String privateDnsServerName = linkProperties.getPrivateDnsServerName();
                if (privateDnsServerName != null && privateDnsServerName.contains("nextdns")) {
                    statusDrawable = R.drawable.success;
                    statusColor = R.color.green;
                } else {
                    statusDrawable = R.drawable.success;
                    statusColor = R.color.yellow;
                }
            } else {
                statusDrawable = R.drawable.failure;
                statusColor = R.color.red;
            }

            if (connectionStatus != null) {
                setConnectionStatus(connectionStatus, statusDrawable, statusColor, context);
            }
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
        checkInheritedDNS(context, activity);
    }

    public void checkInheritedDNS(Context context, AppCompatActivity activity) {
        Request request = new Request.Builder()
                .url("https://test.nextdns.io")
                .header("Accept", "application/json")
                .header("Cache-Control", "no-cache")
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.isSuccessful()) {
                    try {
                        assert response.body() != null;
                        JsonObject testResponse = JsonParser.parseString(response.body().string().trim()).getAsJsonObject();
                        String nextDNSStatus = testResponse.getAsJsonPrimitive(context.getString(R.string.nextdns_status)).getAsString();
                        if (context.getString(R.string.using_nextdns_status).equalsIgnoreCase(nextDNSStatus)) {
                            String nextdnsProtocol = testResponse.getAsJsonPrimitive(context.getString(R.string.nextdns_protocol)).getAsString();
                            ImageView connectionStatus = activity.findViewById(R.id.connectionStatus);
                            String[] secureProtocols = context.getResources().getStringArray(R.array.secure_protocols);
                            boolean isSecure = Arrays.asList(secureProtocols).contains(nextdnsProtocol);
                            if (connectionStatus != null) {
                                setConnectionStatus(connectionStatus, isSecure ? R.drawable.success : R.drawable.failure,
                                        isSecure ? R.color.green : R.color.orange, context);
                            }
                        }
                    } catch (Exception e) {
                        sentryManager.captureException(e);
                    }
                }
                response.close();
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
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
        });
    }

    private void setConnectionStatus(ImageView connectionStatus, int drawableResId, int colorResId, Context context) {
        connectionStatus.setImageResource(drawableResId);
        connectionStatus.setColorFilter(ContextCompat.getColor(context, colorResId));
    }
}