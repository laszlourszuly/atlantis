package com.echsylon.atlantis.filter;

import com.echsylon.atlantis.Request;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Verifies expected behavior on the {@link DefaultRequestFilter} class.
 */
public class DefaultRequestFilterTest {

    @Test
    public void match_canFilterOnMethod() throws Exception {
        // Prepare the mocked request configurations. Our test configuration will
        // hold a single mocked request with a "get" method and an empty path.
        Request mockedRequest = mock(Request.class);
        doReturn("get").when(mockedRequest).method();
        doReturn("").when(mockedRequest).url();

        List<Request> templates = Collections.singletonList(mockedRequest);
        DefaultRequestFilter defaultRequestFilter = new DefaultRequestFilter();

        // Find the first request configuration (or null) matching a "get" method.
        Request result1 = defaultRequestFilter.getRequest(templates, "", "get", null);
        assertThat(result1, is(mockedRequest));

        // Find the first request configuration (or null) matching a "post" method.
        Request result2 = defaultRequestFilter.getRequest(templates, "", "post", null);
        assertThat(result2, is(nullValue()));
    }

    @Test
    public void match_findsMatchingRegexUrl() throws Exception {
        // Prepare the mocked request configurations. Our test configuration will
        // hold a single mocked request with a "get" method and a path that consists
        // of exactly 3 (arbitrary) characters, any number of query parameters built
        // up by any alphanumeric characters and a fragment, also containing any
        // alphanumeric characters.
        Request mockedRequest = mock(Request.class);
        doReturn("get").when(mockedRequest).method();
        doReturn(".*\\/[a-zA-Z]{3}\\?[a-zA-Z_0-9&=]*\\#[a-zA-Z_0-9]*").when(mockedRequest).url();

        List<Request> templates = Collections.singletonList(mockedRequest);
        DefaultRequestFilter defaultRequestFilter = new DefaultRequestFilter();

        // Find the first request configuration (or null) matching:
        // method:   "get"
        // scheme:   "scheme"
        // host:     "host"
        // path:     "abc"
        // query:    "hello=true&goodbye=0"
        // fragment: "fragment123"
        String url1 = "scheme://host/abc?hello=true&goodbye=0#fragment123";
        Request result1 = defaultRequestFilter.getRequest(templates, url1, "get", null);
        assertThat(result1, is(mockedRequest));

        // Find the first request configuration (or null) matching:
        // method:   "get"
        // path:     "abc"
        // query:    "hello=true&goodbye=0"
        // fragment: "fragment123"
        String url2 = "/abc?hello=true&goodbye=0#fragment123";
        Request result2 = defaultRequestFilter.getRequest(templates, url2, "get", null);
        assertThat(result2, is(mockedRequest));

        // Find the first request configuration (or null) matching:
        // method:   "get"
        // path:     "abc"
        // query:    "hello=true&goodbye=0"
        // fragment: "fragment123"
        String url3 = "/abc?hello=true#fragment123";
        Request result3 = defaultRequestFilter.getRequest(templates, url3, "get", null);
        assertThat(result3, is(mockedRequest));
    }

    @Test
    public void match_findsMatchingStaticUrl() {
        // Prepare the mocked request configurations. Our test configuration will hold
        // a single mocked request with a "get" method and a "/path?query#fragment" url.
        Request mockedRequest = mock(Request.class);
        doReturn("get").when(mockedRequest).method();
        doReturn("/path?query#fragment").when(mockedRequest).url();

        List<Request> templates = Collections.singletonList(mockedRequest);
        DefaultRequestFilter defaultRequestFilter = new DefaultRequestFilter();

        // Find the first request configuration (or null) matching a "/path?query#fragment" url.
        String url1 = "/path?query#fragment";
        Request result1 = defaultRequestFilter.getRequest(templates, url1, "get", null);
        assertThat(result1, is(mockedRequest));

        // Find the first request configuration (or null) matching a
        // "scheme://host/path?query#fragment" url.
        String url2 = "scheme://host/path?query#fragment";
        Request result2 = defaultRequestFilter.getRequest(templates, url2, "get", null);
        assertThat(result2, is(mockedRequest));
    }

    @Test
    public void match_doesNotFindNonMatchingRegexUrl() throws Exception {
        // Prepare the mocked request configurations. Our test configuration will
        // hold a single mocked request with a "get" method and a path that consists
        // of exactly 3 (arbitrary) characters, any number of query parameters built
        // up by any alphanumeric characters and a fragment, also containing any
        // alphanumeric characters.
        Request mockedRequest = mock(Request.class);
        doReturn("get").when(mockedRequest).method();
        doReturn(".*\\/[a-zA-Z]{3}\\?[a-zA-Z_0-9&=]*\\#[a-zA-Z_0-9]*").when(mockedRequest).url();

        List<Request> templates = Collections.singletonList(mockedRequest);
        DefaultRequestFilter defaultRequestFilter = new DefaultRequestFilter();

        // Try to find a request configuration for a url with a missing path.
        String url1 = "?hello=true#fragment123";
        Request result1 = defaultRequestFilter.getRequest(templates, url1, "get", null);
        assertThat(result1, is(nullValue()));

        // Try to find a request configuration for an empty url.
        String url2 = "";
        Request result2 = defaultRequestFilter.getRequest(templates, url2, "get", null);
        assertThat(result2, is(nullValue()));
    }

    @Test
    public void match_canFilterOnHeaders() {
        Map<String, String> mockedHeaders = new HashMap<>();
        mockedHeaders.put("key1", "value1");

        Request mockedRequest = mock(Request.class);
        doReturn("").when(mockedRequest).method();
        doReturn("").when(mockedRequest).url();
        doReturn(mockedHeaders).when(mockedRequest).headers();

        List<Request> templates = Collections.singletonList(mockedRequest);
        DefaultRequestFilter defaultRequestFilter = new DefaultRequestFilter();

        // Find the first request that matches my filter header keys and values.
        Map<String, String> filterHeaders1 = new HashMap<>();
        filterHeaders1.put("key1", "value1");
        filterHeaders1.put("key2", "value2");
        Request result1 = defaultRequestFilter.getRequest(templates, "", "", filterHeaders1);
        assertThat(result1, is(mockedRequest));

        // Match on null pointer filter headers (null == "don't match")
        Request result2 = defaultRequestFilter.getRequest(templates, "", "", null);
        assertThat(result2, is(mockedRequest));

        // Don't match on non-matching filter.
        Map<String, String> filterHeaders3 = new HashMap<>();
        filterHeaders3.put("key3", "value3");
        Request result3 = defaultRequestFilter.getRequest(templates, "", "", filterHeaders3);
        assertThat(result3, is(nullValue()));

        // Don't match on empty filter.
        Map<String, String> filterHeaders4 = Collections.emptyMap();
        Request result4 = defaultRequestFilter.getRequest(templates, "", "", filterHeaders4);
        assertThat(result4, is(nullValue()));
    }

}
