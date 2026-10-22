package com.doubleangels.nextdnsmanagement.protocol;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.doubleangels.nextdnsmanagement.R;
import com.doubleangels.nextdnsmanagement.network.HttpClients;
import com.doubleangels.nextdnsmanagement.sentry.SentryManager;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * A class that monitors and visually indicates the network status, specifically
 * whether NextDNS is being used securely. It listens for changes in network
 * properties and updates a UI element (ImageView) accordingly.
 */
public class VisualIndicator {

    private static final long DNS_CHECK_DEBOUNCE_MS = 300L;

    private final OkHttpClient httpClient = HttpClients.getDnsCheckClient();
    private final Handler dnsCheckHandler = new Handler(Looper.getMainLooper());

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private volatile Call activeDnsCheckCall;
    private Runnable pendingDnsCheck;

    /**
     * Shows the current status and keeps it updated until the activity is
     * destroyed.
     */
    public void initialize(AppCompatActivity activity) {
        try {
            connectivityManager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager == null) {
                SentryManager.captureMessage("ConnectivityManager is null.");
                return;
            }

            Network network = connectivityManager.getActiveNetwork();
            if (network == null) {
                SentryManager.captureMessage("No active network found.");
                update(null, activity);
            } else {
                update(connectivityManager.getLinkProperties(network), activity);
            }

            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onLinkPropertiesChanged(@NonNull Network network, @NonNull LinkProperties linkProperties) {
                    update(linkProperties, activity);
                }

                @Override
                public void onAvailable(@NonNull Network network) {
                    ConnectivityManager manager = connectivityManager;
                    if (manager != null) {
                        update(manager.getLinkProperties(network), activity);
                    }
                }

                @Override
                public void onLost(@NonNull Network network) {
                    update(null, activity);
                }
            };

            connectivityManager.registerNetworkCallback(new NetworkRequest.Builder().build(), networkCallback);
            activity.getLifecycle().addObserver(new DefaultLifecycleObserver() {
                @Override
                public void onDestroy(@NonNull LifecycleOwner owner) {
                    release();
                }
            });
        } catch (Exception e) {
            SentryManager.captureException(e);
        }
    }

    private void release() {
        try {
            if (pendingDnsCheck != null) {
                dnsCheckHandler.removeCallbacks(pendingDnsCheck);
                pendingDnsCheck = null;
            }
            Call call = activeDnsCheckCall;
            if (call != null) {
                call.cancel();
                activeDnsCheckCall = null;
            }
            if (connectivityManager != null && networkCallback != null) {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            }
        } catch (Exception e) {
            SentryManager.captureException(e);
        } finally {
            networkCallback = null;
            connectivityManager = null;
        }
    }

    private void update(@Nullable LinkProperties linkProperties, AppCompatActivity activity) {
        runOnUiThreadIfAlive(activity, () -> {
            try {
                int statusColor;
                if (linkProperties == null || !linkProperties.isPrivateDnsActive()) {
                    statusColor = R.color.red;
                } else {
                    String serverName = linkProperties.getPrivateDnsServerName();
                    statusColor = serverName != null && serverName.contains("nextdns") ? R.color.green : R.color.yellow;
                }
                int statusDrawable = statusColor == R.color.red ? R.drawable.failure : R.drawable.success;
                setConnectionStatus(activity, statusDrawable, statusColor);
                scheduleInheritedDnsCheck(activity);
            } catch (Exception e) {
                SentryManager.captureException(e);
            }
        });
    }

    /**
     * Debounces bursts of network callbacks into a single request to
     * test.nextdns.io, which reports whether NextDNS is in use even when it is
     * inherited from the router instead of configured as Private DNS.
     */
    private void scheduleInheritedDnsCheck(AppCompatActivity activity) {
        if (pendingDnsCheck != null) {
            dnsCheckHandler.removeCallbacks(pendingDnsCheck);
        }
        pendingDnsCheck = () -> performInheritedDnsCheck(activity);
        dnsCheckHandler.postDelayed(pendingDnsCheck, DNS_CHECK_DEBOUNCE_MS);
    }

    private void performInheritedDnsCheck(AppCompatActivity activity) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        Request request = new Request.Builder()
                .url("https://test.nextdns.io")
                .header("Accept", "application/json")
                .header("Cache-Control", "no-cache")
                .build();

        Call previousCall = activeDnsCheckCall;
        if (previousCall != null) {
            previousCall.cancel();
        }

        Call call = httpClient.newCall(request);
        activeDnsCheckCall = call;
        call.enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (response) {
                    ResponseBody body = response.body();
                    if (!response.isSuccessful() || body == null) {
                        SentryManager.captureMessage("Connectivity check failed with HTTP " + response.code());
                        return;
                    }

                    JSONObject testResponse = new JSONObject(body.string().trim());
                    String nextDnsStatus = testResponse.optString(activity.getString(R.string.nextdns_status));
                    if (!activity.getString(R.string.using_nextdns_status).equalsIgnoreCase(nextDnsStatus)) {
                        return;
                    }

                    String nextDnsProtocol = testResponse.optString(activity.getString(R.string.nextdns_protocol));
                    boolean isSecure = Arrays.asList(activity.getResources().getStringArray(R.array.secure_protocols))
                            .contains(nextDnsProtocol);
                    runOnUiThreadIfAlive(activity, () -> setConnectionStatus(activity,
                            isSecure ? R.drawable.success : R.drawable.failure,
                            isSecure ? R.color.green : R.color.orange));
                } catch (Exception e) {
                    SentryManager.captureException(e);
                } finally {
                    clearActiveCall(call);
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                SentryManager.captureException(e);
                clearActiveCall(call);
            }
        });
    }

    private void clearActiveCall(Call call) {
        if (activeDnsCheckCall == call) {
            activeDnsCheckCall = null;
        }
    }

    private static void setConnectionStatus(AppCompatActivity activity, int drawableResId, int colorResId) {
        ImageView connectionStatus = activity.findViewById(R.id.connectionStatus);
        if (connectionStatus == null) {
            return;
        }
        connectionStatus.setImageResource(drawableResId);
        connectionStatus.setColorFilter(ContextCompat.getColor(activity, colorResId));
    }

    private static void runOnUiThreadIfAlive(AppCompatActivity activity, Runnable action) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        activity.runOnUiThread(() -> {
            if (!activity.isFinishing() && !activity.isDestroyed()) {
                action.run();
            }
        });
    }
}
