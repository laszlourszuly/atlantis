package com.echsylon.atlantis;

import com.echsylon.atlantis.internal.json.JsonParser;

import org.junit.Test;

import java.util.HashMap;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Verifies expected behavior on the {@link Response} class.
 */
public class RequestTest {

    // This is a test implementation of the Response#Filter interface. It's used
    // for verifying that a response filter can be instantiated by parsing JSON.
    @SuppressWarnings("unused")
    public static final class TestFilter implements Response.Filter {
        @Override
        public Response getResponse(Request request, List<Response> responses) {
            return null;
        }
    }

    @Test
    public void create_canBuildFullRequest() {
        Response response = mock(Response.class);
        Response.Filter filter = mock(Response.Filter.class);

        HashMap<String, String> headers = new HashMap<>();
        headers.put("h1", "v1");
        headers.put("h2", "v2");

        Request request = new Request.Builder()
                .withHeader("h0", "v0")
                .withHeaders(headers)
                .withHeader("h3", "v3")
                .withUrl("url")
                .withMethod("method")
                .withResponse(response)
                .withResponseFilter(filter)
                .build();

        assertThat(request.headers().get("h0"), is("v0"));
        assertThat(request.headers().get("h1"), is("v1"));
        assertThat(request.headers().get("h2"), is("v2"));
        assertThat(request.headers().get("h3"), is("v3"));
        assertThat(request.headers().get("invalid"), is(nullValue()));
        assertThat(request.url(), is("url"));
        assertThat(request.method(), is("method"));
        assertThat(request.responseFilter(), is(filter));
        assertThat(request.responses, is(notNullValue()));
        assertThat(request.responses.size(), is(1));
        assertThat(request.responses.get(0), is(response));
    }

    @Test
    public void create_canParseFullRequest() {
        String json = "{ method: 'method', url: 'url', headers: { h0: 'v0' }, " +
                "responseFilter: 'com.echsylon.atlantis.RequestTest$TestFilter', " +
                "responses: [{ responseCode: { code: 2, name: 'name' } }]}";

        Request request = new JsonParser().fromJson(json, Request.class);

        assertThat(request.headers().get("h0"), is("v0"));
        assertThat(request.headers().get("invalid"), is(nullValue()));
        assertThat(request.url(), is("url"));
        assertThat(request.method(), is("method"));
        assertThat(request.responseFilter(), isA(Response.Filter.class));
        assertThat(request.responses, is(notNullValue()));
        assertThat(request.responses.size(), is(1));
        assertThat(request.responses.get(0), isA(Response.class));
    }

    @Test
    public void response_fallsBackToDefaultResponseFilterWhenNonGiven() {
        Response response = mock(Response.class);
        Request request = new Request.Builder()
                .withResponse(response)
                .withResponse(mock(Response.class))
                .build();

        // The default response filter will always return the first response in
        // the list of available responses.
        assertThat(request.response(), is(response));
    }

    @Test
    public void response_returnsResponseAccordingToCurrentResponseFilter() {
        // The mock response filter will always return this mark response.
        Response response = mock(Response.class);

        // Our custom response filter implementation.
        Response.Filter filter = mock(Response.Filter.class);
        doReturn(response).when(filter).getResponse(any(), anyList());

        Request request = new Request.Builder()
                .withResponseFilter(filter)
                .withResponse(mock(Response.class))
                .withResponse(response)
                .build();

        // Our custom response filter will always return a specific response in
        // the list of available responses.
        assertThat(request.response(), is(response));
    }

}
