package com.echsylon.atlantis;

import android.content.Context;
import android.content.res.AssetManager;

import com.google.common.io.Files;
import com.google.gson.Gson;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * Verifies expected behavior on the {@link Response} class.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 16)
public class ResponseTest {

    @Test
    public void create_canBuildFullResponse() throws Exception {
        AssetManager assetManager = mock(AssetManager.class);
        doReturn(new ByteArrayInputStream(new byte[]{0, 1, 2})).when(assetManager).open("asset://asset");

        Context context = mock(Context.class);
        doReturn(assetManager).when(context).getAssets();

        HashMap<String, String> headers = new HashMap<>();
        headers.put("h1", "v1");
        headers.put("h2", "v2");

        Response response = new Response.Builder()
                .withHeader("h0", "v0")
                .withHeaders(headers)
                .withHeader("h3", "v3")
                .withStatus(2, "CUSTOM_OK")
                .withMimeType("mime")
                .withContent("text")
                .withAsset("asset://asset")
                .withDelay(12);

        assertThat(response.headers().get("h0"), is("v0"));
        assertThat(response.headers().get("h1"), is("v1"));
        assertThat(response.headers().get("h2"), is("v2"));
        assertThat(response.headers().get("h3"), is("v3"));
        assertThat(response.headers().get("invalid"), is(nullValue()));

        assertThat(response.statusCode(), is(2));
        assertThat(response.statusName(), is("CUSTOM_OK"));

        assertThat(response.mimeType(), is("mime"));
        assertThat(response.hasContent(), is(true));
        assertThat(response.content(), is("text"));

        byte[] asset = response.asset(context);
        assertThat(response.hasAsset(), is(true));
        assertThat(asset, is(notNullValue()));
        assertThat(asset.length, is(3));
        assertThat(asset[0], is((byte) 0));
        assertThat(asset[1], is((byte) 1));
        assertThat(asset[2], is((byte) 2));

        assertThat(response.delay(), is(12));
    }

    @Test
    public void create_canParseFullResponse() throws Exception {
        String json = "{ responseCode: { code: 2, name: 'CUSTOM_OK' }, " +
                "headers: { h0: 'v0' }, mime: 'mime', text: 'text', " +
                "asset: 'asset', delay: 1, maxDelay: 1 }";

        Response response = new Gson().fromJson(json, Response.class);
        assertThat(response.statusCode(), is(2));
        assertThat(response.headers().get("h0"), is("v0"));
        assertThat(response.headers().get("invalid"), is(nullValue()));
        assertThat(response.statusName(), is("CUSTOM_OK"));
        assertThat(response.mimeType(), is("mime"));
        assertThat(response.content(), is("text"));
        assertThat(response.asset(null), isA(byte[].class));
        assertThat(response.asset(null).length, is(0));
        assertThat(response.delay(), is(1));
        assertThat(response.asset, is("asset"));
        assertThat(response.delay, is(1));
        assertThat(response.maxDelay, is(1));
    }

    @Test
    public void asset_readingAssetsDoesNotConsumeErrors() throws Exception {
        Response response = new Response.Builder()
                .withAsset("asset://asset")
                .build();

        AssetManager mockedAssetManager = mock(AssetManager.class);
        doThrow(IOException.class).when(mockedAssetManager).open("asset://asset");

        Context mockedContext = mock(Context.class);
        doReturn(mockedAssetManager).when(mockedContext).getAssets();

        assertThatThrownBy(() -> response.asset(mockedContext))
                .isInstanceOf(IOException.class)
                .hasNoCause();
    }

    @Test
    public void asset_requestingAssetIsValidatedInCorrectOrder() throws Exception {
        AssetManager assetManager = mock(AssetManager.class);
        doReturn(new ByteArrayInputStream(new byte[]{2})).when(assetManager).open("asset://asset");

        Context context = mock(Context.class);
        doReturn(assetManager).when(context).getAssets();

        File file = new File("asset");
        Files.write(new byte[]{3}, file);

        try {
            // Byte array is prioritized.
            byte[] asset1 = new Response.Builder()
                    .withAsset("asset://asset")
                    .withAsset(new byte[]{1})
                    .build()
                    .asset(context);
            assertThat(asset1.length, is(1));
            assertThat(asset1[0], is((byte) 1));

            // If no in-memory byte array is found, then a "asset://" asset is
            // returned.
            byte[] asset2 = new Response.Builder()
                    .withAsset("asset://asset")
                    .build()
                    .asset(context);
            assertThat(asset2.length, is(1));
            assertThat(asset2[0], is((byte) 2));

            // If no in-memory byte array and no "asset://" asset is found, then
            // a "file://" asset is returned.
            byte[] asset3 = new Response.Builder()
                    .withAsset("file://asset")
                    .build()
                    .asset(context);
            assertThat(asset3.length, is(1));
            assertThat(asset3[0], is((byte) 3));

            // If there is no asset, then an empty byte array asset is returned.
            byte[] asset4 = new Response.Builder()
                    .build()
                    .asset(context);
            assertThat(asset4, is(notNullValue()));
            assertThat(asset4.length, is(0));

            // If asset is a null pointer, then an empty byte array asset is
            // returned.
            byte[] asset5 = new Response.Builder()
                    .withAsset((byte[]) null)
                    .withAsset((String) null)
                    .build()
                    .asset(context);
            assertThat(asset5, is(notNullValue()));
            assertThat(asset5.length, is(0));
        } finally {
            file.delete();
        }
    }

    @Test
    public void delay_exactDelayIsReturnedIfOnlyDefaultGiven() throws Exception {
        Response response = new Response.Builder()
                .withDelay(113)
                .build();

        assertThat(response.delay(), is(113));
    }

    @Test
    public void delay_exactZeroIsReturnedIfNoDelayGiven() throws Exception {
        Response response = new Response.Builder().build();
        assertThat(response.delay(), is(0));
    }

    @Test
    public void delay_randomDelayBetweenDefaultAndMaxIsReturned() throws Exception {
        Response response = new Response.Builder()
                .withDelay(110, 130)
                .build();

        assertThat(response.delay(), is(both(greaterThanOrEqualTo(110)).and(lessThanOrEqualTo(130))));
    }

    @Test
    public void delay_randomDelayBetweenZeroAndMaxIsReturned() throws Exception {
        Response response = new Response.Builder()
                .withDelay(-1, 130)
                .build();

        assertThat(response.delay(), is(both(greaterThanOrEqualTo(0)).and(lessThanOrEqualTo(130))));
    }

    @Test
    public void delay_defaultDelayReturnedIfMaxDelayLessThanDefaultDelay() throws Exception {
        Response response = new Response.Builder()
                .withDelay(110, 90)
                .build();

        assertThat(response.delay(), is(110));
    }

}
