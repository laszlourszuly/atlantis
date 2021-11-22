package com.echsylon.atlantis.demo

import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.create
import retrofit2.http.GET
import retrofit2.http.Headers

interface HttpClient {
    companion object {
        fun create(): HttpClient {
            return Retrofit.Builder()
                .baseUrl("http://localhost:8080")
                .addConverterFactory(ScalarsConverterFactory.create())
                .build()
                .create()
        }
    }

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