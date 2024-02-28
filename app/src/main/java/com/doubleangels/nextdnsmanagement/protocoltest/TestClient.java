package com.doubleangels.nextdnsmanagement.protocoltest;

import android.content.Context;

import com.doubleangels.nextdnsmanagement.R;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class TestClient {
    private static Retrofit retrofit;

    private TestClient() {
        // Private constructor to prevent instantiation
    }

    public static Retrofit getBaseClient(Context context) {
        return createRetrofit(context);
    }

    private static OkHttpClient createOkHttpClient(Context context) {
        return new OkHttpClient.Builder().build();
    }

    private static Retrofit createRetrofit(Context context) {
        OkHttpClient client = createOkHttpClient(context);
        return new Retrofit.Builder()
                .baseUrl(context.getString(R.string.test_url))
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(client)
                .build();
    }
}
