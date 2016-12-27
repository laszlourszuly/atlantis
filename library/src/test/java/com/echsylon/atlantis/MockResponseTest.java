package com.echsylon.atlantis;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okio.BufferedSource;
import okio.Okio;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class MockResponseTest {

    @Test
    public void public_canBuildValidMockResponseFromCode() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("k2", "v2");

        HashMap<String, String> settings = new HashMap<>();
        settings.put("p2", "a2");

        MockResponse response = new MockResponse.Builder()
                .setStatus(1, "phrase")
                .addHeader("k1", "v1")
                .addHeaders(headers)
                .addSetting("p1", "a1")
                .addSettings(settings)
                .setBody("body")
                .build();

        assertThat(response.code(), is(1));
        assertThat(response.phrase(), is("phrase"));
        assertThat(response.headers().size(), is(2));
        assertThat(response.headers().get("k1"), is("v1"));
        assertThat(response.headers().get("k2"), is("v2"));
        assertThat(response.body(), is("body"));
    }

    @Test
    public void public_preventsModifyingHeadersMap() {
        MockResponse response = new MockResponse.Builder()
                .addHeader("key", "value")
                .build();
        Map<String, String> headers = response.headers();

        assertThatThrownBy(() -> headers.put("key2", "value2"))
                .isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(() -> headers.remove("key"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void public_neverReturnsNullPhrase() {
        MockResponse response = new MockResponse.Builder()
                .setStatus(0, null)
                .build();
        assertThat(response.phrase(), is(not(nullValue())));
        assertThat(response.phrase(), is(""));
    }

    @Test
    public void internal_doesNotOverwriteExistingSourceHelper() throws IOException {
        MockResponse response = new MockResponse.Builder()
                .setBody("foo".getBytes())
                .build();

        response.setSourceHelperIfAbsent(text ->
                Okio.source(new ByteArrayInputStream("bar".getBytes())));

        BufferedSource source = Okio.buffer(response.source());
        assertThat(source.readUtf8(), is("foo"));
    }

    @Test
    public void internal_canHintOnBody() {
        assertThat(new MockResponse.Builder()
                .addHeader("Content-Length", "1")
                .build()
                .isExpectedToHaveBody(), is(true));

        assertThat(new MockResponse.Builder()
                .addHeader("Content-Length", "0")
                .build()
                .isExpectedToHaveBody(), is(false));

        assertThat(new MockResponse.Builder()
                .addHeader("key", "value")
                .build()
                .isExpectedToHaveBody(), is(false));

        assertThat(new MockResponse.Builder()
                .build()
                .isExpectedToHaveBody(), is(false));
    }

    @Test
    public void internal_canHintOnChunked() {
        assertThat(new MockResponse.Builder()
                .addHeader("Transfer-Encoding", "Chunked")
                .build()
                .isExpectedToBeChunked(), is(true));

        assertThat(new MockResponse.Builder()
                .addHeader("Transfer-Encoding", "foo")
                .build()
                .isExpectedToBeChunked(), is(false));

        assertThat(new MockResponse.Builder()
                .addHeader("key", "value")
                .build()
                .isExpectedToBeChunked(), is(false));

        assertThat(new MockResponse.Builder()
                .build()
                .isExpectedToBeChunked(), is(false));
    }

    @Test
    public void internal_canHintOnContinue() {
        assertThat(new MockResponse.Builder()
                .addHeader("Expect", "100-continue")
                .build()
                .isExpectedToContinue(), is(true));

        assertThat(new MockResponse.Builder()
                .addHeader("Expect", "bar")
                .build()
                .isExpectedToContinue(), is(false));

        assertThat(new MockResponse.Builder()
                .addHeader("key", "value")
                .build()
                .isExpectedToContinue(), is(false));

        assertThat(new MockResponse.Builder()
                .build()
                .isExpectedToContinue(), is(false));
    }
}
