package com.echsylon.atlantis;

import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class MockRequestTest {

    @Test
    public void public_canBuildValidMockRequestFromCode() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("k2", "v2");

        MockResponse response = mock(MockResponse.class);
        MockResponse.Filter filter = mock(MockResponse.Filter.class);

        MockRequest request = new MockRequest.Builder()
                .setMethod("get")
                .setUrl("url")
                .addHeader("k1", "v1")
                .addHeaders(headers)
                .addResponse(response)
                .setResponseFilter(filter)
                .build();

        assertThat(request.method(), is("get"));
        assertThat(request.url(), is("url"));
        assertThat(request.headers().size(), is(2));
        assertThat(request.headers().get("k1"), is("v1"));
        assertThat(request.headers().get("k2"), is("v2"));
        assertThat(request.responses().size(), is(1));
        assertThat(request.responses().get(0), is(response));
        assertThat(request.responseFilter(), is(filter));
    }

    @Test
    public void public_preventsModifyingHeadersMap() {
        MockRequest request = new MockRequest.Builder()
                .addHeader("key", "value")
                .build();
        Map<String, String> headers = request.headers();

        assertThatThrownBy(() -> headers.put("key2", "value2"))
                .isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(() -> headers.remove("key"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void public_neverReturnsNullMethod() {
        MockRequest request = new MockRequest.Builder()
                .setMethod(null)
                .build();
        assertThat(request.method(), is(not(nullValue())));
        assertThat(request.method(), is(""));
    }

    @Test
    public void public_neverReturnsNullUrl() {
        MockRequest request = new MockRequest.Builder()
                .setUrl(null)
                .build();
        assertThat(request.url(), is(not(nullValue())));
        assertThat(request.url(), is(""));
    }

    /**
     * This feature is used when inflating a MockRequest object from a JSON
     * string and there is a response filter stated.
     */
    @Test
    public void internal_canOverrideResponseFilter() {
        MockResponse.Filter filter = mock(MockResponse.Filter.class);
        MockRequest request = new MockRequest.Builder()
                .setResponseFilter(mock(MockResponse.Filter.class))
                .build();
        assertThat(request.responseFilter(), is(not(filter)));

        request.setResponseFilter(filter);
        assertThat(request.responseFilter(), is(filter));
    }

    @Test
    public void internal_canGetResponseWhenNoFilterSpecified() {
        MockResponse response = mock(MockResponse.class);
        MockRequest request = new MockRequest.Builder()
                .addResponse(response)
                .addResponse(mock(MockResponse.class))
                .addResponse(mock(MockResponse.class))
                .build();

        assertThat(request.response(), is(response));
    }

    @Test
    public void internal_preventsModifyingMockedResponsesList() {
        MockRequest request = new MockRequest.Builder().build();
        List<MockResponse> responses = request.responses();

        assertThatThrownBy(() -> responses.add(mock(MockResponse.class)))
                .isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(() -> responses.remove(0))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void internal_canHintOnBody() {
        assertThat(new MockRequest.Builder()
                .addHeader("Content-Length", "1")
                .build()
                .isExpectedToHaveBody(), is(true));

        assertThat(new MockRequest.Builder()
                .addHeader("Content-Length", "0")
                .build()
                .isExpectedToHaveBody(), is(false));

        assertThat(new MockRequest.Builder()
                .addHeader("key", "value")
                .build()
                .isExpectedToHaveBody(), is(false));

        assertThat(new MockRequest.Builder()
                .build()
                .isExpectedToHaveBody(), is(false));
    }

    @Test
    public void internal_canHintOnChunked() {
        assertThat(new MockRequest.Builder()
                .addHeader("Transfer-Encoding", "Chunked")
                .build()
                .isExpectedToBeChunked(), is(true));

        assertThat(new MockRequest.Builder()
                .addHeader("Transfer-Encoding", "foo")
                .build()
                .isExpectedToBeChunked(), is(false));

        assertThat(new MockRequest.Builder()
                .addHeader("key", "value")
                .build()
                .isExpectedToBeChunked(), is(false));

        assertThat(new MockRequest.Builder()
                .build()
                .isExpectedToBeChunked(), is(false));
    }

}
