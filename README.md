[![Circle CI](https://circleci.com/gh/echsylon/atlantis/tree/master.svg?style=shield)](https://circleci.com/gh/echsylon/atlantis/tree/master) [![Coverage Status](https://coveralls.io/repos/github/echsylon/atlantis/badge.svg?branch=master)](https://coveralls.io/github/echsylon/atlantis?branch=master)

### What it is

The Atlantis library will aid you in "mocking the Internet" for your Android app. It offers an isolated and ideal representation of the Internet, as you want it, without forcing you to do any changes to your code.

### How to integrate

If you're using Android Studio and Gradle you can easily start using Atlantis simply by listing it in your Gradle `dependencies` section:

```groovy
dependencies {
    compile 'com.echsylon.atlantis:atlantis:1.5.0'
}
```

### How to configure

You need to define which requests you're mocking and what response they should deliver. You do this in a JSON file that could look something like:

```json
{
    "requests": [{
        "headers": {
            "Content-Type": "text/plain",
            "X-Custom": "c_req_header"
        },
        "url": "http://localhost:8080/path/to/resource?query=yes",
        "method": "GET",
        "responses": [{
            "responseCode": {
                "code": 200,
                "name": "OK"
            },
            "headers": {
                "X-Header-1": "value1",
                "X-Header-2": "value2"
            },
            "delay": 1000,
            "maxDelay": 12000,
            "mime": "application/json",
            "text": "{\"attr\": \"value\"}",
            "asset": "asset://anyFile.mp3"
        }]
    }]
}
```

### How to use

Atlantis offers a very straight forward default API; at a minimum you can start it:

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    String configAssetName = "atlantis.json";

    // These babies are optional, implement them if you need them.
    Atlantis.OnSuccessListener successCallback = null;
    Atlantis.OnErrorListener errorCallback = null;

    mServer = Atlantis.start(getContext(),
            configAssetName,
            successCallback,
            errorCallback);
}
```

...and you can shut it down:

```java
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mServer.close();
    }
```

A more complete documentation can be found on the Wiki (see below). There is also a sample implementation along the code which you can have a peek on.

Note that the example host, the `jsontest.com` service, is heavily used by many and it will most likely reach its maximum daily quota somewhere afternoon-ish. You can in such case, either try to run the sample app earlier on the day, or set up your own test server (instructions can be found on [http://www.jsontest.com/](http://www.jsontest.com/)). 

### More
The [Wiki](https://github.com/echsylon/atlantis/wiki) contains more details and information.
