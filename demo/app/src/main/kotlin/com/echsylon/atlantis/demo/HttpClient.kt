package com.echsylon.atlantis.demo

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.*
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.create

class HttpClient private constructor(okHttpClient: OkHttpClient, restApi: RestApi) : RestApi by restApi {
    companion object {
        fun create(): HttpClient {
            val client: OkHttpClient = OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .hostnameVerifier { _, _ -> true }
                .build()

            val api: RestApi = Retrofit.Builder()
                .client(client)
                .baseUrl(BuildConfig.BASE_API_URL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build()
                .create()

            return HttpClient(client, api)
        }
    }

    interface Event
    class Open : Event
    class Close(val reason: Int) : Event
    class Message(val text: String) : Event
    class Failure(val cause: Throwable) : Event


    private val client = okHttpClient
    private var webSocket: WebSocket? = null
    private val webSocketRequest: Request = Request.Builder()
        .url(BuildConfig.BASE_WS_URL)
        .build()

    @ExperimentalCoroutinesApi
    suspend fun connectWebSocket(): Flow<Event> = callbackFlow {
        val callback = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                trySendBlocking(Open()).onFailure { channel.close(it) }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                trySendBlocking(Message(text)).onFailure { channel.close(it) }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                trySendBlocking(Close(code))
                channel.close()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                trySendBlocking(Failure(t))
                channel.close(t)
            }
        }
        webSocket = client.newWebSocket(webSocketRequest, callback)
        awaitClose { webSocket?.cancel() }
    }

    fun sendMessage(message: String) {
        webSocket?.send(message)
    }

    fun disconnectWebSocket() {
        webSocket?.close(1000, null)
    }
}