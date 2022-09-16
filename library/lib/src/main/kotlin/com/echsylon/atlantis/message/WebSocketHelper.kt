package com.echsylon.atlantis.message

import com.echsylon.atlantis.response.Message
import okio.BufferedSink
import okio.BufferedSource

/**
 * This class offers common convenience actions often performed on WebSocket
 * connections.
 */
class WebSocketHelper(
    private val onMessage: (key: String, data: ByteArray, isText: Boolean) -> Unit
) {
    private val webSockets: HashMap<String, WebSocket> = hashMapOf()

    /**
     * Starts to actively process messages on the WebSocket with the associated
     * key.
     *
     * @param key The key by which the WebSocket is associated.
     * @param source The stream to read peer messages from.
     * @param sink The stream to write messages to.
     */
    fun start(key: String, source: BufferedSource, sink: BufferedSink) {
        getWebSocket(key)
            .start(source, sink)
    }

    /**
     * Adds the given messages to the write queue of the WebSocket with the
     * associated key.
     *
     * @param key The key by which the WebSocket is associated.
     * @param messages The messages to send to the peer client.
     */
    fun send(key: String, vararg messages: Message) {
        getWebSocket(key).send(*messages)
    }

    /**
     * Stops the WebSocket with the associated key and removes it from the
     * internal cache.
     *
     * @param key The key by which the WebSocket is associated.
     */
    fun stop(key: String) {
        webSockets[key]?.stop()
        webSockets.remove(key)
    }

    /**
     * Stops all WebSockets and clears the internal cache.
     */
    fun stopAll() {
        webSockets.forEach { (_, webSocket) -> webSocket.stop() }
        webSockets.clear()
    }

    /**
     * Returns an active WebSocket for the given key. This function ensures
     * there is an instance in the internal cache and will therefore always
     * return a WebSocket object.
     *
     * @param key The key identifying the WebSocket to get.
     * @return A WebSocket object.
     */
    private fun getWebSocket(key: String): WebSocket {
        // Don't return a closed WebSocket
        webSockets[key]
            ?.takeIf { it.isShutdown }
            ?.also { webSockets.remove(key) }

        return webSockets.getOrPut(key) {
            WebSocket(key) { message, isText -> onMessage(key, message, isText) }
        }
    }
}
