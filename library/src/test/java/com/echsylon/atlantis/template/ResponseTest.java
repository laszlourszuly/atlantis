package com.echsylon.atlantis.template;

import android.content.Context;
import android.content.res.AssetManager;

import com.echsylon.atlantis.BuildConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
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

    @Test
    public void status_responseCode() throws Exception {
        JsonObject responseCode = new JsonObject();
        responseCode.addProperty("code", 200);
        responseCode.addProperty("name", "OK");
        JsonObject jsonObject = new JsonObject();
        jsonObject.add("responseCode", responseCode);

        Response responseWithContent = new Gson().fromJson(jsonObject, Response.class);
        assertThat(responseWithContent.statusCode(), is(200));
        assertThat(responseWithContent.statusName(), is("OK"));

        Response emptyResponse = new Gson().fromJson(new JsonObject(), Response.class);
        assertThat(emptyResponse.statusCode(), is(0));
        assertThat(emptyResponse.statusName(), is(nullValue()));
    }

    @Test
    public void mime_correctMimeTypeIsReturned() throws Exception {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("mime", "application/json");
        Response responseWithContent = new Gson().fromJson(jsonObject, Response.class);
        assertThat(responseWithContent.mimeType(), is("application/json"));

        Response emptyResponse = new Gson().fromJson(new JsonObject(), Response.class);
        assertThat(emptyResponse.mimeType(), is(nullValue()));
    }

    @Test
    public void content_correctContentTextIsReturned() throws Exception {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("text", "\"{}\"");
        Response responseWithContent = new Gson().fromJson(jsonObject, Response.class);
        assertThat(responseWithContent.hasContent(), is(true));
        assertThat(responseWithContent.content(), is("\"{}\""));

        Response emptyResponse = new Gson().fromJson(new JsonObject(), Response.class);
        assertThat(emptyResponse.hasContent(), is(false));
        assertThat(emptyResponse.content(), is(nullValue()));
    }

    @Test
    public void asset_assetIsReadCorrectly() throws Exception {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("asset", "asset://fake.asset");
        Response response = new Gson().fromJson(jsonObject, Response.class);

        AssetManager mockedAssetManager = mock(AssetManager.class);
        doReturn(new ByteArrayInputStream("{}".getBytes())).when(mockedAssetManager).open(anyString());
        Context mockedContext = mock(Context.class);
        doReturn(mockedAssetManager).when(mockedContext).getAssets();

        assertThat(response.hasAsset(), is(true));
        byte[] goodAsset = response.asset(mockedContext);
        assertThat(goodAsset, is(notNullValue()));
        assertThat(goodAsset.length, is(2));
    }

    @Test
    public void asset_readingAssetsDoesNotConsumeErrors() throws Exception {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("asset", "asset://fake.asset");
        Response response = new Gson().fromJson(jsonObject, Response.class);

        AssetManager mockedAssetManager = mock(AssetManager.class);
        doThrow(IOException.class).when(mockedAssetManager).open("asset://fake.asset");
        Context mockedContext = mock(Context.class);
        doReturn(mockedAssetManager).when(mockedContext).getAssets();

        assertThatThrownBy(() -> response.asset(mockedContext))
                .isInstanceOf(IOException.class)
                .hasNoCause();
    }

    @Test
    public void delay_randomDelayBetweenDefaultAndMaxIsReturned() throws Exception {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("delay", 110L);
        jsonObject.addProperty("maxDelay", 130L);
        Response response = new Gson().fromJson(jsonObject, Response.class);
        assertThat(response.delay(), is(both(greaterThanOrEqualTo(100L)).and(lessThanOrEqualTo(130L))));
    }

    @Test
    public void delay_exactDelayIsReturnedIfOnlyDefaultGiven() throws Exception {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("delay", 113L);
        Response response = new Gson().fromJson(jsonObject, Response.class);
        assertThat(response.delay(), is(113L));
    }

    @Test
    public void delay_exceptionThrownIfMaxDelayLessThanDefaultDelay() throws Exception {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("delay", 110L);
        jsonObject.addProperty("maxDelay", 90L);
        Response response = new Gson().fromJson(jsonObject, Response.class);

        assertThatThrownBy(response::delay)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("'maxDelay' mustn't be less than 'delay'")
                .hasNoCause();
    }

}
