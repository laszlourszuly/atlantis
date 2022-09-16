package com.echsylon.atlantis

import com.echsylon.atlantis.extension.encodeHexStringAsByteArray
import com.echsylon.atlantis.message.Type
import com.echsylon.atlantis.request.Pattern
import com.echsylon.atlantis.response.Message
import com.echsylon.atlantis.response.Response
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken.BEGIN_ARRAY
import com.google.gson.stream.JsonToken.BEGIN_OBJECT
import com.google.gson.stream.JsonToken.END_ARRAY
import com.google.gson.stream.JsonToken.END_DOCUMENT
import com.google.gson.stream.JsonToken.END_OBJECT
import com.google.gson.stream.JsonToken.NAME
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Helps parse a JSON configuration into a domain object.
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
    //                   "calculateContentLengthIfAbsent": false,
    //                   "calculateSecWebSocketAcceptIfAbsent": false
    //               },
    //               "messageOrder": "SEQUENTIAL",
    //               "messages": [{
    //                   "type": "TEXT"|"DATA"|"CLOSE"|"PING"|"PONG",
    //                   "path": "/ws/messages",
    //                   "text": "some text",
    //                   "data": "0xDEADBEEF",
    //                   "code": 1000,
    //                   "chunk": [120, 2002],
    //                   "delay": [100, 4000]
    //               }]
    //           }]
    //       }]
    //    }
    companion object {
        private val BEHAVIOR_CHUNK = Regex("""\$\.requests\[\d+]\.responses\[\d+]\.behavior\.chunk\[\d+]""")
        private val BEHAVIOR_DELAY = Regex("""\$\.requests\[\d+]\.responses\[\d+]\.behavior\.delay\[\d+]""")

        private val REQUEST = Regex("""\$\.requests\[\d+]\.?""")
        private val REQUEST_PATH = Regex("""\$\.requests\[\d+]\.path""")
        private val REQUEST_HEADER = Regex("""\$\.requests\[\d+]\.headers\[\d+]""")
        private val REQUEST_PROTOCOL = Regex("""\$\.requests\[\d+]\.protocol""")

        private val RESPONSE = Regex("""\$\.requests\[\d+]\.responses\[\d+]\.?""")
        private val RESPONSE_CODE = Regex("""\$\.requests\[\d+]\.responses\[\d+]\.code""")
        private val RESPONSE_HEADER = Regex("""\$\.requests\[\d+]\.responses\[\d+]\.headers\[\d+]""")
        private val RESPONSE_PROTOCOL = Regex("""\$\.requests\[\d+]\.responses\[\d+]\.protocol""")

        private val MESSAGE = Regex("""\$\.requests\[\d+]\.responses\[\d+]\.messages\[\d+]\.?""")
        private val MESSAGE_CODE = Regex("""\$\.requests\[\d+]\.responses\[\d+]\.messages\[\d+]\.code""")
        private val MESSAGE_PATH = Regex("""\$\.requests\[\d+]\.responses\[\d+]\.messages\[\d+]\.path""")
        private val MESSAGE_DELAY = Regex("""\$\.requests\[\d+]\.responses\[\d+]\.messages\[\d+]\.delay\[\d+]""")
        private val MESSAGE_CHUNK = Regex("""\$\.requests\[\d+]\.responses\[\d+]\.messages\[\d+]\.chunk\[\d+]""")
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
        lateinit var pattern: Pattern
        lateinit var response: Response
        lateinit var message: Message

        reader.use { json ->
            while (true)
                when (json.peek()) {
                    BEGIN_OBJECT -> json.beginObject().also {
                        when {
                            REQUEST.matches(json.path) ->
                                pattern = Pattern()
                            RESPONSE.matches(json.path) ->
                                response = Response()
                            MESSAGE.matches(json.path) ->
                                message = Message()
                        }
                    }
                    END_OBJECT -> json.endObject().also {
                        when {
                            RESPONSE.matches(json.path) ->
                                configuration.addResponse(pattern, response)
                            MESSAGE.matches(json.path) ->
                                response.messages.add(message)
                        }
                    }
                    BEGIN_ARRAY -> json.beginArray().also {
                        when {
                            REQUEST_HEADER.matches(json.path) ->
                                while (json.hasNext()) {
                                    pattern.headers.add(json.nextString())
                                }
                            RESPONSE_HEADER.matches(json.path) ->
                                while (json.hasNext()) {
                                    response.headers.add(json.nextString())
                                }
                            BEHAVIOR_CHUNK.matches(json.path) ->
                                response.behavior.chunk = IntRange(
                                    json.nextInt(),
                                    json.nextInt()
                                ).also { json.endArray() }
                            BEHAVIOR_DELAY.matches(json.path) ->
                                response.behavior.delay = IntRange(
                                    json.nextInt(),
                                    json.nextInt()
                                ).also { json.endArray() }
                            MESSAGE_DELAY.matches(json.path) ->
                                message.delay = IntRange(
                                    json.nextInt(),
                                    json.nextInt()
                                ).also { json.endArray() }
                            MESSAGE_CHUNK.matches(json.path) ->
                                message.chunk = IntRange(
                                    json.nextInt(),
                                    json.nextInt()
                                ).also { json.endArray() }
                        }
                    }
                    END_ARRAY -> json.endArray()
                    NAME -> when (json.nextName()) {
                        "verb" -> pattern.verb = json.nextString()
                        "path" -> when {
                            REQUEST_PATH.matches(json.path) -> pattern.path = json.nextString()
                            MESSAGE_PATH.matches(json.path) -> message.path = json.nextString()
                            else -> json.skipValue()
                        }
                        "code" -> when {
                            RESPONSE_CODE.matches(json.path) -> response.code = json.nextInt()
                            MESSAGE_CODE.matches(json.path) -> message.code = json.nextInt()
                            else -> json.skipValue()
                        }
                        "content" -> response.content = json.nextString().encodeToByteArray()
                        "protocol" -> when {
                            REQUEST_PROTOCOL.matches(json.path) -> pattern.protocol = json.nextString()
                            RESPONSE_PROTOCOL.matches(json.path) -> response.protocol = json.nextString()
                            else -> json.skipValue()
                        }
                        "responseOrder" -> pattern.responseOrder = Order.from(json.nextString())
                        "messageOrder" -> response.messageOrder = Order.from(json.nextString())
                        "calculateContentLengthIfAbsent" -> response.behavior.calculateLength = json.nextBoolean()
                        "calculateSecWebSocketAcceptIfAbsent" -> response.behavior.calculateWsAccept = json.nextBoolean()
                        "text" -> message.text = json.nextString()
                        "data" -> message.data = json.nextString().encodeHexStringAsByteArray()
                        "type" -> message.type = Type.fromString(json.nextString())
                        "websockets" -> Unit
                        "requests" -> Unit
                        "headers" -> Unit
                        "responses" -> Unit
                        "messages" -> Unit
                        "behavior" -> Unit
                        "chunk" -> Unit
                        "delay" -> Unit
                        else -> json.skipValue()
                    }
                    END_DOCUMENT -> break
                    else -> json.skipValue()
                }
        }
    }
}
