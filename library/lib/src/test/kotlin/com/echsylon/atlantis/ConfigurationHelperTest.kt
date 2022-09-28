package com.echsylon.atlantis

import com.echsylon.atlantis.message.Type
import com.echsylon.atlantis.request.Pattern
import com.echsylon.atlantis.response.Response
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.amshove.kluent.invoking
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should contain same`
import org.amshove.kluent.`should throw`
import org.junit.Test
import kotlin.text.Charsets.UTF_8

class ConfigurationHelperTest {

    @Test
    fun `When parsing json configuration request, the pattern is correctly parsed`() {
        val json = """{
                "requests": [{
                    "verb": "PUT",
                    "path": "/path/.*",
                    "protocol": "HTTP/1000",
                    "headers": [
                        "Accept: text/plain"
                    ],
                    "responseOrder": "RANDOM",
                    "responses": [{
                    }]
                }]
            }"""

        val helper = ConfigurationHelper()
        val pattern = slot<Pattern>()
        val mock = mockk<Configuration> { every { addResponse(capture(pattern), any()) } returns Unit }

        helper.addFromJson(mock, json.byteInputStream())
        verify(exactly = 1) { mock.addResponse(any(), any()) }
        pattern.captured.verb `should be equal to` "PUT"
        pattern.captured.path `should be equal to` "/path/.*"
        pattern.captured.protocol `should be equal to` "HTTP/1000"
        pattern.captured.headers `should contain same` listOf("Accept: text/plain")
        pattern.captured.responseOrder `should be equal to` Order.RANDOM
    }

    @Test
    fun `When parsing json configuration with empty request pattern, it is parsed with default values`() {
        val json = """{
                "requests": [{
                    "responses": [{
                    }]
                }]
            }"""

        val helper = ConfigurationHelper()
        val pattern = slot<Pattern>()
        val mock = mockk<Configuration> { every { addResponse(capture(pattern), any()) } returns Unit }

        helper.addFromJson(mock, json.byteInputStream())
        verify(exactly = 1) { mock.addResponse(any(), any()) }
        pattern.captured `should be equal to` Pattern()
    }

    @Test
    fun `When parsing json configuration request without a response, the request is not saved`() {
        val json = """{
                "requests": [{
                    "verb": "PUT",
                    "path": "/path/.*"
                }]
            }"""

        val helper = ConfigurationHelper()
        val mock = mockk<Configuration> { every { addResponse(any(), any()) } returns Unit }

        helper.addFromJson(mock, json.byteInputStream())
        verify(exactly = 0) { mock.addResponse(any(), any()) }
    }

    @Test
    fun `When parsing json configuration request, the response is correctly parsed`() {
        val json = """{
                "requests": [{
                    "responses": [{
                        "code": 201,
                        "protocol": "HTTP/2000",
                        "headers": [
                            "Content-Type: text/plain",
                            "Content-Length: 3"
                        ],
                        "content": "Hej",
                        "behavior": {
                            "chunk": [1, 1600],
                            "delay": [10, 500],
                            "calculateContentLengthIfAbsent": true,
                            "calculateSecWebSocketAcceptIfAbsent": true
                        },
                        "messages": [{
                            "path": "/path",
                            "text": "some text",
                            "chunk": 12
                        }]
                    }]
                }]
            }"""

        val helper = ConfigurationHelper()
        val response = slot<Response>()
        val mock = mockk<Configuration> { every { addResponse(any(), capture(response)) } returns Unit }

        helper.addFromJson(mock, json.byteInputStream())
        verify(exactly = 1) { mock.addResponse(any(), any()) }
        response.captured.code `should be equal to` 201
        response.captured.protocol `should be equal to` "HTTP/2000"
        response.captured.headers `should contain same` listOf("Content-Type: text/plain", "Content-Length: 3")
        response.captured.content.toString(UTF_8) `should be equal to` "Hej"
        response.captured.chunk `should be equal to` 1..1600
        response.captured.delay `should be equal to` 10..500
        response.captured.behavior.calculateLength `should be equal to` true
        response.captured.behavior.calculateWsAccept `should be equal to` true
        response.captured.messages.size `should be equal to` 1
    }

    @Test
    fun `When parsing json configuration response with too long chunk array, an error is thrown`() {
        val json = """{
                "requests": [{
                    "responses": [{
                        "behavior": {
                            "chunk": [1, 800, 1600]
                        }
                    }]
                }]
            }"""

        val helper = ConfigurationHelper()
        val mock = mockk<Configuration> { every { addResponse(any(), any()) } returns Unit }
        invoking { helper.addFromJson(mock, json.byteInputStream()) } `should throw` IllegalStateException::class
    }

    @Test
    fun `When parsing json configuration response with too long delay array, an error is thrown`() {
        val json = """{
                "requests": [{
                    "responses": [{
                        "behavior": {
                            "delay": [100, 150, 200]
                        }
                    }]
                }]
            }"""

        val helper = ConfigurationHelper()
        val mock = mockk<Configuration> { every { addResponse(any(), any()) } returns Unit }
        invoking { helper.addFromJson(mock, json.byteInputStream()) } `should throw` IllegalStateException::class
    }

    @Test
    fun `When parsing json configuration response with too short chunk array, an error is thrown`() {
        val json = """{
                "requests": [{
                    "responses": [{
                        "behavior": {
                            "chunk": [1]
                        }
                    }]
                }]
            }"""

        val helper = ConfigurationHelper()
        val mock = mockk<Configuration> { every { addResponse(any(), any()) } returns Unit }
        invoking { helper.addFromJson(mock, json.byteInputStream()) } `should throw` IllegalStateException::class
    }

    @Test
    fun `When parsing json configuration response with too short delay array, an error is thrown`() {
        val json = """{
                "requests": [{
                    "responses": [{
                        "behavior": {
                            "delay": [1]
                        }
                    }]
                }]
            }"""

        val helper = ConfigurationHelper()
        val mock = mockk<Configuration> { every { addResponse(any(), any()) } returns Unit }
        invoking { helper.addFromJson(mock, json.byteInputStream()) } `should throw` IllegalStateException::class
    }

    @Test
    fun `When parsing json configuration message with path attribute, the correct path is parsed`() {
        val json = """{
                "requests": [{
                    "path": "/path/to/request",
                    "responses": [{
                        "messages": [{
                            "path": "/path/to/request/message"
                        }]
                    }]
                }]
            }"""

        val helper = ConfigurationHelper()
        val response = slot<Response>()
        val mock = mockk<Configuration> { every { addResponse(any(), capture(response)) } returns Unit }

        helper.addFromJson(mock, json.byteInputStream())
        verify(exactly = 1) { mock.addResponse(any(), any()) }
        response.captured.messages[0].path `should be equal to` "/path/to/request/message"
    }

    @Test
    fun `When parsing json configuration message with data attribute, the attribute is correctly parsed`() {
        val json = """{
                "requests": [{
                    "responses": [{
                        "messages": [{
                            "data": "0x7E127F"
                        }]
                    }]
                }]
            }"""

        val helper = ConfigurationHelper()
        val response = slot<Response>()
        val mock = mockk<Configuration> { every { addResponse(any(), capture(response)) } returns Unit }

        helper.addFromJson(mock, json.byteInputStream())
        verify(exactly = 1) { mock.addResponse(any(), any()) }
        response.captured.messages[0].payload!! `should be equal to` byteArrayOf(0x7E, 0x12, 0x7F)
    }

    @Test
    fun `When parsing json configuration message with a valid type attribute, the attribute correctly parsed`() {
        val json = """{
                "requests": [{
                    "responses": [{
                        "messages": [{
                            "type": "Text"
                        }]
                    }]
                }]
            }"""

        val helper = ConfigurationHelper()
        val response = slot<Response>()
        val mock = mockk<Configuration> { every { addResponse(any(), capture(response)) } returns Unit }

        helper.addFromJson(mock, json.byteInputStream())
        verify(exactly = 1) { mock.addResponse(any(), any()) }
        response.captured.messages[0].type `should be equal to` Type.TEXT
    }

    @Test
    fun `When parsing json configuration message with an invalid type attribute, the attribute is not set`() {
        val json = """{
                "requests": [{
                    "responses": [{
                        "messages": [{
                            "type": "WRONG"
                        }]
                    }]
                }]
            }"""

        val helper = ConfigurationHelper()
        val response = slot<Response>()
        val mock = mockk<Configuration> { every { addResponse(any(), capture(response)) } returns Unit }

        helper.addFromJson(mock, json.byteInputStream())
        verify(exactly = 1) { mock.addResponse(any(), any()) }
        response.captured.messages[0].type `should be equal to` null
    }

    @Test
    fun `When parsing json configuration message with too short delay array, an error is thrown`() {
        val json = """{
                "requests": [{
                    "responses": [{
                        "messages": [{
                            "delay": [1]
                        }]
                    }]
                }]
            }"""

        val helper = ConfigurationHelper()
        val mock = mockk<Configuration> { every { addResponse(any(), any()) } returns Unit }
        invoking { helper.addFromJson(mock, json.byteInputStream()) } `should throw` IllegalStateException::class
    }

    @Test
    fun `When parsing json configuration message with too long delay array, an error is thrown`() {
        val json = """{
                "requests": [{
                    "responses": [{
                        "messages": [{
                            "delay": [100, 150, 200]
                        }]
                    }]
                }]
            }"""

        val helper = ConfigurationHelper()
        val mock = mockk<Configuration> { every { addResponse(any(), any()) } returns Unit }
        invoking { helper.addFromJson(mock, json.byteInputStream()) } `should throw` IllegalStateException::class
    }

    @Test
    fun `When parsing json configuration message with too short chunk array, an error is thrown`() {
        val json = """{
                "requests": [{
                    "responses": [{
                        "messages": [{
                            "chunk": [1000]
                        }]
                    }]
                }]
            }"""

        val helper = ConfigurationHelper()
        val mock = mockk<Configuration> { every { addResponse(any(), any()) } returns Unit }
        invoking { helper.addFromJson(mock, json.byteInputStream()) } `should throw` IllegalStateException::class
    }

    @Test
    fun `When parsing json configuration message with too long chunk array, an error is thrown`() {
        val json = """{
                "requests": [{
                    "responses": [{
                        "messages": [{
                            "chunk": [1000, 1500, 2000]
                        }]
                    }]
                }]
            }"""

        val helper = ConfigurationHelper()
        val mock = mockk<Configuration> { every { addResponse(any(), any()) } returns Unit }
        invoking { helper.addFromJson(mock, json.byteInputStream()) } `should throw` IllegalStateException::class
    }

}
