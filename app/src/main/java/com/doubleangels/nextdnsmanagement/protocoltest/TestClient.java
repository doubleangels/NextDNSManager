package com.doubleangels.nextdnsmanagement.protocoltest;

import android.content.Context;

import com.doubleangels.nextdnsmanagement.R;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLHandshakeException;

import io.sentry.ITransaction;
import io.sentry.Sentry;
import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class TestClient {
    private static Retrofit retrofit;

    private TestClient() {
        // Private constructor to prevent instantiation
    }

    public static Retrofit getBaseClient(Context context) {
        // If retrofit is not initialized, create it
        if (retrofit == null) {
            retrofit = createRetrofit(context);
        }
        return retrofit;
    }

    private static OkHttpClient createOkHttpClient(Context context) {
        // Define cache directory
        File cacheDir = new File(context.getCacheDir(), "http-cache");
        // Set cache size
        Cache cache = new Cache(cacheDir, 10 * 1024 * 1024); // 10MB cache size

        // Build OkHttpClient with cache and interceptor
        return new OkHttpClient.Builder()
                .cache(cache)
                .addInterceptor(createSentryInterceptor())
                .build();
    }

    private static Interceptor createSentryInterceptor() {
        return chain -> {
            // Start a Sentry transaction
            ITransaction createSentryInterceptorTransaction = Sentry.startTransaction("TestClient_createSentryInterceptor()", "TestClient");
            try {
                // Intercept the request
                Request request = chain.request();
                // Proceed with the request
                return chain.proceed(request);
            } catch (IOException e) {
                // Check if error is recoverable
                if (isRecoverableError(e)) {
                    // Add breadcrumb for recoverable error
                    Sentry.addBreadcrumb("Unable to query NextDNS encryption protocol: " + e);
                } else {
                    // Capture exception in Sentry
                    Sentry.captureException(e);
                }
                // Rethrow the exception
                throw e;
            } finally {
                // Finish Sentry transaction
                createSentryInterceptorTransaction.finish();
            }
        };
    }

    private static boolean isRecoverableError(IOException e) {
        // Check if the IOException is a recoverable error
        return e instanceof UnknownHostException || e instanceof SocketTimeoutException || e instanceof SSLHandshakeException || e instanceof SocketException;
    }

    private static Retrofit createRetrofit(Context context) {
        // Create OkHttpClient
        OkHttpClient client = createOkHttpClient(context);

        // Build Retrofit instance
        return new Retrofit.Builder()
                .baseUrl(context.getString(R.string.test_url)) // Base URL from resources
                .addConverterFactory(GsonConverterFactory.create()) // Use Gson for JSON conversion
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create()) // Use RxJava2 for async calls
                .client(client) // Set the OkHttpClient
                .build();
    }
}
