package com.echsylon.atlantis;

import android.content.Context;
import android.content.res.AssetManager;

import com.echsylon.atlantis.internal.json.JsonParser;
import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Stack;

import static org.hamcrest.CoreMatchers.either;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 16)
public class AtlantisTest {
    private static final String AUTHORITY = "http://localhost:8080";

    // Returns a mocked context that holds a mocked AssetManager which in turn
    // will deliver the given JSON when requesting the "config.json" asset.
    private Context getMockedContext(String configJson) throws Exception {
        Context context = mock(Context.class);
        AssetManager assetManager = mock(AssetManager.class);
        doReturn(assetManager).when(context).getAssets();
        doReturn(new ByteArrayInputStream(configJson.getBytes())).when(assetManager).open("config.json");
        return context;
    }

    // Returns a default mocked context and writes the given JSON to the given
    // file. The context and the file are not tied to each other by any means.
    private Context getMockedContext(String configJson, File file) throws Exception {
        Context context = mock(Context.class);
        Files.write(configJson, file, Charsets.UTF_8);
        return context;
    }

    // Returns a mocked context that holds a mocked AssetManager which in turn
    // will deliver the given JSON when requesting the "config.json" asset and
    // the given byte array when requesting the "asset://asset.bin" asset.
    private Context getMockedContext(String configJson, byte[] externalAsset) throws Exception {
        Context context = mock(Context.class);
        AssetManager assetManager = mock(AssetManager.class);
        doReturn(assetManager).when(context).getAssets();
        doReturn(new ByteArrayInputStream(configJson.getBytes())).when(assetManager).open("config.json");
        doReturn(new ByteArrayInputStream(externalAsset)).when(assetManager).open("asset.bin");
        return context;
    }

    // Returns a mocked context that holds a mocked AssetManager which in turn
    // will deliver the given JSON when requesting the "config.json" asset and
    // throw an exception when requesting the "asset://asset.bin" asset.
    private Context getMockedContext(String configJson, Class<? extends Exception> typeOfError) throws Exception {
        Context context = mock(Context.class);
        AssetManager assetManager = mock(AssetManager.class);
        doReturn(assetManager).when(context).getAssets();
        doReturn(new ByteArrayInputStream(configJson.getBytes())).when(assetManager).open("config.json");
        doThrow(typeOfError).when(assetManager).open("asset://asset.bin");
        return context;
    }

    // Returns a mocked context that holds a mocked AssetManager which in turn
    // will throw an exception when requesting the "config.json" asset.
    private Context getMockedContext(Class<? extends Exception> typeOfError) throws Exception {
        Context context = mock(Context.class);
        AssetManager assetManager = mock(AssetManager.class);
        doReturn(assetManager).when(context).getAssets();
        doThrow(typeOfError).when(assetManager).open("config.json");
        return context;
    }

    @Test
    public void asset_intactExternalAssetIsDelivered() throws Exception {
        Context context = getMockedContext("{requests:[{url:'/one', method:'get', " +
                "responses:[{responseCode:{code: 200, name: 'OK'}, mime: 'application/octet-stream', " +
                "asset:'asset://asset.bin'}]}]}", new byte[]{1, 2, 3});
        Atlantis target = Atlantis.start();
        InputStream inputStream = null;

        try {
            target.setConfiguration(context, "config.json", null, null);
            inputStream = new URL(AUTHORITY + "/one").openConnection().getInputStream();

            byte[] bytes = ByteStreams.toByteArray(inputStream);
            assertThat(bytes.length, is(3));
            assertThat(bytes[0], is((byte) 1));
            assertThat(bytes[1], is((byte) 2));
            assertThat(bytes[2], is((byte) 3));
        } finally {
            target.stop();
            if (inputStream != null)
                inputStream.close();
        }
    }

    @Test
    public void asset_internalErrorResponseWhenAssetCanNotBeRead() throws Exception {
        Context context = getMockedContext("{requests:[{url:'/one', method:'get', " +
                "responses:[{responseCode:{code: 200, name: 'OK'}, mime: 'application/octet-stream', " +
                "asset:'asset://asset.bin'}]}]}", IOException.class);
        Atlantis target = Atlantis.start();

        try {
            target.setConfiguration(context, "config.json", null, null);

            HttpURLConnection connection = (HttpURLConnection) new URL(AUTHORITY + "/one").openConnection();
            assertThat(connection.getResponseCode(), is(500));
        } finally {
            target.stop();
        }
    }

    @Test
    public void callbacks_successCallbackCalledEventually() throws Exception {
        Context context = getMockedContext("{requests:[{url:'/one', method:'get'}]}");
        Atlantis.OnErrorListener errorCallback = mock(Atlantis.OnErrorListener.class);
        Atlantis.OnSuccessListener successCallback = mock(Atlantis.OnSuccessListener.class);
        Atlantis target = Atlantis.start();

        try {
            target.setConfiguration(context, "config.json", successCallback, errorCallback);
            verifyZeroInteractions(errorCallback);
            verify(successCallback).onSuccess();
        } finally {
            target.stop();
        }
    }

    @Test
    public void callbacks_errorCallbackCalledWhenAssetNotReadable() throws Exception {
        Context context = getMockedContext(IOException.class);
        Atlantis.OnSuccessListener successCallback = mock(Atlantis.OnSuccessListener.class);
        Atlantis.OnErrorListener errorCallback = mock(Atlantis.OnErrorListener.class);
        Atlantis target = Atlantis.start();

        try {
            target.setConfiguration(context, "config.json", successCallback, errorCallback);
            verifyZeroInteractions(successCallback);
            verify(errorCallback).onError(any(IOException.class));
        } finally {
            target.stop();
        }
    }

    @Test
    public void capture_requestsCapturedWhenInCapturingMode() throws Exception {
        Context context = getMockedContext("{requests:[{url:'/one', method:'get'}]}");
        Atlantis target = Atlantis.start();

        try {
            target.setConfiguration(context, "config.json", null, null);

            ((HttpURLConnection) new URL(AUTHORITY + "/one").openConnection()).getResponseCode();
            assertThat(target.getCapturedRequests().size(), is(0));

            target.startCapturing();

            ((HttpURLConnection) new URL(AUTHORITY + "/one").openConnection()).getResponseCode();
            assertThat(target.getCapturedRequests().size(), is(1));
        } finally {
            target.stop();
        }
    }

    @Test
    public void capture_requestsCapturedInCorrectOrder() throws Exception {
        Context context = getMockedContext("{requests:[{url:'/one', method:'get'},{url:'/two', method:'get'}]}");
        Atlantis target = Atlantis.start();

        try {
            target.setConfiguration(context, "config.json", null, null);
            target.startCapturing();

            ((HttpURLConnection) new URL(AUTHORITY + "/one").openConnection()).getResponseCode();
            ((HttpURLConnection) new URL(AUTHORITY + "/two").openConnection()).getResponseCode();

            target.stopCapturing();

            Stack<Request> capturedRequests = target.getCapturedRequests();
            assertThat(capturedRequests.size(), is(2));
            assertThat(capturedRequests.get(0).url(), is("/one"));
            assertThat(capturedRequests.get(1).url(), is("/two"));
        } finally {
            target.stop();
        }
    }

    @Test
    public void capture_alsoCapturesUnmappedRequests() throws Exception {
        Context context = getMockedContext("{requests:[]}");
        Atlantis target = Atlantis.start();

        try {
            target.setConfiguration(context, "config.json", null, null);
            target.startCapturing();

            ((HttpURLConnection) new URL(AUTHORITY + "/unmapped/request").openConnection()).getResponseCode();

            target.stopCapturing();

            Stack<Request> capturedRequests = target.getCapturedRequests();
            assertThat(capturedRequests.size(), is(1));
            assertThat(capturedRequests.get(0).url(), is("/unmapped/request"));
            assertThat(capturedRequests.get(0).method(), either(is("get")).or(is("GET")));
        } finally {
            target.stop();
        }
    }

    @Test
    public void capture_captureHistoryStackResetOnStartCapture() throws Exception {
        Context context = getMockedContext("{requests:[{url:'/one', method:'get'},{url:'/two', method:'get'}]}");
        Atlantis target = Atlantis.start();

        try {
            target.setConfiguration(context, "config.json", null, null);
            target.startCapturing();

            ((HttpURLConnection) new URL(AUTHORITY + "/one").openConnection()).getResponseCode();
            ((HttpURLConnection) new URL(AUTHORITY + "/two").openConnection()).getResponseCode();

            target.stopCapturing();
            assertThat(target.getCapturedRequests().size(), is(2));

            target.startCapturing();
            assertThat(target.getCapturedRequests().size(), is(0));
        } finally {
            target.stop();
        }
    }

    @Test
    public void capture_clearCapturedHistoryStackWorks() throws Exception {
        Context context = getMockedContext("{requests:[{url:'/one', method:'get'}]}");
        Atlantis target = Atlantis.start();

        try {
            target.setConfiguration(context, "config.json", null, null);
            target.startCapturing();

            ((HttpURLConnection) new URL(AUTHORITY + "/one").openConnection()).getResponseCode();

            target.stopCapturing();
            assertThat(target.getCapturedRequests().size(), is(1));

            target.clearCapturedRequests();
            assertThat(target.getCapturedRequests().size(), is(0));
        } finally {
            target.stop();
        }
    }

    @Test
    public void configuration_canSetAssetConfiguration() throws Exception {
        Context context = getMockedContext("{requests:[{url:'/one', method:'get', " +
                "responses:[{responseCode:{code: 200, name: 'OK'}, mime:'application/json', " +
                "text:'{}'}]}]}");
        Atlantis target = Atlantis.start();

        try {
            target.setConfiguration(context, "config.json", null, null);
            HttpURLConnection connection = (HttpURLConnection) new URL(AUTHORITY + "/one").openConnection();
            assertThat(connection.getResponseCode(), is(200));
        } finally {
            target.stop();
        }
    }

    @Test
    public void configuration_canSetBuiltConfiguration() throws Exception {
        Context context = mock(Context.class);
        Configuration configuration = new Configuration.Builder()
                .withRequest(new Request.Builder()
                        .withUrl("/one")
                        .withMethod("get")
                        .withResponse(new Response.Builder()
                                .withStatus(200, "OK")
                                .withContent("{}")
                                .build())
                        .build())
                .build();
        Atlantis target = Atlantis.start();

        try {
            target.setConfiguration(context, configuration);
            HttpURLConnection connection = (HttpURLConnection) new URL(AUTHORITY + "/one").openConnection();
            assertThat(connection.getResponseCode(), is(200));
        } finally {
            target.stop();
        }
    }

    @Test
    public void configuration_canSetFileConfiguration() throws Exception {
        File file = new File("test.json");
        Context context = getMockedContext("{requests:[{url:'/one', method:'get', " +
                "responses:[{responseCode:{code: 200, name: 'OK'}, mime:'application/json', " +
                "text:'{}'}]}]}", file);
        Atlantis target = Atlantis.start();

        try {
            target.setConfiguration(context, file, null, null);
            HttpURLConnection connection = (HttpURLConnection) new URL(AUTHORITY + "/one").openConnection();
            assertThat(connection.getResponseCode(), is(200));
        } finally {
            target.stop();
            file.delete();
        }
    }

    @Test
    public void configuration_canStartWithAssetConfiguration() throws Exception {
        Context context = getMockedContext("{requests:[{url:'/one', method:'get', " +
                "responses:[{responseCode:{code: 200, name: 'OK'}, mime:'application/json', " +
                "text:'{}'}]}]}");
        Atlantis.OnErrorListener errorCallback = mock(Atlantis.OnErrorListener.class);
        Atlantis.OnSuccessListener successCallback = mock(Atlantis.OnSuccessListener.class);
        Atlantis target = Atlantis.start(context, "config.json", successCallback, errorCallback);

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(AUTHORITY + "/one").openConnection();
            verifyZeroInteractions(errorCallback);
            verify(successCallback).onSuccess();
            assertThat(connection.getResponseCode(), is(200));
        } finally {
            target.stop();
        }
    }

    @Test
    public void configuration_canStartWithBuildConfiguration() throws Exception {
        Context context = mock(Context.class);
        Configuration configuration = new Configuration.Builder()
                .withRequest(new Request.Builder()
                        .withUrl("/one")
                        .withMethod("get")
                        .withResponse(new Response.Builder()
                                .withStatus(200, "OK")
                                .withContent("{}")
                                .build())
                        .build())
                .build();
        Atlantis.OnErrorListener errorCallback = mock(Atlantis.OnErrorListener.class);
        Atlantis.OnSuccessListener successCallback = mock(Atlantis.OnSuccessListener.class);
        Atlantis target = Atlantis.start(context, configuration, successCallback, errorCallback);

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(AUTHORITY + "/one").openConnection();
            verifyZeroInteractions(errorCallback);
            verify(successCallback).onSuccess();
            assertThat(connection.getResponseCode(), is(200));
        } finally {
            target.stop();
        }
    }

    @Test
    public void configuration_canStartWithFileConfiguration() throws Exception {
        File file = new File("test.json");
        Context context = getMockedContext("{requests:[{url:'/one', method:'get', " +
                "responses:[{responseCode:{code: 200, name: 'OK'}, mime:'application/json', " +
                "text:'{}'}]}]}", file);

        Atlantis.OnErrorListener errorCallback = mock(Atlantis.OnErrorListener.class);
        Atlantis.OnSuccessListener successCallback = mock(Atlantis.OnSuccessListener.class);
        Atlantis target = Atlantis.start(context, file, successCallback, errorCallback);

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(AUTHORITY + "/one").openConnection();
            verifyZeroInteractions(errorCallback);
            verify(successCallback).onSuccess();
            assertThat(connection.getResponseCode(), is(200));
        } finally {
            target.stop();
            file.delete();
        }
    }

    // This test isn't very meaningful as Atlantis will guarantee to block for
    // a certain amount of time, but the overhead time from other tasks (like
    // reading assets, sending events through the socket, etc) is not accounted
    // for, hence the actual time between the points where the request was sent
    // and the response was received may very well exceed the stated delay time.
    // For now we're tolerant about this, but maybe it would make more sense to
    // just remove this test.
    @Test
    public void delay_responseIsDelayedExactTime() throws Exception {
        Context context = getMockedContext("{requests:[{url:'/one', method:'get', " +
                "responses:[{responseCode:{code: 200, name: 'OK'}, mime:'application/json', " +
                "text:'{}', delay: 20}]}]}");
        Atlantis target = Atlantis.start();

        try {
            target.setConfiguration(context, "config.json", null, null);
            long time = System.currentTimeMillis();

            ((HttpURLConnection) new URL(AUTHORITY + "/one").openConnection()).getResponseCode();

            time = System.currentTimeMillis() - time;
            assertThat(time, is(greaterThanOrEqualTo(20L)));
        } finally {
            target.stop();
        }
    }

    // Same as delay_responseIsDelayedExactTime()
    @Test
    public void delay_responseIsDelayedRandomTime() throws Exception {
        Context context = getMockedContext("{requests:[{url:'/one', method:'get', " +
                "responses:[{responseCode:{code: 200, name: 'OK'}, mime:'application/json', " +
                "text:'{}', delay: 20, maxDelay: 40}]}]}");
        Atlantis target = Atlantis.start();

        try {
            target.setConfiguration(context, "config.json", null, null);
            long time = System.currentTimeMillis();

            ((HttpURLConnection) new URL(AUTHORITY + "/one").openConnection()).getResponseCode();

            time = System.currentTimeMillis() - time;
            assertThat(time, is(greaterThanOrEqualTo(20L)));
        } finally {
            target.stop();
        }
    }

    @Test
    public void filter_canParseRequestFilterFromAssetConfiguration() throws Exception {
        Context context = getMockedContext("{requestFilter:'com.echsylon.atlantis.filter.DefaultRequestFilter', requests:[]}");
        Atlantis.OnErrorListener errorCallback = mock(Atlantis.OnErrorListener.class);
        Atlantis.OnSuccessListener successCallback = mock(Atlantis.OnSuccessListener.class);
        Atlantis target = Atlantis.start();

        try {
            target.setConfiguration(context, "config.json", successCallback, errorCallback);
            verifyZeroInteractions(errorCallback);
            verify(successCallback).onSuccess();
        } finally {
            target.stop();
        }
    }

    @Test
    public void filter_canParseResponseFilterFromAssetConfiguration() throws Exception {
        Context context = getMockedContext("{requests:[{url:'/one', method:'get'," +
                "  responseFilter: 'com.echsylon.atlantis.filter.DefaultResponseFilter', " +
                "  responses:[]}]}");
        Atlantis.OnErrorListener errorCallback = mock(Atlantis.OnErrorListener.class);
        Atlantis.OnSuccessListener successCallback = mock(Atlantis.OnSuccessListener.class);
        Atlantis target = Atlantis.start();

        try {
            target.setConfiguration(context, "config.json", successCallback, errorCallback);
            verifyZeroInteractions(errorCallback);
            verify(successCallback).onSuccess();
        } finally {
            target.stop();
        }
    }

    @Test
    public void filter_degradesGracefullyWhenCanNotParseRequestFilterFromAssetConfiguration() throws Exception {
        Context context = getMockedContext("{requestFilter:'unknownRequestFilterName', requests:[]}");
        Atlantis.OnErrorListener errorCallback = mock(Atlantis.OnErrorListener.class);
        Atlantis.OnSuccessListener successCallback = mock(Atlantis.OnSuccessListener.class);
        Atlantis target = Atlantis.start();

        try {
            target.setConfiguration(context, "config.json", successCallback, errorCallback);
            verifyZeroInteractions(successCallback);
            verify(errorCallback).onError(any(JsonParser.JsonException.class));
        } finally {
            target.stop();
        }
    }


    @Test
    public void filter_degradesGracefullyWhenCanNotParseResponseFilterFromAssetConfiguration() throws Exception {
        Context context = getMockedContext("{requests:[{url:'/one', method:'get'," +
                "  responseFilter: 'unknownResponseFilter', responses:[]}]}");
        Atlantis.OnErrorListener errorCallback = mock(Atlantis.OnErrorListener.class);
        Atlantis.OnSuccessListener successCallback = mock(Atlantis.OnSuccessListener.class);
        Atlantis target = Atlantis.start();

        try {
            target.setConfiguration(context, "config.json", successCallback, errorCallback);
            verifyZeroInteractions(successCallback);
            verify(errorCallback).onError(any(JsonParser.JsonException.class));
        } finally {
            target.stop();
        }
    }

    @Test
    public void response_returnsNotFoundResponseWhenNoMatchingConfigurationFound() throws Exception {
        Context context = getMockedContext("{requests:[{url:'/one', method:'get'}]}");
        Atlantis target = Atlantis.start();

        try {
            target.setConfiguration(context, "config.json", null, null);
            HttpURLConnection connection = (HttpURLConnection) new URL(AUTHORITY + "/ten").openConnection();
            assertThat(connection.getResponseCode(), is(404));
        } finally {
            target.stop();
        }
    }

    @Test
    public void response_returnsCorrectMockResponseForUrlWithQuery() throws Exception {
        Context context = getMockedContext("{requests:[{url:'/one?q=1', method:'get', " +
                "responses:[{responseCode:{code: 2, name: 'CUSTOM_OK'}, mime:'application/json', " +
                "text:'{}'}]}]}");
        Atlantis target = Atlantis.start();

        try {
            target.setConfiguration(context, "config.json", null, null);
            HttpURLConnection connection = (HttpURLConnection) new URL(AUTHORITY + "/one?q=1").openConnection();
            assertThat(connection.getResponseCode(), is(2));
            assertThat(connection.getResponseMessage(), is("CUSTOM_OK"));
        } finally {
            target.stop();
        }
    }

    @Test
    public void response_returnsCorrectMockedResponseHeaders() throws Exception {
        Context context = getMockedContext("{requests:[{url:'/one?q=1', method:'get', " +
                "responses:[{responseCode:{code: 2, name: 'CUSTOM_OK'}, mime:'application/json', " +
                "headers: { key1: 'value1', key2: 'value2' }, text:'{}'}]}]}");
        Atlantis target = Atlantis.start();

        try {
            target.setConfiguration(context, "config.json", null, null);
            HttpURLConnection connection = (HttpURLConnection) new URL(AUTHORITY + "/one?q=1").openConnection();
            assertThat(connection.getHeaderField("key1"), is("value1"));
            assertThat(connection.getHeaderField("key2"), is("value2"));
        } finally {
            target.stop();
        }
    }

}
