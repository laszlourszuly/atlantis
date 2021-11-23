package com.echsylon.atlantis

import com.echsylon.atlantis.request.Order
import com.echsylon.atlantis.request.Order.SEQUENTIAL
import com.echsylon.atlantis.request.Pattern
import com.echsylon.atlantis.response.Behavior
import com.echsylon.atlantis.response.Response
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken.BEGIN_ARRAY
import com.google.gson.stream.JsonToken.BEGIN_OBJECT
import com.google.gson.stream.JsonToken.END_ARRAY
import com.google.gson.stream.JsonToken.END_DOCUMENT
import com.google.gson.stream.JsonToken.END_OBJECT
import com.google.gson.stream.JsonToken.NAME
import okio.Buffer
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Helps parsing a JSON configuration into a domain object.
 */
internal class ConfigurationHelper {
    //   Reference JSON protocol
    //  
    //   {
    //       "requests": [{
    //           "verb": "PUT",
    //           "path": "/path/.*",
    //           "protocol": "HTTP/1000",
    //           "headers": [
    //               "Accept: text/plain"
    //           ],
    //           "responseOrder": "RANDOM",
    //           "responses": [{
    //               "code": 201,
    //               "protocol": "HTTP/2000",
    //               "headers": [
    //                   "Content-Type: text/plain",
    //                   "Content-Length: 3"
    //               ],
    //               "content": "Hej",
    //               "behavior": {
    //                   "chunk": [1, 1600],
    //                   "delay": [10, 500],
    //                   "calculateContentLengthIfAbsent": false
    //               }
    //           }]
    //       }]
    //    }
    companion object {
        private val BEHAVIOR_CHUNK = Regex("""\$\.requests\[\d+]\.responses\[\d+]\.behavior\.chunk\[\d+]""")
        private val BEHAVIOR_DELAY = Regex("""\$\.requests\[\d+]\.responses\[\d+]\.behavior\.delay\[\d+]""")

        private val REQUEST = Regex("""\$\.requests\[\d+]\.?""")
        private val REQUEST_HEADER = Regex("""\$\.requests\[\d+]\.headers\[\d+]""")
        private val REQUEST_PROTOCOL = Regex("""\$\.requests\[\d+]\.protocol""")

        private val RESPONSE = Regex("""\$\.requests\[\d+]\.responses\[\d+]\.?""")
        private val RESPONSE_HEADER = Regex("""\$\.requests\[\d+]\.responses\[\d+]\.headers\[\d+]""")
        private val RESPONSE_PROTOCOL = Regex("""\$\.requests\[\d+]\.responses\[\d+]\.protocol""")
    }

    /**
     * Parses the given UTF-8 JSON data stream and adds the configuration
     * details to the given Configuration instance.
     *
     * @param configuration The configuration object to add the new data to.
     * @param jsonStream    The input stream serving the configuration JSON
     *                      to parse.
     */
    fun addFromJson(configuration: Configuration, jsonStream: InputStream) {
        val reader = JsonReader(InputStreamReader(jsonStream))
        lateinit var request: Pattern
        lateinit var response: Response

        reader.use { json ->
            while (true)
                when (json.peek()) {
                    BEGIN_OBJECT -> json.beginObject().also {
                        when {
                            REQUEST.matches(json.path) ->
                                request = Pattern()
                            RESPONSE.matches(json.path) ->
                                response = Response()
                        }
                    }
                    BEGIN_ARRAY -> json.beginArray().also {
                        when {
                            BEHAVIOR_CHUNK.matches(json.path) ->
                                response.behavior.chunk = ULongRange(
                                    json.nextLong().toULong(),
                                    json.nextLong().toULong()
                                ).also { json.endArray() }
                            BEHAVIOR_DELAY.matches(json.path) ->
                                response.behavior.delay = ULongRange(
                                    json.nextLong().toULong(),
                                    json.nextLong().toULong()
                                ).also { json.endArray() }
                            REQUEST_HEADER.matches(json.path) ->
                                while (json.hasNext()) {
                                    request.headers.add(json.nextString())
                                }
                            RESPONSE_HEADER.matches(json.path) ->
                                while (json.hasNext()) {
                                    response.headers.add(json.nextString())
                                }
                        }
                    }
                    END_ARRAY -> json.endArray()
                    END_OBJECT -> json.endObject().also {
                        when {
                            RESPONSE.matches(json.path) ->
                                configuration.addResponse(request, response)
                        }
                    }
                    NAME -> when (json.nextName()) {
                        "verb" -> request.verb = json.nextString()
                        "path" -> request.path = json.nextString()
                        "protocol" -> when {
                            REQUEST_PROTOCOL.matches(json.path) ->
                                request.protocol = json.nextString()
                            RESPONSE_PROTOCOL.matches(json.path) ->
                                response.protocol = json.nextString()
                            else ->
                                json.skipValue()
                        }
                        "responseOrder" -> request.responseOrder =
                            runCatching { Order.valueOf(json.nextString()) }
                                .getOrDefault(SEQUENTIAL)
                        "code" -> response.code = json.nextInt()
                        "content" -> response.content = json.nextString().encodeToByteArray()
                        "calculateContentLengthIfAbsent" -> response.behavior.calculateLength = json.nextBoolean()
                        else -> Unit
                    }
                    END_DOCUMENT -> break
                    else -> json.skipValue()
                }
        }
    }
}