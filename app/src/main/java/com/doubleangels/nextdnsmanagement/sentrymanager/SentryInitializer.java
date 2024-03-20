package com.doubleangels.nextdnsmanagement.sentrymanager;

import android.content.Context;
import android.os.AsyncTask;

import io.sentry.android.core.SentryAndroid;

public class SentryInitializer extends AsyncTask<Context, Void, Void> {

    @Override
    protected Void doInBackground(Context... contexts) {
        if (contexts.length > 0) {
            Context context = contexts[0];
            SentryAndroid.init(context, options -> {
                options.setDsn("https://8b52cc2148b94716a69c9a4f0c0b4513@o244019.ingest.us.sentry.io/6270764");
                options.setEnableTracing(true);
                options.setAttachScreenshot(true);
                options.setAttachViewHierarchy(true);
                options.setTracesSampleRate(1.0);
                options.setEnableAppStartProfiling(true);
                options.setAnrEnabled(true);
            });
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
    }
}
