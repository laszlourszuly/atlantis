[![Circle CI](https://circleci.com/gh/echsylon/atlantis/tree/master.svg?style=svg)](https://circleci.com/gh/echsylon/atlantis/tree/master)

# Atlantis

<big><em>
This project aims to offer easy means to control network responses in situations where such control is extra desirable. This can be in automated testing or situations when the backend server isn't reliable or available, e.g. during new development.  
</em></big>

## How it works

The Atlantis library holds a minimal web server which is started and stopped at will by the target implementation. The web server is configurable to take command over a certain domain (often `localhost` or alike) and serve pre-configured responses to certain end points.

The configuration is a JSON based file, placed as an asset in the app. Alltogether, this library and its corresponding configuration, can easily be placed in a separate build type or flavor, keeping it isolated from your production code still easily at hand when needed.

## Configuration

The configuration file is a simple JSON file with a known pattern. You simply state how a request looks like in terms of a URL, an HTTP request method and headers and - of course - a desired response.

As a lucky coincidence the config file protocol just happens to coincide with the output from [Postman](https://www.getpostman.com/docs) when downloading a collection as JSON.

A full example could look something like this:

````json
{
	"requests": [{
		"headers": "Content-Type: text/plain\nX-Request: custom\n",
		"url": "http://localhost:8080/path/to/resource?query=yes",
		"method": "GET",
		"responses": [{
			"responseCode": {
				"code": 200,
				"name": "OK"
			},
			"headers": [{
				"key": "X-Header-1",
				"value": "value1"
			}, {
				"key": "X-Header-2",
				"value": "value2"
			}],
			"mime": "application/json",
			"text": "{\"attr\": \"value\"}"
		}]
	}]
}
````

## Integration

The Atlantis library is made available through JCenter, which is the default repository for Android Gradle projects. You simply add a entry in the `dependencies` section in your `build.gradle` file, like so:

````groovy
dependencies {
    compile 'com.echsylon.atlantis:atlantis:1.0.0'
}
````

## Dependencies

The Atlantis library depends on the following third party libraries:

* Google Gson (JSON parsing)
* NanoHTTPD (local web server)

## Building from source

The library is developed in Android Studio, which in turn relies on the Gradle build system. To build the Atlantis library from source you simply issue the following terminal command from the project root directory:

    ./gradlew clean assemble

The different packagings of the library are then to be found in the `{PROJ_ROOT}/library/build/outputs/aar` directory for Android archives and `{PROJ_ROOT}/library/build/outputs/jar` directory for Java archives.

There are several built Java archives, most of them self explanatory by their names, with the exception of the "fat" archive, maybe. The "fat" archive contains all it's internal dependencies as well, which means that you won't need to satisfy those constraints from your own app. Note, though, that if your app itself has dependencies to the same libraries, you will get naming conflicts if using the "fat" JAR.

## Testing the source

There is a set of test cases written for the project. You can run the entire test suite by issuing the following terminal command from the project root directory:

    ./gradlew clean test

Make sure you have a device connected or an emulator running.

Any test results are found in the `{PROJ_ROOT}/sdk/build/reports/` directory. Note that code coverage can only be generated on rooted devices or the emulator.
