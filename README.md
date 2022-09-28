## What is this?
Atlantis aims to help Kotlin developers to mock an API server without having any footprint on the actual application code itself. It does this by monitoring a user selected localhost port on a socket level and map any HTTP requests received on it to a set of user configured mock responses. In practice this means that the developer can write real application code without having to design for the mocking infrastructure.

## How do I use it?
Start by adding below dependency to your gradle build configuration. The library is available through the Maven central repository.

```groovy
implementation 'com.echsylon.atlantis:atlantis:{version}'
```

In code you can then configure and start your Atlantis server like so:

```kotlin
    val atlantis = Atlantis()
    atlantis.start()
    File("config.json")
        .inputStream()
        .use { atlantis.addConfiguration(it) }

    // In Android you would maybe prefer to use the "assets"
    // infrastructure instead:
    // assets.open("config.json").use { ... }
```

You can change your configuration at any time, regardless of the current state of the Atlantis server.

## How do I configure it?
The easiest way to configure Atlantis is by writing a JSON configuration file. For smaller use cases, say unit tests, you can also create a configuration tree from regular POJO's as well exposed by Atlantis.

### The JSON API
Below is a pseudo-configuration for Atlantis which presents all attributes with some values. This example only serves as some sort of exhaustive example, but not all fields make sense in combination with eachother. The example doesn't even pass standard JSON validation. 
```json
{
    "requests": [{
        "verb": "(PUT|PATCH)",
        "path": "/api/content\?.*",
        "protocol": "HTTP/1.1",
        "headers": [
            "Content-Type: application/json",
 	    "Accept: text/plain"
        ],
        "responseOrder": "SEQUENTIAL"|"RANDOM",
        "responses": [{
            "code": 200,
            "protocol": "HTTP/1.1",
            "headers": [
                "Content-Type: text/plain"
            ],
            "content": "Hej",
            "chunk": [1, 1600],
            "delay": [10, 500],
            "behavior": {
                "calculateContentLengthIfAbsent": false,
                "calculateSecWebSocketAcceptIfAbsent": false
            },
            "messageOrder": "SEQUENTIAL"|"RANDOM"|"BATCH",
            "messages": [{
                "type": "TEXT"|"DATA"|"CLOSE"|"PING"|"PONG",
                "path": "/ws/messages",
                "text": "some text",
                "data": "0xDEADBEEF",
                "code": 1000,
                "chunk": [120, 2002],
                "delay": [100, 4000]
            }]
        }]
    }]
}
```

**Requests**
You configure each request pattern you want Atlantis to serve a mock response for in terms of "verb", "path", "protocol" and "headers". You can use regular expressions to describe the request "verb", "path" and "protocol". The "headers" list, on the other hand, defines the required subset of exact headers in the request in order to consider it a match.

Atlantis will check each incoming request on the configured port against your configuration and pick the first pattern that gives a full match.

**Responses**
You can configure multiple responses for each request pattern. By default these will be served in a wrapped sequential order, but you can also configure them to be served at random order.

You can also configure basic behavior for each mocked response in terms of "chunk" size and delay. Atlantis will split the mock response body in random sized chunks within the configured range and delay each chunk by a random amount of milliseconds in the corresponding delay range. By default no chunking and no delay is applied.

By default, Atlantis will calculate and set the "Content-Length" and "Sec-WebSocket-Accept" headers on relevant responses. You can turn this off if you have very peculiar cases by setting the "calculateContentLengthIfAbsent" and "calculateSecWebSocketAcceptIfAbsent" behaviour attributes respectively.

**WebSocket**
For each response you can also configure optional WebSocket messages. Every time that particular response is served for a request, one ("messageOrder" = SEQUENTIAL or RANDOM) or all ("messageOrder" = BATCH) configured messages will also be sent to a previously opened WebSocket.

You need to define a handshake request + response to enable WebSocket functionality. The handshake request MUST have the path set to whatever path your client uses when establishing the WebSocket connection, and it MUST have the "Connection: Upgrade" and "Upgrade: websocket" headers set. Similarly the handshake response MUST have a status code of 101 and MUST have the "Connection: Upgrade" and "Upgrade: websocket" headers set.

### A Real Example
A more realistic example for a WebSocket configuration could look something like this:
```json
{
    "requests": [{
        "verb": "GET",
        "path": "/ws/connect",
        "headers": [
            "Upgrade: websocket",
            "Connection: Upgrade"
        ],
        "responses": [{
            "code": 101,
            "headers": [
                "Upgrade: websocket",
                "Connection: Upgrade"
            ],
            "messageOrder": "BATCH",
            "messages": [{
                "type": "TEXT",
                "path": "/ws/connect",
                "text": "{ \"data\": 12 }",
                "delay": [1000, 2000]
            }, {
                "type": "TEXT",
                "path": "/ws/connect",
                "text": "{ \"data\": 33 }",
                "delay": [4000, 5000]
            }]
        }]
    }, {
        "verb": "PATCH",
        "path": "/api/update",
        "headers": [
            "Content-Type: application/json"
        ],
        "responses": [{
            "code": 202,
            "messages": [{
                "type": "TEXT",
                "path": "/ws/connect",
                "text": "{ \"data\": 4 }",
                "delay": [1000, 2000]
            }, {
                "type": "TEXT",
                "path": "/ws/connect",
                "text": "{ \"data\": 17 }",
                "delay": [1000, 2000]
            }, {
                "type": "CLOSE",
                "path": "/ws/connect",
                "code": 1011
            }]
        }]
    }]
}
```

Here we have two requests. The first one is a valid WebSocket handshake request, along with a corresponding handshake response. The handshake response will also send two messages, slightly delayed, upon successful connection.

The second request definition catches an regular REST API update call. It responds with a 202 (Accepted) response and shortly after also sends the first message to the WebSocket connection established in the first request/response. The next time the client calls the REST API update enpoint, the second message will be sent, and if called again, Atlantis will mimic a server side error by sending a close message with "1011 Internal error" reason over WebSocket. This will also close the WebSocket connection. 

### The domain object API
The root configuration is represented by the `com.echsylon.atlantis.Configuration` class. You can then define a request pattern as an instance of `com.echsylon.atlantis.request.Pattern` and corresponding mock response objects as `com.echsylon.atlantis.response.Response` and `com.echsylon.atlantis.message.Message` WebSocket messages and add them to the configuration object (the pattern is the key and the mock response is a value, and yes, they will form a multi-map internally).
