package com.doubleangels.nextdnsmanagement.biometriclock;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.doubleangels.nextdnsmanagement.sentry.SentryManager;

import java.util.concurrent.Executor;

public class BiometricLock {

    private final AppCompatActivity activity;

    public BiometricLock(AppCompatActivity activity) {
        this.activity = activity;
    }

    public boolean canAuthenticate() {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return false;
        }
        try {
            BiometricManager biometricManager = BiometricManager.from(activity);
            int canAuth = biometricManager.canAuthenticate(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG
                            | BiometricManager.Authenticators.DEVICE_CREDENTIAL);
            boolean result = (canAuth == BiometricManager.BIOMETRIC_SUCCESS);
            if (!result) {
                SentryManager.captureMessage("Biometric authentication not available. Status: " + canAuth);
            }
            return result;
        } catch (Exception e) {
            SentryManager.captureException(e);
            return false;
        }
    }

    public void showPrompt(String title,
            String subtitle,
            String description,
            BiometricLockCallback callback) {

        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        Executor executor = ContextCompat.getMainExecutor(activity);

        BiometricPrompt biometricPrompt = new BiometricPrompt(activity,
                executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errorCode,
                            @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        if (activity.isFinishing() || activity.isDestroyed()) {
                            return;
                        }
                        String errorMessage = "Biometric authentication error (" + errorCode + "): " + errString;
                        SentryManager.captureMessage(errorMessage);
                        callback.onAuthenticationError(errorCode, errString.toString());
                    }

                    @Override
                    public void onAuthenticationSucceeded(
                            @NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        if (activity.isFinishing() || activity.isDestroyed()) {
                            return;
                        }
                        callback.onAuthenticationSucceeded();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        if (activity.isFinishing() || activity.isDestroyed()) {
                            return;
                        }
                        String failMessage = "Biometric authentication failed: unrecognized biometric input.";
                        SentryManager.captureMessage(failMessage);
                        callback.onAuthenticationFailed();
                    }
                });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setDescription(description)
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG
                        | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();

        try {
            biometricPrompt.authenticate(promptInfo);
        } catch (Exception e) {
            SentryManager.captureException(e);
            callback.onAuthenticationError(BiometricPrompt.ERROR_HW_UNAVAILABLE, e.getMessage());
        }
    }

    public interface BiometricLockCallback {
        void onAuthenticationSucceeded();

        void onAuthenticationError(int errorCode, String error);

        void onAuthenticationFailed();
    }
}
