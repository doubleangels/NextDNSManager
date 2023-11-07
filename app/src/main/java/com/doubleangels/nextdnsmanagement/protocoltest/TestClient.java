package com.doubleangels.nextdnsmanagement.protocoltest;

import android.content.Context;
import com.doubleangels.nextdnsmanagement.R;
import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import io.sentry.Sentry;
import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class TestClient {
    private static volatile Retrofit retrofit;

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

    private static OkHttpClient createOkHttpClient(Context context) {
        // Define a cache directory for OkHttpClient
        File cacheDir = new File(context.getCacheDir(), "http-cache");
        Cache cache = new Cache(cacheDir, 10 * 1024 * 1024); // 10MB cache size

        // Create an OkHttpClient with a cache and an interceptor
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .cache(cache)
                .addInterceptor(createSentryInterceptor());

        return clientBuilder.build();
    }

    private static Interceptor createSentryInterceptor() {
        return chain -> {
            try {
                // Intercept the request and capture exceptions using Sentry
                Request request = chain.request();
                return chain.proceed(request);
            } catch (IOException e) {
                if (e instanceof UnknownHostException || e instanceof SocketTimeoutException) {
                    // If the exception is one of these types, add it as a breadcrumb
                    Sentry.addBreadcrumb("Unable to query NextDNS encryption protocol: " + e);
                } else {
                    Sentry.captureException(e); // Capture and report the exception to Sentry
                }
                throw e;
            }
        };
    }

    private static Retrofit createRetrofit(Context context) {
        OkHttpClient client = createOkHttpClient(context);

        return new Retrofit.Builder()
                .baseUrl(context.getString(R.string.test_url)) // Base URL from resources
                .addConverterFactory(GsonConverterFactory.create()) // Use Gson for JSON conversion
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create()) // Use RxJava2 for async calls
                .client(client) // Set the OkHttpClient
                .build();
    }
}
