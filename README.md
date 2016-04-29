[![Circle CI](https://circleci.com/gh/echsylon/atlantis/tree/master.svg?style=svg)](https://circleci.com/gh/echsylon/atlantis/tree/master)

### Integrate

If you're using Android Studio and Gradle you can easily start using Atlantis simply by listing it in your Gradle `dependencies` section:

```groovy
dependencies {
    compile 'com.echsylon.atlantis:atlantis:1.0.0'
}
```

### Configure

You need to define which requests you're mocking and what response they should deliver. You do this in a JSON file that could look something like:

```json
{
    "requests": [{
        "headers": "Content-Type: text/plain\nX-Custom: c_req_header\n",
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
```

### Use

The tool offers a very straight forward API; you can start it:

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Uri uri = Uri.parse("http://localhost:8080");
    String configAssetName = "atlantis.json";
    Atlantis.OnSuccessListener successCallback = null;
    Atlantis.OnErrorListener errorCallback = null;

    Atlantis.start(getContext(),
            uri.getHost(),
            uri.getPort(),
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
        Atlantis.shutdown();
    }
```

### More
Feel free to consult the [Wiki](https://github.com/echsylon/atlantis/wiki) for details and more info.

