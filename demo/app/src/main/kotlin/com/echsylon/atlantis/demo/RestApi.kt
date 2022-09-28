package com.echsylon.atlantis.demo

import retrofit2.http.GET
import retrofit2.http.Headers

interface RestApi {
    @GET("/api/json")
    @Headers("Accept: text/plain")
    suspend fun getJsonString(): String

    @GET("/api/next")
    @Headers("Accept: text/plain")
    suspend fun getNextString(): String

    @GET("/api/random")
    @Headers("Accept: text/plain")
    suspend fun getAnyString(): String
}