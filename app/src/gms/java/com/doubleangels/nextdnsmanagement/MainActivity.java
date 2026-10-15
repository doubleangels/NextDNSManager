package com.doubleangels.nextdnsmanagement;

import static android.Manifest.permission.POST_NOTIFICATIONS;

import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.doubleangels.nextdnsmanagement.firebasemessaging.MessagingInitializer;

/**
 * gms-flavor entry point. Adds Firebase Cloud Messaging initialization and the
 * POST_NOTIFICATIONS permission request on top of the shared WebView/UI logic in
 * {@link BaseMainActivity}.
 */
public class MainActivity extends BaseMainActivity {

    @Override
    protected void initializeMessaging() {
        if (MessagingInitializer.isInitialized()) {
            return;
        }
        new Thread(() -> {
            try {
                MessagingInitializer.initialize(getApplicationContext());
            } catch (Exception e) {
                runOnUiThread(() -> sentryManager.captureException(e));
            }
        }, "fcm-init").start();
    }

    @Override
    protected void onBiometricAuthenticationSucceeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(this,
                        new String[] { POST_NOTIFICATIONS },
                        2);
            }
        }
    }
}
