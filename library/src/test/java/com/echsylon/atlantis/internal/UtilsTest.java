package com.echsylon.atlantis.internal;

import android.content.Context;
import android.content.res.AssetManager;

import com.echsylon.atlantis.BuildConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * Verifies expected behavior on the {@link Utils} class.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 16)
public class UtilsTest {

    @Test
    public void testCloseSilentlyReallyIsSilent() throws Exception {
        Closeable mockedCloseable = mock(Closeable.class);
        doThrow(IOException.class).when(mockedCloseable).close();
        Utils.closeSilently(mockedCloseable);
    }

    @Test
    public void testGetNativeBoolean() throws Exception {
        assertThat(Utils.getNative(Boolean.TRUE, false), is(true));
        assertThat(Utils.getNative(null, true), is(true));
    }

    @Test
    public void testGetNativeFloat() throws Exception {
        Float floatObject = 1f;
        assertThat(Utils.getNative(floatObject, 0f), is(1f));
        assertThat(Utils.getNative(null, 2f), is(2f));
    }

    @Test
    public void testGetNativeInteger() throws Exception {
        Integer intObject = 3;
        assertThat(Utils.getNative(intObject, 0), is(3));
        assertThat(Utils.getNative((Integer) null, 4), is(4));
    }

    @Test
    public void testGetNativeLong() throws Exception {
        Long longObject = 5L;
        assertThat(Utils.getNative(longObject, 0L), is(5L));
        assertThat(Utils.getNative((Long) null, 6L), is(6L));
    }

    @Test
    public void testGetNonNull() throws Exception {
        Object object = new Object();
        Object fallback = new Object();
        assertThat(Utils.getNonNull(object, fallback), is(object));
        assertThat(Utils.getNonNull(null, fallback), is(fallback));
        assertThat(Utils.getNonNull(null, null), is(nullValue()));
    }

    @Test
    public void testIsEmptyArray() throws Exception {
        assertThat(Utils.isEmpty((Object[]) null), is(true));
        assertThat(Utils.isEmpty(new Object[0]), is(true));
        assertThat(Utils.isEmpty(new Object[1]), is(false));
    }

    @Test
    public void testIsEmptyList() throws Exception {
        assertThat(Utils.isEmpty((List) null), is(true));
        assertThat(Utils.isEmpty(new ArrayList<>()), is(true));
        assertThat(Utils.isEmpty(Arrays.asList(new Object[1])), is(false));
    }

    @Test
    public void testIsEmptyString() throws Exception {
        assertThat(Utils.isEmpty((String) null), is(true));
        assertThat(Utils.isEmpty(""), is(true));
        assertThat(Utils.isEmpty(" "), is(false));
    }

    @Test
    public void testNotEmptyArray() throws Exception {
        assertThat(Utils.notEmpty((Object[]) null), is(false));
        assertThat(Utils.notEmpty(new Object[0]), is(false));
        assertThat(Utils.notEmpty(new Object[1]), is(true));
    }

    @Test
    public void testNotEmptyList() throws Exception {
        assertThat(Utils.notEmpty((List) null), is(false));
        assertThat(Utils.notEmpty(new ArrayList<>()), is(false));
        assertThat(Utils.notEmpty(Arrays.asList(new Object[1])), is(true));
    }

    @Test
    public void testNotEmptyString() throws Exception {
        assertThat(Utils.notEmpty((String) null), is(false));
        assertThat(Utils.notEmpty(""), is(false));
        assertThat(Utils.notEmpty(" "), is(true));
    }

    @Test
    public void testCanReadAsset() throws Exception {
        Context mockedContext = mock(Context.class);
        AssetManager mockedAssetManager = mock(AssetManager.class);

        doReturn(mockedAssetManager).when(mockedContext).getAssets();
        doReturn(new ByteArrayInputStream("{}".getBytes())).when(mockedAssetManager).open(anyString());

        byte[] bytes = Utils.readAsset(mockedContext, "whatever.asset");
        assertThat(bytes, notNullValue());
        assertThat(bytes.length, is(2));
    }

}
