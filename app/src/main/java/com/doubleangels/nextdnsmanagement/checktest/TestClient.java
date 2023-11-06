package com.doubleangels.nextdnsmanagement.checktest;

import android.content.Context;
import com.doubleangels.nextdnsmanagement.R;

import io.sentry.Sentry;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.File;
import java.io.IOException;

public class TestClient {
    private static volatile Retrofit retrofit;
    private static OkHttpClient okHttpClient;
    private TestClient() {
        // Private constructor to prevent instantiation
    }

    public static Retrofit getBaseClient(Context context) {
        if (retrofit == null) {
            synchronized (TestClient.class) {
                if (retrofit == null) {
                    // If the Retrofit instance is not initialized, create one
                    retrofit = createRetrofit(context);
                }
            }
        }
        return retrofit;
    }

    private static OkHttpClient getOkHttpClient(Context context) {
        if (okHttpClient == null) {
            // Define a cache directory for OkHttpClient
            File cacheDir = new File(context.getCacheDir(), "http-cache");
            Cache cache = new Cache(cacheDir, 10 * 1024 * 1024); // 10MB cache size

            // Build an OkHttpClient with a cache and an interceptor
            OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                    .cache(cache)
                    .addInterceptor(chain -> {
                        try {
                            // Intercept the request and capture exceptions using Sentry
                            Request request = chain.request();
                            return chain.proceed(request);
                        } catch (IOException e) {
                            Sentry.captureException(e); // Capture and report the exception to Sentry
                            throw e;
                        }
                    });

            okHttpClient = clientBuilder.build();
        }
        return okHttpClient;
    }

    private static Retrofit createRetrofit(Context context) {
        OkHttpClient client = getOkHttpClient(context);

        return new Retrofit.Builder()
                .baseUrl(context.getString(R.string.test_url)) // Base URL from resources
                .addConverterFactory(GsonConverterFactory.create()) // Use Gson for JSON conversion
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create()) // Use RxJava2 for async calls
                .client(client) // Set the OkHttpClient
                .build();
    }
}
