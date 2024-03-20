package com.doubleangels.nextdnsmanagement.protocoltest;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkRequest;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.doubleangels.nextdnsmanagement.R;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;

import javax.net.ssl.SSLException;

import io.sentry.Sentry;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.http2.ConnectionShutdownException;

public class VisualIndicator {

    private final Context context;
    private final OkHttpClient httpClient;

    public VisualIndicator(Context context) {
        this.httpClient = new OkHttpClient();
        this.context = context;
    }

    public boolean isSentryEnabled() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean("sentry_enable", false);
    }

    public void initiateVisualIndicator(AppCompatActivity activity, Context context) {
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
    }

    public void updateVisualIndicator(@Nullable LinkProperties linkProperties, AppCompatActivity activity, Context context) {
        try {
            ImageView connectionStatus = activity.findViewById(R.id.connectionStatus);
            if (connectionStatus != null) {
                int drawableResId;
                int colorResId;

                if (linkProperties != null && linkProperties.isPrivateDnsActive()) {
                    String privateDnsServerName = linkProperties.getPrivateDnsServerName();
                    if (privateDnsServerName != null && privateDnsServerName.contains("nextdns")) {
                        drawableResId = R.drawable.success;
                        colorResId = R.color.green;
                    } else {
                        drawableResId = R.drawable.success;
                        colorResId = R.color.yellow;
                    }
                } else {
                    drawableResId = R.drawable.failure;
                    colorResId = R.color.red;
                }

                setConnectionStatus(connectionStatus, drawableResId, colorResId, context);
            }
        } catch (Exception e) {
            if (isSentryEnabled()) {
                Sentry.captureException(e);
            }
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
                    try (response) {
                        assert response.body() != null;
                        String jsonResponse = response.body().string().trim();
                        Gson gson = new Gson();
                        JsonElement jsonElement = gson.fromJson(jsonResponse, JsonElement.class);
                        if (jsonElement.isJsonObject()) {
                            JsonObject testResponse = jsonElement.getAsJsonObject();
                            JsonElement nextDNSStatusElement = testResponse.get(context.getString(R.string.nextdns_status));
                            if (nextDNSStatusElement != null) {
                                String nextDNSStatus = nextDNSStatusElement.getAsString();
                                if (context.getString(R.string.using_nextdns_status).equalsIgnoreCase(nextDNSStatus)) {
                                    JsonElement nextdnsProtocolElement = testResponse.get(context.getString(R.string.nextdns_protocol));
                                    if (nextdnsProtocolElement != null) {
                                        String nextdnsProtocol = nextdnsProtocolElement.getAsString();
                                        boolean isSecure = Arrays.asList(context.getResources().getStringArray(R.array.secure_protocols))
                                                .contains(nextdnsProtocol.toLowerCase());
                                        ImageView connectionStatus = activity.findViewById(R.id.connectionStatus);
                                        if (connectionStatus != null) {
                                            setConnectionStatus(connectionStatus, isSecure ? R.drawable.success : R.drawable.failure,
                                                    isSecure ? R.color.green : R.color.orange, context);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        if (isSentryEnabled()) {
                            Sentry.captureException(e);
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (e instanceof UnknownHostException ||
                        e instanceof SocketTimeoutException ||
                        e instanceof SocketException ||
                        e instanceof SSLException ||
                        e instanceof ConnectionShutdownException) {
                    Sentry.addBreadcrumb("Network exception captured: " + e);
                } else {
                    if (isSentryEnabled()) {
                        Sentry.captureException(e);
                    }
                }
            }
        });
    }

    private void setConnectionStatus(ImageView connectionStatus, int drawableResId, int colorResId, Context context) {
        connectionStatus.setImageResource(drawableResId);
        connectionStatus.setColorFilter(ContextCompat.getColor(context, colorResId));
    }
}
