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

import javax.net.ssl.SSLException;

import io.sentry.ITransaction;
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
            if (isSentryEnabled()) {
                Sentry.captureException(e);
            }
        } finally {
            updateVisualIndicatorTransaction.finish();
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
                        String jsonResponse = response.body().string().trim();
                        Gson gson = new Gson();
                        JsonElement jsonElement = gson.fromJson(jsonResponse, JsonElement.class);
                        if (jsonElement.isJsonObject()) {
                            JsonObject testResponse = jsonElement.getAsJsonObject();
                            String nextDNSStatus = testResponse.get(context.getString(R.string.nextdns_status)).getAsString();
                            if (context.getString(R.string.using_nextdns_status).equalsIgnoreCase(nextDNSStatus)) {
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
                    } catch (Exception e) {
                        if (isSentryEnabled()) {
                            Sentry.captureException(e);
                        }
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
