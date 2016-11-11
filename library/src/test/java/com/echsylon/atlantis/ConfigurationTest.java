package com.echsylon.atlantis;

import com.echsylon.atlantis.internal.json.JsonParser;

import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Verifies expected behavior on the {@link Configuration} class.
 */
public class ConfigurationTest {

    // This is a test implementation of the Request#Filter interface. It's used
    // for verifying that a request filter can be instantiated by parsing JSON.
    @SuppressWarnings("unused")
    public static final class TestFilter implements Request.Filter {
        @Override
        public Request getRequest(List<Request> requests, String url, String method, Map<String, String> headers) {
            return null;
        }
    }

    @Test
    public void create_canBuildFullConfiguration() throws Exception {
        Request request = mock(Request.class);
        Request.Filter filter = mock(Request.Filter.class);

        Configuration configuration = new Configuration.Builder()
                .withFallbackBaseUrl("fallbackUrl")
                .withRequest(request)
                .withRequestFilter(filter)
                .build();

        assertThat(configuration.hasAlternativeRoute(), is(true));
        assertThat(configuration.fallbackBaseUrl(), is("fallbackUrl"));
        assertThat(configuration.requestFilter(), is(filter));
        assertThat(configuration.requests, is(notNullValue()));
        assertThat(configuration.requests.size(), is(1));
        assertThat(configuration.requests.get(0), is(request));
    }

    @Test
    public void create_canParseFullConfiguration() throws Exception {
        String json = "{ fallbackBaseUrl: 'fallbackUrl', " +
                "requestFilter: 'com.echsylon.atlantis.ConfigurationTest$TestFilter', " +
                "requests: [{}]}";

        Configuration configuration = new JsonParser().fromJson(json, Configuration.class);

        assertThat(configuration.hasAlternativeRoute(), is(true));
        assertThat(configuration.fallbackBaseUrl(), is("fallbackUrl"));
        assertThat(configuration.requestFilter(), isA(Request.Filter.class));
        assertThat(configuration.requests, is(notNullValue()));
        assertThat(configuration.requests.size(), is(1));
        assertThat(configuration.requests.get(0), isA(Request.class));
    }

    @Test
    public void request_canAddNewRequestAfterCreationWithExistingDefaultRequests() {
        Request request1 = mock(Request.class);
        Configuration configuration = new Configuration.Builder()
                .withRequest(request1)
                .build();

        assertThat(configuration.requests, is(notNullValue()));
        assertThat(configuration.requests.size(), is(1));
        assertThat(configuration.requests.get(0), is(request1));

        Request request2 = mock(Request.class);
        configuration.addRequest(request2);

        assertThat(configuration.requests, is(notNullValue()));
        assertThat(configuration.requests.size(), is(2));
        assertThat(configuration.requests.get(0), is(request1));
        assertThat(configuration.requests.get(1), is(request2));
    }

    @Test
    public void request_canAddNewRequestAfterCreationWithNoDefaultRequests() {
        Configuration configuration = new Configuration.Builder().build();
        assertThat(configuration.requests, is(nullValue()));

        Request request = mock(Request.class);
        configuration.addRequest(request);

        assertThat(configuration.requests, is(notNullValue()));
        assertThat(configuration.requests.size(), is(1));
        assertThat(configuration.requests.get(0), is(request));
    }

    @Test
    public void request_canNotAddSameRequestInstanceAfterCreation() {
        Configuration configuration = new Configuration.Builder().build();
        assertThat(configuration.requests, is(nullValue()));

        Request request = mock(Request.class);

        configuration.addRequest(request);
        assertThat(configuration.requests, is(notNullValue()));
        assertThat(configuration.requests.size(), is(1));
        assertThat(configuration.requests.get(0), is(request));

        configuration.addRequest(request);
        assertThat(configuration.requests, is(notNullValue()));
        assertThat(configuration.requests.size(), is(1));
        assertThat(configuration.requests.get(0), is(request));
    }

    @Test
    public void request_fallsBackToDefaultRequestFilterWhenNonGiven() {
        Request request = mock(Request.class);
        doReturn("get").when(request).method();
        doReturn("scheme://host/path").when(request).url();

        Configuration configuration = new Configuration.Builder()
                .withRequest(request)
                .build();

        assertThat(configuration.findRequest("scheme://host/path", "get", null), is(request));
    }

    @Test
    public void request_returnsRequestAccordingToCurrentRequestFilter() {
        Request request = mock(Request.class);
        doReturn("get").when(request).method();
        doReturn("scheme://host/path").when(request).url();

        Request.Filter filter = mock(Request.Filter.class);
        doReturn(request).when(filter).getRequest(anyList(), anyString(), anyString(), anyMap());

        Configuration configuration = new Configuration.Builder()
                .withRequest(request)
                .withRequestFilter(filter)
                .build();

        assertThat(configuration.findRequest("url", "method", Collections.emptyMap()), is(request));
    }

}
