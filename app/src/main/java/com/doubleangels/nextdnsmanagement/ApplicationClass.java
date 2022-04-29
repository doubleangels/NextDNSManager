package com.doubleangels.nextdnsmanagement;

import android.app.Application;
import android.content.SharedPreferences;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.onesignal.OneSignal;

import java.util.concurrent.Executor;

import io.sentry.ITransaction;
import io.sentry.Sentry;

public class ApplicationClass extends Application {

    private static final String ONESIGNAL_APP_ID = "dabc92aa-6dc5-4c29-a096-ac6eba076214";
    private FirebaseRemoteConfig firebaseRemoteConfig;

    @Override
    public void onCreate() {
        ITransaction ApplicationClass_create_transaction = Sentry.startTransaction("onCreate()", "ApplicationClass");
        super.onCreate();
        try{
            // Set up our notifications.
            OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE);
            OneSignal.initWithContext(this);
            OneSignal.setAppId(ONESIGNAL_APP_ID);

            // Set up our shared preferences.
            SharedPreferences sharedPreferences = getSharedPreferences("sharedPreferences", MODE_PRIVATE);

            // Get our remote configuration information.
            Trace remoteConfigStartTrace = FirebasePerformance.getInstance().newTrace("remoteConfig_setup");
            remoteConfigStartTrace.start();
            firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
            FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(1800).build();
            firebaseRemoteConfig.setConfigSettingsAsync(configSettings);
            firebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);
            remoteConfigStartTrace.stop();
            Trace remoteConfigFetchTrace = FirebasePerformance.getInstance().newTrace("remoteConfig_fetch");
            remoteConfigFetchTrace.start();
            firebaseRemoteConfig.fetchAndActivate().addOnCompleteListener((Executor) this, task -> {
                if (task.isSuccessful()) {
                    boolean updated = task.getResult();
                    if (updated) {
                        Sentry.setTag("remote_config_fetched", "true");
                    } else {
                        Sentry.setTag("remote_config_fetched", "false");
                    }
                    firebaseRemoteConfig.activate();
                }
            });
            remoteConfigFetchTrace.stop();
        } catch (Exception e){
            captureException(e);
        } finally {
            ApplicationClass_create_transaction.finish();
        }
    }

    public void captureException(Exception exception) {
        Sentry.captureException(exception);
        FirebaseCrashlytics.getInstance().recordException(exception);
    }
}
