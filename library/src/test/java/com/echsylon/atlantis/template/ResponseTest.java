package com.echsylon.atlantis.template;

import android.content.Context;
import android.content.res.AssetManager;

import com.echsylon.atlantis.BuildConfig;
import com.google.gson.Gson;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * Verifies expected behavior on the {@link Response} class.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 16)
public class ResponseTest {
    private static final Response GOOD_RESPONSE;
    private static final Response BAD_RESPONSE;

    static {
        GOOD_RESPONSE = new Gson().fromJson("{" +
                "  \"responseCode\": {" +
                "    \"code\": 200, " +
                "    \"name\": \"OK\"" +
                "  }, " +
                "  \"mime\": \"application/json\", " +
                "  \"text\": \"\\\"{}\\\"\", " +
                "  \"asset\": \"asset://fake.asset\", " +
                "  \"headers\": [{" +
                "    \"key\": \"k1\", " +
                "    \"value\": \"v1\"" +
                "  }]" +
                "}", Response.class);

        BAD_RESPONSE = new Gson().fromJson("{}",
                Response.class);
    }

    @Test
    public void status_ResponseCode() throws Exception {
        assertThat(GOOD_RESPONSE.statusCode(), is(200));
        assertThat(GOOD_RESPONSE.statusName(), is("OK"));
        assertThat(BAD_RESPONSE.statusCode(), is(0));
        assertThat(BAD_RESPONSE.statusName(), is(nullValue()));
    }

    @Test
    public void header_CorrectHeadersAreReturned() throws Exception {
        assertThat(GOOD_RESPONSE.headers(), notNullValue());
        assertThat(GOOD_RESPONSE.headers().size(), is(1));
        assertThat(GOOD_RESPONSE.headers("k1"), notNullValue());
        assertThat(GOOD_RESPONSE.headers("k1").size(), is(1));
        assertThat(GOOD_RESPONSE.headers("invalid_key"), notNullValue());
        assertThat(GOOD_RESPONSE.headers("invalid_key").size(), is(0));

        assertThat(BAD_RESPONSE.headers(), notNullValue());
        assertThat(BAD_RESPONSE.headers().size(), is(0));
        assertThat(BAD_RESPONSE.headers("invalid_key"), notNullValue());
        assertThat(BAD_RESPONSE.headers("invalid_key").size(), is(0));
    }

    @Test
    public void mime_CorrectMimeTypeIsReturned() throws Exception {
        assertThat(GOOD_RESPONSE.mimeType(), is("application/json"));
        assertThat(BAD_RESPONSE.mimeType(), nullValue());
    }

    @Test
    public void content_CorrectContentTextIsReturned() throws Exception {
        assertThat(GOOD_RESPONSE.hasContent(), is(true));
        assertThat(GOOD_RESPONSE.content(), is("\"{}\""));
        assertThat(BAD_RESPONSE.hasContent(), is(false));
        assertThat(BAD_RESPONSE.content(), nullValue());
    }

    @Test
    public void asset_CorrectAssetIsRead() throws Exception {
        Context mockedContext = mock(Context.class);
        AssetManager mockedAssetManager = mock(AssetManager.class);

        doReturn(mockedAssetManager).when(mockedContext).getAssets();
        doReturn(new ByteArrayInputStream("{}".getBytes())).when(mockedAssetManager).open(anyString());

        assertThat(GOOD_RESPONSE.hasAsset(), is(true));
        byte[] goodAsset = GOOD_RESPONSE.asset(mockedContext);
        assertThat(goodAsset, notNullValue());
        assertThat(goodAsset.length, is(2));

        assertThat(BAD_RESPONSE.hasAsset(), is(false));
        byte[] badAsset = BAD_RESPONSE.asset(mockedContext);
        assertThat(badAsset, notNullValue());
        assertThat(badAsset.length, is(0));
    }

    @Test
    public void asset_ReadingAssetsReturnGracefullyOnError() throws Exception {
        Context mockedContext = mock(Context.class);
        AssetManager mockedAssetManager = mock(AssetManager.class);

        doReturn(mockedAssetManager).when(mockedContext).getAssets();
        doThrow(IOException.class).when(mockedAssetManager).open(anyString());

        byte[] goodAsset = GOOD_RESPONSE.asset(mockedContext);
        assertThat(goodAsset, notNullValue());
        assertThat(goodAsset.length, is(0));
    }
}
