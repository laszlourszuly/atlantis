package com.echsylon.atlantis;

import android.content.Context;
import android.content.res.AssetManager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * Verifies expected behavior on the {@link Response} class.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 16)
public class ResponseTest {

    @Test
    public void create_canBuildEmptyResponse() throws Exception {
        Context context = mock(Context.class);
        Response response = new Response.Builder();
        assertThat(response, is(notNullValue()));
        assertThat(response, is(instanceOf(Response.class)));
        assertThat(response.statusCode(), is(0));
        assertThat(response.statusName(), is(nullValue()));
        assertThat(response.mimeType(), is(nullValue()));
        assertThat(response.hasContent(), is(false));
        assertThat(response.content(), is(nullValue()));
        assertThat(response.hasAsset(), is(false));

        byte[] asset = response.asset(context);
        assertThat(asset, is(notNullValue()));
        assertThat(asset.length, is(0));

        assertThat(response.delay(), is(0L));
        assertThat(response.headers(), is(notNullValue()));
        assertThat(response.headers(), is(Collections.EMPTY_MAP));
    }

    @Test
    public void create_canBuildFullResponse() throws Exception {
        AssetManager assetManager = mock(AssetManager.class);
        doReturn(new ByteArrayInputStream(new byte[]{0, 1, 2})).when(assetManager).open("asset://hejhopp.bin");

        Context context = mock(Context.class);
        doReturn(assetManager).when(context).getAssets();

        Response response = new Response.Builder()
                .withHeader("h1", "v1")
                .withStatus(200, "OK")
                .withMimeType("application/json")
                .withContent("{}")
                .withAsset("hejhopp.bin")
                .withDelay(12);
        assertThat(response.headers().get("h1"), is("v1"));
        assertThat(response.headers().get("invalid"), is(nullValue()));
        assertThat(response.statusCode(), is(200));
        assertThat(response.statusName(), is("OK"));
        assertThat(response.mimeType(), is("application/json"));
        assertThat(response.hasContent(), is(true));
        assertThat(response.content(), is("{}"));

        byte[] asset = response.asset(context);
        assertThat(response.hasAsset(), is(true));
        assertThat(asset, is(notNullValue()));
        assertThat(asset.length, is(3));
        assertThat(asset[0], is((byte) 0));
        assertThat(asset[1], is((byte) 1));
        assertThat(asset[2], is((byte) 2));

        assertThat(response.delay(), is(12L));
    }

    @Test
    public void create_canBuildResponseWithHeadersMap() throws Exception {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("h1", "v1");
        headers.put("h2", "v2");

        Response response = new Response.Builder()
                .withHeaders(headers);
        assertThat(response.headers().size(), is(2));
        assertThat(response.headers().get("h1"), is("v1"));
        assertThat(response.headers().get("h2"), is("v2"));
        assertThat(response.headers().get("invalid"), is(nullValue()));
    }

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
