package com.echsylon.atlantis;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okio.Okio;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class MockResponseTest {

    @Test
    public void public_canBuildValidMockResponseFromCode() {
        ArrayList<String> values = new ArrayList<>();
        values.add("v2");

        HashMap<String, List<String>> headers = new HashMap<>();
        headers.put("k2", values);

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
        assertThat(response.headerManager().keyCount(), is(2));
        assertThat(response.headerManager().getMostRecent("k1"), is("v1"));
        assertThat(response.headerManager().getMostRecent("k2"), is("v2"));
        assertThat(response.source(), is("body"));
    }

    @Test
    public void public_preventsModifyingHeadersMap() {
        MockResponse response = new MockResponse.Builder()
                .addHeader("key", "value")
                .build();

        Map<String, List<String>> headers = response.headerManager().getAllAsMultiMap();

        assertThatThrownBy(() -> headers.put("key", null))
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
                .setBody("foo")
                .build();

        // Set the original source helper
        response.setSourceHelperIfAbsent(bytes -> Okio.source(new ByteArrayInputStream(bytes)));

        // Now try to overwrite it
        response.setSourceHelperIfAbsent(bytes -> Okio.source(new ByteArrayInputStream("bar".getBytes())));

        // Verify overwrite failed
        assertThat(response.body(), is("foo"));
    }

    @Test
    public void internal_canHintOnBody() {
        assertThat(new MockResponse.Builder()
                .addHeader("Content-Length", "1")
                .build()
                .headerManager()
                .isExpectedToHaveBody(), is(true));

        assertThat(new MockResponse.Builder()
                .addHeader("Content-Length", "0")
                .build()
                .headerManager()
                .isExpectedToHaveBody(), is(false));

        assertThat(new MockResponse.Builder()
                .addHeader("key", "value")
                .build()
                .headerManager()
                .isExpectedToHaveBody(), is(false));

        assertThat(new MockResponse.Builder()
                .build()
                .headerManager()
                .isExpectedToHaveBody(), is(false));
    }

    @Test
    public void internal_canHintOnChunked() {
        assertThat(new MockResponse.Builder()
                .addHeader("Transfer-Encoding", "Chunked")
                .build()
                .headerManager()
                .isExpectedToBeChunked(), is(true));

        assertThat(new MockResponse.Builder()
                .addHeader("Transfer-Encoding", "foo")
                .build()
                .headerManager()
                .isExpectedToBeChunked(), is(false));

        assertThat(new MockResponse.Builder()
                .addHeader("key", "value")
                .build()
                .headerManager()
                .isExpectedToBeChunked(), is(false));

        assertThat(new MockResponse.Builder()
                .build()
                .headerManager()
                .isExpectedToBeChunked(), is(false));
    }

    @Test
    public void internal_canHintOnContinue() {
        assertThat(new MockResponse.Builder()
                .addHeader("Expect", "100-continue")
                .build()
                .headerManager()
                .isExpectedToContinue(), is(true));

        assertThat(new MockResponse.Builder()
                .addHeader("Expect", "bar")
                .build()
                .headerManager()
                .isExpectedToContinue(), is(false));

        assertThat(new MockResponse.Builder()
                .addHeader("key", "value")
                .build()
                .headerManager()
                .isExpectedToContinue(), is(false));

        assertThat(new MockResponse.Builder()
                .build()
                .headerManager()
                .isExpectedToContinue(), is(false));
    }
}
