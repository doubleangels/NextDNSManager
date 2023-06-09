package com.doubleangels.nextdnsmanagement.checktest;

import android.content.Context;

import com.doubleangels.nextdnsmanagement.R;

import io.sentry.ITransaction;
import io.sentry.Sentry;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TestClient {
    private static Retrofit retrofit = null;
    public static Retrofit getBaseClient(Context c) {
        ITransaction TestClient_create_transaction = Sentry.startTransaction("TestClient_getBaseClient()", "TestClient");
        try {
            if (retrofit == null) {
                retrofit = new Retrofit.Builder()
                        .baseUrl(c.getString(R.string.test_url))
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();
            }
        } catch (Exception e) {
            Sentry.captureException(e);
        }
        TestClient_create_transaction.finish();
        return retrofit;
    }
}
