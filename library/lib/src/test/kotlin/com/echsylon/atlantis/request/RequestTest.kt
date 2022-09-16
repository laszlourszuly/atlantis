package com.echsylon.atlantis.request

import org.amshove.kluent.`should be equal to`
import org.junit.Test

class RequestTest {

    @Test
    fun `When wrong request phrase, then isWebSocket returns false`() {
        val request = Request("PATCH")
            .apply { headers.add("Upgrade: websocket") }
            .apply { headers.add("Connection: Upgrade") }
            .apply { headers.add("Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==") }
        request.isWebSocket `should be equal to` false
    }

    @Test
    fun `When no Upgrade header is defined, then isWebSocket returns false`() {
        val request = Request("GET")
            .apply { headers.add("Connection: Upgrade") }
            .apply { headers.add("Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==") }
        request.isWebSocket `should be equal to` false
    }

    @Test
    fun `When wrong Upgrade header is defined, then isWebSocket returns false`() {
        val request = Request("GET")
            .apply { headers.add("Upgrade: wrong") }
            .apply { headers.add("Connection: Upgrade") }
            .apply { headers.add("Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==") }
        request.isWebSocket `should be equal to` false
    }

    @Test
    fun `When no Connection header is defined, then isWebSocket returns false`() {
        val request = Request("GET")
            .apply { headers.add("Upgrade: websocket") }
            .apply { headers.add("Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==") }
        request.isWebSocket `should be equal to` false
    }

    @Test
    fun `When wrong Connection header is defined, then isWebSocket returns false`() {
        val request = Request("GET")
            .apply { headers.add("Upgrade: websocket") }
            .apply { headers.add("Connection: wrong") }
            .apply { headers.add("Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==") }
        request.isWebSocket `should be equal to` false
    }

    @Test
    fun `When no Sec-WebSocket-Key header is defined, then isWebSocket returns false`() {
        val request = Request("GET")
            .apply { headers.add("Upgrade: websocket") }
            .apply { headers.add("Connection: Upgrade") }
        request.isWebSocket `should be equal to` false
    }

    @Test
    fun `When wrong Sec-WebSocket-Key header is defined, then isWebSocket returns false`() {
        val request = Request("GET")
            .apply { headers.add("Upgrade: websocket") }
            .apply { headers.add("Connection: upgrade") }
            .apply { headers.add("Sec-WebSocket-Key:") }
        request.isWebSocket `should be equal to` false
    }

    @Test
    fun `When correct phrase and Upgrade header and Connection header and Sec-WebSocket-Key header, then isWebSocket returns true`() {
        val request = Request("GET")
            .apply { headers.add("Upgrade: websocket") }
            .apply { headers.add("Connection: Upgrade") }
            .apply { headers.add("Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==") }
        request.isWebSocket `should be equal to` true
    }
}
