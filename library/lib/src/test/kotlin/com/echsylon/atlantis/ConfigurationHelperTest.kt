package com.echsylon.atlantis

import com.echsylon.atlantis.request.Order
import com.echsylon.atlantis.request.Pattern
import com.echsylon.atlantis.response.Response
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlin.text.Charsets.UTF_8
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should contain same`
import org.amshove.kluent.`should throw`
import org.amshove.kluent.invoking
import org.junit.Test
import java.io.ByteArrayInputStream

class ConfigurationHelperTest {

    @Test
    fun `When parsing json configuration request, the pattern is correctly parsed`() {
        val json = "{" +
                "    \"requests\": [{" +
                "        \"verb\": \"PUT\"," +
                "        \"path\": \"/path/.*\"," +
                "        \"protocol\": \"HTTP/1000\"," +
                "        \"headers\": [" +
                "            \"Accept: text/plain\"" +
                "        ]," +
                "        \"responseOrder\": \"RANDOM\"," +
                "        \"responses\": [{}]" +
                "    }]" +
                "}"

        val helper = ConfigurationHelper()
        val reader = ByteArrayInputStream(json.toByteArray())
        val pattern = slot<Pattern>()
        val mock = mockk<Configuration> { every { addResponse(any(), any()) } returns Unit }

        helper.addFromJson(mock, reader)
        verify(exactly = 1) { mock.addResponse(capture(pattern), any()) }
        pattern.captured.verb `should be equal to` "PUT"
        pattern.captured.path `should be equal to` "/path/.*"
        pattern.captured.protocol `should be equal to` "HTTP/1000"
        pattern.captured.headers `should contain same` listOf("Accept: text/plain")
        pattern.captured.responseOrder `should be equal to` Order.RANDOM
    }

    @Test
    fun `When parsing json configuration with empty request pattern, it is parsed with default values`() {
        val json = "{" +
                "    \"requests\": [{" +
                "        \"responses\": [{}]" +
                "    }]" +
                "}"

        val helper = ConfigurationHelper()
        val reader = ByteArrayInputStream(json.toByteArray())
        val pattern = slot<Pattern>()
        val mock = mockk<Configuration> { every { addResponse(any(), any()) } returns Unit }

        helper.addFromJson(mock, reader)
        verify(exactly = 1) { mock.addResponse(capture(pattern), any()) }
        pattern.captured `should be equal to` Pattern()
    }

    @Test
    fun `When parsing json configuration request without a response, the request is not saved`() {
        val json = "{" +
                "    \"requests\": [{" +
                "        \"verb\": \"PUT\"," +
                "        \"path\": \"/path/.*\"" +
                "    }]" +
                "}"

        val helper = ConfigurationHelper()
        val reader = ByteArrayInputStream(json.toByteArray())
        val mock = mockk<Configuration> { every { addResponse(any(), any()) } returns Unit }

        helper.addFromJson(mock, reader)
        verify(exactly = 0) { mock.addResponse(any(), any()) }
    }

    @Test
    fun `When parsing json configuration request, the response is correctly parsed`() {
        val json = "{" +
                "    \"requests\": [{" +
                "        \"responses\": [{" +
                "            \"code\": 201," +
                "            \"protocol\": \"HTTP/2000\"," +
                "            \"headers\": [" +
                "                \"Content-Type: text/plain\"," +
                "                \"Content-Length: 3\"" +
                "            ]," +
                "            \"content\": \"Hej\"," +
                "            \"behavior\": {" +
                "                \"chunk\": [1, 1600]," +
                "                \"delay\": [10, 500]," +
                "                \"calculateContentLengthIfAbsent\": true" +
                "            }" +
                "        }]" +
                "    }]" +
                "}"

        val helper = ConfigurationHelper()
        val reader = ByteArrayInputStream(json.toByteArray())
        val response = slot<Response>()
        val mock = mockk<Configuration> { every { addResponse(any(), any()) } returns Unit }

        helper.addFromJson(mock, reader)
        verify(exactly = 1) { mock.addResponse(any(), capture(response)) }
        response.captured.code `should be equal to` 201
        response.captured.protocol `should be equal to` "HTTP/2000"
        response.captured.headers `should contain same` listOf("Content-Type: text/plain", "Content-Length: 3")
        response.captured.content.toString(UTF_8) `should be equal to` "Hej"
        response.captured.behavior.chunk `should be equal to` 1UL..1600UL
        response.captured.behavior.delay `should be equal to` 10UL..500UL
        response.captured.behavior.calculateLength `should be equal to` true
    }

    @Test
    fun `When parsing json configuration response with too long chunk array, an error is thrown`() {
        val json = "{" +
                "    \"requests\": [{" +
                "        \"responses\": [{" +
                "            \"behavior\": {" +
                "                \"chunk\": [1, 800, 1600]" +
                "            }" +
                "        }]" +
                "    }]" +
                "}"

        val helper = ConfigurationHelper()
        val reader = ByteArrayInputStream(json.toByteArray())
        val mock = mockk<Configuration> { every { addResponse(any(), any()) } returns Unit }
        invoking { helper.addFromJson(mock, reader) } `should throw` IllegalStateException::class
    }

    @Test
    fun `When parsing json configuration response with too long delay array, an error is thrown`() {
        val json = "{" +
                "    \"requests\": [{" +
                "        \"responses\": [{" +
                "            \"behavior\": {" +
                "                \"delay\": [100, 150, 200]" +
                "            }" +
                "        }]" +
                "    }]" +
                "}"

        val helper = ConfigurationHelper()
        val reader = ByteArrayInputStream(json.toByteArray())
        val mock = mockk<Configuration> { every { addResponse(any(), any()) } returns Unit }
        invoking { helper.addFromJson(mock, reader) } `should throw` IllegalStateException::class
    }

    @Test
    fun `When parsing json configuration response with too short chunk array, an error is thrown`() {
        val json = "{" +
                "    \"requests\": [{" +
                "        \"responses\": [{" +
                "            \"behavior\": {" +
                "                \"chunk\": [1]" +
                "            }" +
                "        }]" +
                "    }]" +
                "}"

        val helper = ConfigurationHelper()
        val reader = ByteArrayInputStream(json.toByteArray())
        val mock = mockk<Configuration> { every { addResponse(any(), any()) } returns Unit }
        invoking { helper.addFromJson(mock, reader) } `should throw` IllegalStateException::class
    }

    @Test
    fun `When parsing json configuration response with too short delay array, an error is thrown`() {
        val json = "{" +
                "    \"requests\": [{" +
                "        \"responses\": [{" +
                "            \"behavior\": {" +
                "                \"delay\": [100]" +
                "            }" +
                "        }]" +
                "    }]" +
                "}"

        val helper = ConfigurationHelper()
        val reader = ByteArrayInputStream(json.toByteArray())
        val mock = mockk<Configuration> { every { addResponse(any(), any()) } returns Unit }
        invoking { helper.addFromJson(mock, reader) } `should throw` IllegalStateException::class
    }
}