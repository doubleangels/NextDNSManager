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
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

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
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    public VisualIndicator(Context context) {
        this.sentryManager = new SentryManager(context);
        this.httpClient = new OkHttpClient();
    }

    public void initiateVisualIndicator(Context context, LifecycleOwner lifecycleOwner, AppCompatActivity activity) {
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest networkRequest = new NetworkRequest.Builder().build();
        Network network = connectivityManager.getActiveNetwork();
        if (network == null) {
            return;
        }
        LinkProperties linkProperties = connectivityManager.getLinkProperties(network);
        updateVisualIndicator(linkProperties, activity, context);
        connectivityManager.registerNetworkCallback(networkRequest, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onLinkPropertiesChanged(@NonNull Network network, @NonNull LinkProperties linkProperties) {
                super.onLinkPropertiesChanged(network, linkProperties);
                if (linkProperties != null) {
                    updateVisualIndicator(linkProperties, activity, context);
                }
            }
        });
        lifecycleOwner.getLifecycle().addObserver(new LifecycleObserver() {
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            void onDestroy() {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            }
        });
    }

    public void updateVisualIndicator(@Nullable LinkProperties linkProperties, AppCompatActivity activity, Context context) {
        try {
            if (linkProperties == null) {
                setConnectionStatus(activity.findViewById(R.id.connectionStatus), R.drawable.failure, R.color.red, context);
                checkInheritedDNS(context, activity);
                return;
            }
            ImageView connectionStatus = activity.findViewById(R.id.connectionStatus);
            String privateDnsServerName = linkProperties.getPrivateDnsServerName();
            int statusDrawable = linkProperties.isPrivateDnsActive()
                    ? (R.drawable.success)
                    : R.drawable.failure;
            int statusColor = linkProperties.isPrivateDnsActive()
                    ? (privateDnsServerName != null && privateDnsServerName.contains("nextdns")
                    ? R.color.green : R.color.yellow)
                    : R.color.red;
            setConnectionStatus(connectionStatus, statusDrawable, statusColor, context);
            checkInheritedDNS(context, activity);
        } catch (Exception e) {
            sentryManager.captureException(e);
        }
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
                try {
                    if (!response.isSuccessful()) {
                        sentryManager.captureMessage("Response was not successful.");
                        response.close();
                        return;
                    }
                    assert response.body() != null;
                    JsonObject testResponse = JsonParser.parseString(response.body().string().trim()).getAsJsonObject();
                    String nextDnsStatusKey = context.getString(R.string.nextdns_status);
                    String nextDnsProtocolKey = context.getString(R.string.nextdns_protocol);
                    String usingNextDnsStatusValue = context.getString(R.string.using_nextdns_status);
                    String[] secureProtocols = context.getResources().getStringArray(R.array.secure_protocols);
                    String nextDNSStatus = testResponse.getAsJsonPrimitive(nextDnsStatusKey).getAsString();
                    if (!usingNextDnsStatusValue.equalsIgnoreCase(nextDNSStatus)) {
                        response.close();
                        return;
                    }
                    String nextdnsProtocol = testResponse.getAsJsonPrimitive(nextDnsProtocolKey).getAsString();
                    boolean isSecure = Arrays.asList(secureProtocols).contains(nextdnsProtocol);
                    ImageView connectionStatus = activity.findViewById(R.id.connectionStatus);
                    if (connectionStatus != null) {
                        connectionStatus.setImageResource(isSecure ? R.drawable.success : R.drawable.failure);
                        connectionStatus.setColorFilter(ContextCompat.getColor(context, isSecure ? R.color.green : R.color.orange));
                    }
                    response.close();
                } catch (Exception e) {
                    catchNetworkErrors(e);
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                catchNetworkErrors(e);
            }
        });
    }

    private void setConnectionStatus(ImageView connectionStatus, int drawableResId, int colorResId, Context context) {
        connectionStatus.setImageResource(drawableResId);
        connectionStatus.setColorFilter(ContextCompat.getColor(context, colorResId));
    }

    private void catchNetworkErrors(@NonNull Exception e) {
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

    public void cleanup() {
        if (networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
            networkCallback = null;
        }
    }
}