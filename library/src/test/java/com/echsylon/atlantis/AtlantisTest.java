package com.echsylon.atlantis;

import android.content.Context;
import android.content.res.AssetManager;

import com.echsylon.atlantis.template.Request;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Stack;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 16)
public class AtlantisTest {
    private static final String AUTHORITY = "http://localhost:8080";
    private Atlantis target;
    private Atlantis.OnErrorListener mockedErrorListener;
    private Atlantis.OnSuccessListener mockedSuccessListener;

    private Context mockedContext;
    private AssetManager mockedAssetManager;

    @Before
    public void setup() throws Exception {
        String json = "{\"requests\":[{\"url\":\"http://test.com/one\",\"method\":\"GET\"},{\"url\":\"http://test.com/two\",\"method\":\"GET\"}]}";

        mockedContext = mock(Context.class);
        mockedAssetManager = mock(AssetManager.class);
        mockedErrorListener = mock(Atlantis.OnErrorListener.class);
        mockedSuccessListener = mock(Atlantis.OnSuccessListener.class);

        doReturn(mockedAssetManager).when(mockedContext).getAssets();
        doReturn(new ByteArrayInputStream(json.getBytes())).when(mockedAssetManager).open(anyString());

        target = Atlantis.start(mockedContext, "requests.json", mockedSuccessListener, mockedErrorListener);
    }

    @After
    public void tearDown() throws Exception {
        target.stopCapturing();
        target.stop();
        target = null;
        mockedContext = null;
        mockedAssetManager = null;
        mockedErrorListener = null;
        mockedSuccessListener = null;
    }

    @Test
    public void callbacks_SuccessCallbackCalledEventually() throws Exception {
        verifyZeroInteractions(mockedErrorListener);
        verify(mockedSuccessListener).onSuccess();
    }

    @Test
    public void callbacks_ErrorCallbackCalledWhenAssetNotReadable() throws Exception {
        Context context = mock(Context.class);
        AssetManager assetManager = mock(AssetManager.class);
        Atlantis.OnSuccessListener successCallback = mock(Atlantis.OnSuccessListener.class);
        Atlantis.OnErrorListener errorCallback = mock(Atlantis.OnErrorListener.class);

        doReturn(assetManager).when(context).getAssets();
        doThrow(IOException.class).when(assetManager).open(anyString());

        Atlantis.start(context, "", successCallback, errorCallback);
        verifyZeroInteractions(successCallback);
        verify(errorCallback).onError(any(Throwable.class));
    }

    @Test
    public void capture_RequestsCapturedWhenInCapturingMode() throws Exception {
        // Make a request and verify nothing has been captured.
        ((HttpURLConnection) new URL(AUTHORITY + "/one").openConnection()).getResponseCode();
        assertThat(target.getCapturedRequests().size(), is(0));

        // Start capturing, make a new request and verify that a request has been captured.
        target.startCapturing();
        ((HttpURLConnection) new URL(AUTHORITY + "/two").openConnection()).getResponseCode();
        assertThat(target.getCapturedRequests().size(), is(1));
    }

    @Test
    public void capture_RequestsCapturedInCorrectOrder() throws Exception {
        target.startCapturing();
        ((HttpURLConnection) new URL(AUTHORITY + "/one").openConnection()).getResponseCode();
        ((HttpURLConnection) new URL(AUTHORITY + "/two").openConnection()).getResponseCode();
        target.stopCapturing();
        Stack<Request> capturedRequests = target.getCapturedRequests();
        assertThat(capturedRequests.size(), is(2));
        assertThat(capturedRequests.get(0).url(), is("http://test.com/one"));
        assertThat(capturedRequests.get(1).url(), is("http://test.com/two"));
    }

    @Test
    public void capture_CaptureHistoryStackResetOnStartCapture() throws Exception {
        target.startCapturing();
        ((HttpURLConnection) new URL(AUTHORITY + "/one").openConnection()).getResponseCode();
        ((HttpURLConnection) new URL(AUTHORITY + "/two").openConnection()).getResponseCode();
        target.stopCapturing();
        assertThat(target.getCapturedRequests().size(), is(2));

        target.startCapturing();
        assertThat(target.getCapturedRequests().size(), is(0));
    }

    @Test
    public void capture_ClearCapturedHistoryStackWorks() throws Exception {
        target.startCapturing();
        ((HttpURLConnection) new URL(AUTHORITY + "/one").openConnection()).getResponseCode();
        target.stopCapturing();
        assertThat(target.getCapturedRequests().size(), is(1));

        target.clearCapturedRequests();
        assertThat(target.getCapturedRequests().size(), is(0));
    }
}
