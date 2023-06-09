package com.doubleangels.nextdnsmanagement.checktest;

import android.content.Context;

import com.doubleangels.nextdnsmanagement.R;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TestClient {
    private static Retrofit retrofit = null;
    public static Retrofit getBaseClient(Context c) {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(c.getString(R.string.test_url))
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
