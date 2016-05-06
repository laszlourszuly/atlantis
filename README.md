[![Circle CI](https://circleci.com/gh/echsylon/atlantis/tree/master.svg?style=svg)](https://circleci.com/gh/echsylon/atlantis/tree/master)

### Integrate

If you're using Android Studio and Gradle you can easily start using Atlantis simply by listing it in your Gradle `dependencies` section:

```groovy
dependencies {
    compile 'com.echsylon.atlantis:atlantis:1.1.0'
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
            "text": "{\"attr\": \"value\"}",
            "asset": "assetFileName.mp3"
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

    String configAssetName = "atlantis.json";
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

There is a complete sample implementation along the code which you can have a peek on.

Note that the example host, the `jsontest.com` service, is heavily used by many and it will most likely reach its maximum daily quota somewhere afternoon-ish. You can in such case, either try to run the sample app earlier on the day, or setup your own test server (instructions can be found on [http://www.jsontest.com/](http://www.jsontest.com/)). 

### More
The [Wiki](https://github.com/echsylon/atlantis/wiki) contains more details and information.

### License

The Atlantis library is licensed under the [Apache 2](http://www.apache.org/licenses/LICENSE-2.0) license.

The [Google Gson](https://github.com/google/gson) library is licensed under the [Apache 2](http://www.apache.org/licenses/LICENSE-2.0) license.

The [local web server](https://github.com/NanoHttpd/nanohttpd), used by Atlantis, is licensed as:

<code>
Copyright (c) 2012-2013 by Paul S. Hawke, 2001,2005-2013 by Jarno Elonen, 2010 by Konstantinos Togias
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

* Neither the name of the NanoHttpd organization nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
</code>
