## What is this?
Atlantis aims to help Kotlin developers to mock an API server without having any footprint on the actual application code itself. It does this by monitoring a user selected port on a socket level and map any HTTP requests received on it to a set of user configured mock responses. In practice this means that the developer can write real application code without having to design for the mocking infrastructure.

## How do I use it?
Start by adding below dependency to your gradle build file replacing "{version}" with the version of your liking. This documentation applies to version 4.0.0 and higher:

```groovy
implementation 'com.github.laszlourszuly:atlantis:{version}'
```

In code you can then configure and start your Atlantis server like so:

```kotlin
    val atlantis = Atlantis()
    atlantis.start()
    File("config.json")
        .inputStream()
        .use { atlantis.addConfiguration(it) }

    // In Android you would maybe prefer to use the "assets"
    // infrastructure instead, like so:
    // assets.open("config.json").use { ... }
```

As the above example suggests, you can change your configuration at any time, regardless of the current state of the Atlantis server.

## How do I configure it?
The easiest way to configure Atlantis is by writing a JSON configuration file. For smaller use cases, say unit tests, you can also create a configuration tree from regular domain objects exposed by Atlantis.

### The JSON API

```json
{
    "requests": [{
        "verb": "PUT",
        "path": "/path/.*",
        "protocol": "HTTP/1000",
        "headers": [
            "Accept: text/plain"
        ],
        "responseOrder": "RANDOM",
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
                "calculateContentLengthIfAbsent": false
            }
        }]
    }]
}
```

**Requests**
You configure each request pattern you want Atlantis to serve a mock response for in terms of "verb", "path", "protocol" and "headers". You can use regular expressions for both the request "verb" and "path". The protocol must be an exact match. The "headers" list defines the required subset of headers in the request in order to consider it a match.

Atlantis will check each incoming request on the configured port against your configuration and pick the first pattern that gives a full match.

**Responses**
You can configure multiple responses for each request pattern. By default these will be served in a wrapped sequential order, but you can also configure them to be served at random order.

You can also configure basic behavior for each mocked response in terms of "chunk" size and delay. Atlantis will split the mock response body in random sized chunks within the configured range and delay each chunk by a random amount of milliseconds in the corresponding range. By default no chunking and no delay is applied.

Furthermore you can choose to automatically calculate and set the "Content-Length" header based on the mocked response body.

### The domain object API
The root configuration is represented by the `com.echsylon.atlantis.Configuration` class. You can then define a request pattern as an instance of `com.echsylon.atlantis.request.Pattern` and corresponding mock response objects as `com.echsylon.atlantis.response.Response` and add them to the configuration object (the pattern is the key and the mock response is a value, yes, they will form a multi-map internally).
