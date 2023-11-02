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
    private static Retrofit retrofit;

    public static Retrofit getBaseClient(Context context) {
        if (retrofit == null) {
            File cacheDir = new File(context.getCacheDir(), "http-cache");
            Cache cache = new Cache(cacheDir, 10 * 1024 * 1024);

            OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                    .cache(cache)
                    .addInterceptor(chain -> {
                        try {
                            Request request = chain.request();
                            return chain.proceed(request);
                        } catch (IOException e) {
                            Sentry.captureException(e);
                            throw e;
                        }
                    });

            retrofit = new Retrofit.Builder()
                    .baseUrl(context.getString(R.string.test_url))
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .client(clientBuilder.build())
                    .build();
        }
        return retrofit;
    }
}
