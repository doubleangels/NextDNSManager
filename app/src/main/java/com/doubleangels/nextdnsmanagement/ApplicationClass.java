package com.doubleangels.nextdnsmanagement;

import android.app.Application;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.onesignal.OneSignal;

import io.sentry.ITransaction;
import io.sentry.Sentry;

public class ApplicationClass extends Application {
    private static final String ONESIGNAL_APP_ID = "dabc92aa-6dc5-4c29-a096-ac6eba076214";

    @Override
    public void onCreate() {
        ITransaction ApplicationClass_create_transaction = Sentry.startTransaction("onCreate()", "ApplicationClass");
        super.onCreate();
        try{
            OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE);
            OneSignal.initWithContext(this);
            OneSignal.setAppId(ONESIGNAL_APP_ID);
        } catch (Exception e){
            Sentry.captureException(e);
            FirebaseCrashlytics.getInstance().recordException(e);
        } finally {
            ApplicationClass_create_transaction.finish();
        }


    }
}
