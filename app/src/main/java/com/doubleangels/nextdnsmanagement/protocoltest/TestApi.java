package com.doubleangels.nextdnsmanagement.protocoltest;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;

public interface TestApi {
    @Headers({
            "Accept: application/json",
            "Cache-Control: no-cache"
    })
    @GET("/")
    Call<JsonObject> getResponse();
}