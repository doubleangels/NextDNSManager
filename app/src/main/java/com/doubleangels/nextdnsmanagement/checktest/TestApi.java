package com.doubleangels.nextdnsmanagement.checktest;

import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;

// Definition of a Java interface named TestApi.
public interface TestApi {
    // Annotation to specify HTTP request headers for this API endpoint.
    @Headers({
            "Accept: application/json",  // Specify that the response format should be JSON.
            "Cache-Control: no-cache"   // Instruct the client not to cache the response.
    })

    // Annotation to specify that this method represents an HTTP GET request.
    @GET("/")
    // Method signature for making an HTTP GET request and receiving a JsonObject response.
    Call<JsonObject> getResponse();

}