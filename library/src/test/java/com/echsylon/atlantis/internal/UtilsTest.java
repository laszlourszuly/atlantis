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
    public void close_closeSilentlyReallyIsSilent() throws Exception {
        Closeable mockedCloseable = mock(Closeable.class);
        doThrow(IOException.class).when(mockedCloseable).close();
        Utils.closeSilently(mockedCloseable);
    }

    @Test
    public void native_getNativeBoolean() throws Exception {
        assertThat(Utils.getNative(Boolean.TRUE, false), is(true));
        assertThat(Utils.getNative(null, true), is(true));
    }

    @Test
    public void native_getNativeFloat() throws Exception {
        Float floatObject = 1f;
        assertThat(Utils.getNative(floatObject, 0f), is(1f));
        assertThat(Utils.getNative(null, 2f), is(2f));
    }

    @Test
    public void native_getNativeInteger() throws Exception {
        Integer intObject = 3;
        assertThat(Utils.getNative(intObject, 0), is(3));
        assertThat(Utils.getNative((Integer) null, 4), is(4));
    }

    @Test
    public void native_getNativeLong() throws Exception {
        Long longObject = 5L;
        assertThat(Utils.getNative(longObject, 0L), is(5L));
        assertThat(Utils.getNative((Long) null, 6L), is(6L));
    }

    @Test
    public void native_getNonNull() throws Exception {
        Object object = new Object();
        Object fallback = new Object();
        assertThat(Utils.getNonNull(object, fallback), is(object));
        assertThat(Utils.getNonNull(null, fallback), is(fallback));
        assertThat(Utils.getNonNull(null, null), is(nullValue()));
    }

    @Test
    public void empty_isAnyEmptyString() throws Exception {
        assertThat(Utils.isAnyEmpty(), is(true));
        assertThat(Utils.isAnyEmpty(""), is(true));
        assertThat(Utils.isAnyEmpty((String) null), is(true));
        assertThat(Utils.isAnyEmpty("not empty", ""), is(true));
        assertThat(Utils.isAnyEmpty("not empty", null), is(true));

        assertThat(Utils.isAnyEmpty(" "), is(false));
        assertThat(Utils.isAnyEmpty("also not empty"), is(false));
    }

    @Test
    public void empty_isEmptyArray() throws Exception {
        assertThat(Utils.isEmpty((Object[]) null), is(true));
        assertThat(Utils.isEmpty(new Object[0]), is(true));
        assertThat(Utils.isEmpty(new Object[1]), is(false));
    }

    @Test
    public void empty_isEmptyList() throws Exception {
        assertThat(Utils.isEmpty((List) null), is(true));
        assertThat(Utils.isEmpty(new ArrayList<>()), is(true));
        assertThat(Utils.isEmpty(Arrays.asList(new Object[1])), is(false));
    }

    @Test
    public void empty_isEmptyString() throws Exception {
        assertThat(Utils.isEmpty((String) null), is(true));
        assertThat(Utils.isEmpty(""), is(true));
        assertThat(Utils.isEmpty(" "), is(false));
    }

    @Test
    public void empty_notAnyEmptyString() throws Exception {
        assertThat(Utils.notAnyEmpty(), is(false));
        assertThat(Utils.notAnyEmpty(""), is(false));
        assertThat(Utils.notAnyEmpty((String) null), is(false));
        assertThat(Utils.notAnyEmpty("not empty", ""), is(false));
        assertThat(Utils.notAnyEmpty("not empty", null), is(false));

        assertThat(Utils.notAnyEmpty(" "), is(true));
        assertThat(Utils.notAnyEmpty("also not empty"), is(true));
    }

    @Test
    public void empty_notEmptyArray() throws Exception {
        assertThat(Utils.notEmpty((Object[]) null), is(false));
        assertThat(Utils.notEmpty(new Object[0]), is(false));
        assertThat(Utils.notEmpty(new Object[1]), is(true));
    }

    @Test
    public void empty_notEmptyList() throws Exception {
        assertThat(Utils.notEmpty((List) null), is(false));
        assertThat(Utils.notEmpty(new ArrayList<>()), is(false));
        assertThat(Utils.notEmpty(Arrays.asList(new Object[1])), is(true));
    }

    @Test
    public void empty_notEmptyString() throws Exception {
        assertThat(Utils.notEmpty((String) null), is(false));
        assertThat(Utils.notEmpty(""), is(false));
        assertThat(Utils.notEmpty(" "), is(true));
    }

    @Test
    public void io_canReadAsset() throws Exception {
        Context mockedContext = mock(Context.class);
        AssetManager mockedAssetManager = mock(AssetManager.class);

        doReturn(mockedAssetManager).when(mockedContext).getAssets();
        doReturn(new ByteArrayInputStream("{}".getBytes())).when(mockedAssetManager).open(anyString());

        byte[] bytes = Utils.readAsset(mockedContext, "whatever.asset");
        assertThat(bytes, notNullValue());
        assertThat(bytes.length, is(2));
    }

}
