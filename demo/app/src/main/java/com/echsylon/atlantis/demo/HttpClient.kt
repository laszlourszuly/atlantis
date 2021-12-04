package com.echsylon.atlantis.demo

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.create
import retrofit2.http.GET
import retrofit2.http.Headers
import kotlin.time.DurationUnit

interface HttpClient {
    companion object {
        fun create(): HttpClient {
            val httpClient = with(OkHttpClient.Builder()) {
                retryOnConnectionFailure(true)
                hostnameVerifier { _, _ -> true }
                build()
            }
            return Retrofit.Builder()
                .client(httpClient)
                .baseUrl("https://localhost:8080")
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