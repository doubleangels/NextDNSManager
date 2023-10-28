// Import statements for required libraries and classes.
package com.doubleangels.nextdnsmanagement.checktest;
import android.content.Context;
import com.doubleangels.nextdnsmanagement.R;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import java.io.File;

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
                    .cache(cache);

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
