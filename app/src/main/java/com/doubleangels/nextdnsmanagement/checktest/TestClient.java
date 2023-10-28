// Import statements for required libraries and classes.
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

// Definition of the TestClient class.
public class TestClient {
    private static Retrofit retrofit;

    // Method to get a base Retrofit client with caching enabled.
    public static Retrofit getBaseClient(Context context) {
        if (retrofit == null) {
            // Specify the cache directory and size for caching responses.
            File cacheDir = new File(context.getCacheDir(), "http-cache");
            Cache cache = new Cache(cacheDir, 10 * 1024 * 1024); // 10 MB cache size.

            // Create an OkHttpClient instance with caching enabled.
            OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                    .cache(cache)
                    .addInterceptor(chain -> {
                        try {
                            Request request = chain.request();
                            return chain.proceed(request);
                        } catch (IOException e) {
                            // Capture the exception with Sentry.
                            Sentry.captureException(e);
                            throw e; // Rethrow the exception to propagate it.
                        }
                    });

            // Build a Retrofit instance with various configurations:
            retrofit = new Retrofit.Builder()
                    .baseUrl(context.getString(R.string.test_url))  // Set the base URL from resources.
                    .addConverterFactory(GsonConverterFactory.create())  // Use Gson for JSON serialization/deserialization.
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())  // Use RxJava for handling asynchronous responses.
                    .client(clientBuilder.build())  // Set the custom OkHttpClient with caching.
                    .build();
        }
        return retrofit;
    }
}
