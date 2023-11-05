package com.doubleangels.nextdnsmanagement.checktest;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;

public interface TestApi {
    // Define the headers for the HTTP request
    @Headers({
            "Accept: application/json",
            "Cache-Control: no-cache"
    })

    // Define the HTTP GET request method with a base URL of "/"
    @GET("/")
    Call<JsonObject> getResponse(); // Specify the response type as JsonObject
}