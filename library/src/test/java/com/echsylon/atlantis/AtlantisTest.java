package com.echsylon.atlantis;

import android.content.Context;
import android.content.res.AssetManager;

import com.echsylon.atlantis.template.Configuration;
import com.echsylon.atlantis.template.Request;
import com.echsylon.atlantis.template.Response;
import com.google.common.io.ByteStreams;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Stack;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 16)
public class AtlantisTest {
    private static final String AUTHORITY = "http://localhost:8080";

    // Returns a mocked context that holds a mocked AssetManager which in turn will deliver the
    // given JSON when requesting the "config.json" asset.
    private Context getMockedContext(String configJson) throws Exception {
        Context context = mock(Context.class);
        AssetManager assetManager = mock(AssetManager.class);
        doReturn(assetManager).when(context).getAssets();
        doReturn(new ByteArrayInputStream(configJson.getBytes())).when(assetManager).open("config.json");
        return context;
    }

    // Returns a mocked context that holds a mocked AssetManager which in turn will deliver the
    // given JSON when requesting the "config.json" asset and the given byte array when requesting
    // the "asset://asset.bin" asset.
    private Context getMockedContext(String configJson, byte[] externalAsset) throws Exception {
        Context context = mock(Context.class);
        AssetManager assetManager = mock(AssetManager.class);
        doReturn(assetManager).when(context).getAssets();
        doReturn(new ByteArrayInputStream(configJson.getBytes())).when(assetManager).open("config.json");
        doReturn(new ByteArrayInputStream(externalAsset)).when(assetManager).open("asset://asset.bin");
        return context;
    }

    // Returns a mocked context that holds a mocked AssetManager which in turn will deliver the
    // given JSON when requesting the "config.json" asset and throw an exception when requesting
    // the "asset://asset.bin" asset.
    private Context getMockedContext(String configJson, Class<? extends Exception> typeOfError) throws Exception {
        Context context = mock(Context.class);
        AssetManager assetManager = mock(AssetManager.class);
        doReturn(assetManager).when(context).getAssets();
        doReturn(new ByteArrayInputStream(configJson.getBytes())).when(assetManager).open("config.json");
        doThrow(typeOfError).when(assetManager).open("asset://asset.bin");
        return context;
    }

    // Returns a mocked context that holds a mocked AssetManager which in turn will throw an
    // exception of the given type when requesting the "config.json" asset.
    private Context getMockedContext(Class<? extends Exception> typeOfError) throws Exception {
        Context context = mock(Context.class);
        AssetManager assetManager = mock(AssetManager.class);
        doReturn(assetManager).when(context).getAssets();
        doThrow(typeOfError).when(assetManager).open("config.json");
        return context;
    }

    @Test
    public void configuration_canSetAssetConfiguration() throws Exception {
        Context context = getMockedContext("{requests:[{url:'/one', method:'get', " +
                "responses:[{responseCode:{code: 200, name: 'OK'}, mime:'application/json', " +
                "text:'{}'}]}]}");
        Atlantis target = null;

        try {
            target = Atlantis.start(null, null);
            target.setConfiguration(context, "config.json", null, null);

            HttpURLConnection connection = (HttpURLConnection) new URL(AUTHORITY + "/one").openConnection();
            assertThat(connection.getResponseCode(), is(200));
        } finally {
            if (target != null)
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
                                .withContent("{}")));
        Atlantis target = null;

        try {
            target = Atlantis.start(null, null);
            target.setConfiguration(context, configuration);

            HttpURLConnection connection = (HttpURLConnection) new URL(AUTHORITY + "/one").openConnection();
            assertThat(connection.getResponseCode(), is(200));
        } finally {
            if (target != null)
                target.stop();
        }
    }

    @Test
    public void callbacks_successCallbackCalledEventually() throws Exception {
        Context context = getMockedContext("{requests:[{url:'/one', method:'get'}]}");
        Atlantis.OnErrorListener errorListener = mock(Atlantis.OnErrorListener.class);
        Atlantis.OnSuccessListener successListener = mock(Atlantis.OnSuccessListener.class);
        Atlantis target = null;

        try {
            target = Atlantis.start(null, null);
            target.setConfiguration(context, "config.json", successListener, errorListener);

            verifyZeroInteractions(errorListener);
            verify(successListener).onSuccess();
        } finally {
            if (target != null)
                target.stop();
        }
    }

    @Test
    public void callbacks_errorCallbackCalledWhenAssetNotReadable() throws Exception {
        Context context = getMockedContext(IOException.class);
        Atlantis.OnSuccessListener successCallback = mock(Atlantis.OnSuccessListener.class);
        Atlantis.OnErrorListener errorCallback = mock(Atlantis.OnErrorListener.class);
        Atlantis target = null;

        try {
            target = Atlantis.start(null, null);
            target.setConfiguration(context, "config.json", successCallback, errorCallback);

            verifyZeroInteractions(successCallback);
            verify(errorCallback).onError(any(IOException.class));
        } finally {
            if (target != null)
                target.stop();
        }
    }

    @Test
    public void response_returnsNotFoundResponseWhenNoMatchingConfigurationFound() throws Exception {
        Context context = getMockedContext("{requests:[{url:'/one', method:'get'}]}");
        Atlantis target = null;

        try {
            target = Atlantis.start(null, null);
            target.setConfiguration(context, "config.json", null, null);

            HttpURLConnection connection = (HttpURLConnection) new URL(AUTHORITY + "/ten").openConnection();
            assertThat(connection.getResponseCode(), is(404));
        } finally {
            if (target != null)
                target.stop();
        }
    }

    @Test
    public void capture_requestsCapturedWhenInCapturingMode() throws Exception {
        Context context = getMockedContext("{requests:[{url:'/one', method:'get'}]}");
        Atlantis target = null;

        try {
            target = Atlantis.start(null, null);
            target.setConfiguration(context, "config.json", null, null);

            ((HttpURLConnection) new URL(AUTHORITY + "/one").openConnection()).getResponseCode();
            assertThat(target.getCapturedRequests().size(), is(0));

            Thread.sleep(10);

            target.startCapturing();
            ((HttpURLConnection) new URL(AUTHORITY + "/one").openConnection()).getResponseCode();
            assertThat(target.getCapturedRequests().size(), is(1));
        } finally {
            if (target != null)
                target.stop();
        }
    }

    @Test
    public void capture_requestsCapturedInCorrectOrder() throws Exception {
        Context context = getMockedContext("{requests:[{url:'/one', method:'get'},{url:'/two', method:'get'}]}");
        Atlantis target = null;

        try {
            target = Atlantis.start(null, null);
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
            if (target != null)
                target.stop();
        }
    }

    @Test
    public void capture_captureHistoryStackResetOnStartCapture() throws Exception {
        Context context = getMockedContext("{requests:[{url:'/one', method:'get'},{url:'/two', method:'get'}]}");
        Atlantis target = null;

        try {
            target = Atlantis.start(null, null);
            target.setConfiguration(context, "config.json", null, null);

            target.startCapturing();
            ((HttpURLConnection) new URL(AUTHORITY + "/one").openConnection()).getResponseCode();
            ((HttpURLConnection) new URL(AUTHORITY + "/two").openConnection()).getResponseCode();
            target.stopCapturing();
            assertThat(target.getCapturedRequests().size(), is(2));

            target.startCapturing();
            assertThat(target.getCapturedRequests().size(), is(0));
        } finally {
            if (target != null)
                target.stop();
        }
    }

    @Test
    public void capture_clearCapturedHistoryStackWorks() throws Exception {
        Context context = getMockedContext("{requests:[{url:'/one', method:'get'}]}");
        Atlantis target = null;

        try {
            target = Atlantis.start(null, null);
            target.setConfiguration(context, "config.json", null, null);

            target.startCapturing();
            ((HttpURLConnection) new URL(AUTHORITY + "/one").openConnection()).getResponseCode();
            target.stopCapturing();
            assertThat(target.getCapturedRequests().size(), is(1));

            target.clearCapturedRequests();
            assertThat(target.getCapturedRequests().size(), is(0));
        } finally {
            if (target != null)
                target.stop();
        }
    }

    // This test isn't very meaningful as Atlantis will guarantee to block for a certain amount of
    // time, but the overhead time from other tasks (like reading assets, sending events through the
    // socket, etc) is not accounted for, hence the actual time between the points where the request
    // was sent and the response was received, may very well exceed the stated delay time. For now
    // we're tolerant about this, but maybe it would make more sense to just remove this test.
    @Test
    public void delay_responseIsDelayedExactTime() throws Exception {
        Context context = getMockedContext("{requests:[{url:'/one', method:'get', " +
                "responses:[{responseCode:{code: 200, name: 'OK'}, mime:'application/json', " +
                "text:'{}', delay: 20}]}]}");
        Atlantis target = null;

        try {
            target = Atlantis.start(null, null);
            target.setConfiguration(context, "config.json", null, null);

            long time = System.currentTimeMillis();
            ((HttpURLConnection) new URL(AUTHORITY + "/one").openConnection()).getResponseCode();
            time = System.currentTimeMillis() - time;
            assertThat(time, is(greaterThanOrEqualTo(20L)));
        } finally {
            if (target != null)
                target.stop();
        }
    }

    // This test isn't very meaningful as Atlantis will guarantee to block for a certain amount of
    // time, but the overhead time from other tasks (like reading assets, sending events through the
    // socket, etc) is not accounted for, hence the actual time between the points where the request
    // was sent and the response was received, may very well exceed the stated delay time. Therefore
    // we can not test the upper bound, and maybe it would make more sense to just remove this test.
    @Test
    public void delay_responseIsDelayedRandomTime() throws Exception {
        Context context = getMockedContext("{requests:[{url:'/one', method:'get', " +
                "responses:[{responseCode:{code: 200, name: 'OK'}, mime:'application/json', " +
                "text:'{}', delay: 20, maxDelay: 40}]}]}");
        Atlantis target = null;

        try {
            target = Atlantis.start(null, null);
            target.setConfiguration(context, "config.json", null, null);

            long time = System.currentTimeMillis();
            ((HttpURLConnection) new URL(AUTHORITY + "/one").openConnection()).getResponseCode();
            time = System.currentTimeMillis() - time;
            assertThat(time, is(greaterThanOrEqualTo(20L)));
        } finally {
            if (target != null)
                target.stop();
        }
    }

    @Test
    public void asset_intactExternalAssetIsDelivered() throws Exception {
        Context context = getMockedContext("{requests:[{url:'/one', method:'get', " +
                "responses:[{responseCode:{code: 200, name: 'OK'}, mime: 'application/octet-stream', " +
                "asset:'asset://asset.bin'}]}]}", new byte[]{1, 2, 3});
        Atlantis target = null;
        InputStream inputStream = null;

        try {
            target = Atlantis.start(null, null);
            target.setConfiguration(context, "config.json", null, null);

            inputStream = new URL(AUTHORITY + "/one").openConnection().getInputStream();
            byte[] bytes = ByteStreams.toByteArray(inputStream);
            assertThat(bytes.length, is(3));
            assertThat(bytes[0], is((byte) 1));
            assertThat(bytes[1], is((byte) 2));
            assertThat(bytes[2], is((byte) 3));
        } finally {
            if (target != null)
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
        Atlantis target = null;

        try {
            target = Atlantis.start(null, null);
            target.setConfiguration(context, "config.json", null, null);

            HttpURLConnection connection = (HttpURLConnection) new URL(AUTHORITY + "/one").openConnection();
            assertThat(connection.getResponseCode(), is(500));
        } finally {
            if (target != null)
                target.stop();
        }
    }

}
