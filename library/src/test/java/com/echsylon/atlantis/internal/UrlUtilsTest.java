package com.echsylon.atlantis.internal;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * Verifies expected behavior on the {@link UrlUtils} class.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 16)
public class UrlUtilsTest {

    @Test
    public void read_canParsePath() throws Exception {
        assertThat(UrlUtils.getPath("host/path"), is("/path"));
        assertThat(UrlUtils.getPath("scheme://host?query=1"), is(""));
        assertThat(UrlUtils.getPath("scheme://host/path?query=2"), is("/path"));
        assertThat(UrlUtils.getPath("scheme://host/multi/path?query=3"), is("/multi/path"));
        assertThat(UrlUtils.getPath("scheme://host/path#fragment"), is("/path"));
    }

    @Test
    public void read_canParseQuery() throws Exception {
        assertThat(UrlUtils.getQuery("scheme://path"), is(""));
        assertThat(UrlUtils.getQuery("scheme://path?"), is("?"));
        assertThat(UrlUtils.getQuery("scheme://path?q=2#fragment"), is("?q=2"));
        assertThat(UrlUtils.getQuery("?q=3&r=1"), is("?q=3&r=1"));
    }

    @Test
    public void read_canParseFragment() throws Exception {
        assertThat(UrlUtils.getFragment("scheme://host/path?q=1"), is(""));
        assertThat(UrlUtils.getFragment("scheme://host/path?q=1#fragment"), is("#fragment"));
    }

    @Test
    public void read_canParseResponseCode() throws Exception {
        HttpURLConnection mockConnection = mock(HttpURLConnection.class);
        doReturn(1).when(mockConnection).getResponseCode();
        assertThat(UrlUtils.getResponseCode(mockConnection), is(1));

        doThrow(IOException.class).when(mockConnection).getResponseCode();
        assertThat(UrlUtils.getResponseCode(mockConnection), is(0));
        assertThat(UrlUtils.getResponseCode(null), is(0));
    }

    @Test
    public void read_canParseResponseMessage() throws Exception {
        HttpURLConnection mockConnection = mock(HttpURLConnection.class);
        doReturn("message").when(mockConnection).getResponseMessage();
        assertThat(UrlUtils.getResponseMessage(mockConnection), is("message"));

        doThrow(IOException.class).when(mockConnection).getResponseMessage();
        assertThat(UrlUtils.getResponseMessage(mockConnection), is(""));
        assertThat(UrlUtils.getResponseMessage(null), is(""));
    }

    @Test
    public void read_canParseResponseHeaders() throws Exception {
        Map mockHeaders = mock(Map.class);
        doReturn(3).when(mockHeaders).size();

        HttpURLConnection mockConnection = mock(HttpURLConnection.class);
        doReturn(mockHeaders).when(mockConnection).getHeaderFields();
        doReturn("key1", null, "key3").when(mockConnection).getHeaderFieldKey(anyInt());
        doReturn(null, null, "value3").when(mockConnection).getHeaderField(anyInt());

        Map<String, String> headers = UrlUtils.getResponseHeaders(null);
        assertThat(headers, is(notNullValue()));
        assertThat(headers.size(), is(0));

        headers = UrlUtils.getResponseHeaders(mockConnection);
        assertThat(headers.size(), is(1));
        assertThat(headers.get("key3"), is("value3"));
    }

    @Test
    public void read_canParseSuccessResponseBody() throws Exception {
        final byte[] mark = {0};
        InputStream mockInputStream = mock(InputStream.class);
        doAnswer(invocation -> {
            byte[] buff = invocation.getArgument(0);
            if (buff[0] != mark[0]) {
                buff[0] = mark[0];
                return 1;
            } else {
                return -1;
            }
        }).when(mockInputStream).read(any(byte[].class));

        HttpURLConnection mockConnection = mock(HttpURLConnection.class);
        doReturn(mockInputStream).when(mockConnection).getInputStream();
        doReturn(mockInputStream).when(mockConnection).getErrorStream();

        doThrow(IOException.class).when(mockConnection).getResponseCode();
        byte[] result = UrlUtils.getResponseBody(mockConnection);
        assertThat(result, is(notNullValue()));
        assertThat(result.length, is(0));

        mark[0] = 12;
        doReturn(200).when(mockConnection).getResponseCode();
        byte[] success = UrlUtils.getResponseBody(mockConnection);
        assertThat(success.length, is(1));
        assertThat(success[0], is(mark[0]));

        mark[0] = 24;
        doReturn(400).when(mockConnection).getResponseCode();
        byte[] failure = UrlUtils.getResponseBody(mockConnection);
        assertThat(failure.length, is(1));
        assertThat(failure[0], is(mark[0]));

        byte[] empty = UrlUtils.getResponseBody(null);
        assertThat(empty.length, is(0));
    }

    @Test
    public void read_canParseContentType() throws Exception {
        HttpURLConnection mockConnection = mock(HttpURLConnection.class);
        doReturn("application/json").when(mockConnection).getContentType();
        assertThat(UrlUtils.getResponseMimeType(mockConnection), is("application/json"));

        doReturn(null).when(mockConnection).getContentType();
        assertThat(UrlUtils.getResponseMimeType(mockConnection), is(""));
        assertThat(UrlUtils.getResponseMimeType(null), is(""));
    }

}
