package com.echsylon.atlantis;

import android.content.Context;
import android.content.res.AssetManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.IOException;

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

    @Test
    public void verifySuccessCallbackCalledEventually() throws Exception {
        Context mockedContext = mock(Context.class);
        AssetManager mockedAssetManager = mock(AssetManager.class);
        Atlantis.OnSuccessListener mockedSuccessListener = mock(Atlantis.OnSuccessListener.class);
        Atlantis.OnErrorListener mockedErrorListener = mock(Atlantis.OnErrorListener.class);

        doReturn(mockedAssetManager).when(mockedContext).getAssets();
        doReturn(new ByteArrayInputStream("{}".getBytes())).when(mockedAssetManager).open(anyString());

        Atlantis.start(mockedContext, "localhost", 8080, "any.json", mockedSuccessListener, mockedErrorListener);
        verifyZeroInteractions(mockedErrorListener);
        verify(mockedSuccessListener).onSuccess();
    }

    @Test
    public void verifyErrorCallbackCalledWhenAssetNotReadable() throws Exception {
        Context mockedContext = mock(Context.class);
        AssetManager mockedAssetManager = mock(AssetManager.class);
        Atlantis.OnSuccessListener mockedSuccessListener = mock(Atlantis.OnSuccessListener.class);
        Atlantis.OnErrorListener mockedErrorListener = mock(Atlantis.OnErrorListener.class);

        doReturn(mockedAssetManager).when(mockedContext).getAssets();
        doThrow(IOException.class).when(mockedAssetManager).open(anyString());

        Atlantis.start(mockedContext, "localhost", 8080, "any.json", mockedSuccessListener, mockedErrorListener);
        verifyZeroInteractions(mockedSuccessListener);
        verify(mockedErrorListener).onError(any(Throwable.class));
    }
}
