package com.echsylon.atlantis.template;

import com.google.gson.Gson;

import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Verifies expected behavior on the {@link Response} class.
 */
public class RequestTest {

    @Test
    public void create_canBuildEmptyRequest() throws Exception {
        Request request = new Request.Builder();
        assertThat(request, is(notNullValue()));
        assertThat(request, is(instanceOf(Request.class)));
        assertThat(request.url(), is(nullValue()));
        assertThat(request.method(), is(nullValue()));
        assertThat(request.headers(), is(notNullValue()));
        assertThat(request.headers(), is(Collections.EMPTY_MAP));
        assertThat(request.response(), is(nullValue()));
    }

    @Test
    public void create_canBuildFullRequest() throws Exception {
        Request request = new Request.Builder()
                .withHeader("h1", "v1")
                .withUrl("/test")
                .withMethod("get")
                .withResponse(Response.EMPTY);
        assertThat(request.headers().get("h1"), is("v1"));
        assertThat(request.headers().get("invalid"), is(nullValue()));
        assertThat(request.url(), is("/test"));
        assertThat(request.method(), is("get"));
        assertThat(request.response(), is(Response.EMPTY));
    }

    @Test
    public void method_returnsExpectedMethodOrNull() throws Exception {
        Request requestOne = new Gson().fromJson("{method:'get'}", Request.class);
        assertThat(requestOne.method(), is("get"));

        Request requestTwo = new Gson().fromJson("{}", Request.class);
        assertThat(requestTwo.method(), nullValue());
    }

    @Test
    public void url_returnsExpectedUrlOrNull() throws Exception {
        Request requestOne = new Gson().fromJson("{url:'/one'}", Request.class);
        assertThat(requestOne.url(), is("/one"));

        Request requestTwo = new Gson().fromJson("{}", Request.class);
        assertThat(requestTwo.url(), nullValue());
    }

    @Test
    public void response_returnsExpectedResponseOrNull() throws Exception {
        Request requestOne = new Gson().fromJson("{url:'/one', responses:[{text:'{}'}]}", Request.class);
        assertThat(requestOne.response(), is(notNullValue()));

        Request requestTwo = new Gson().fromJson("{url:'/two'}", Request.class);
        assertThat(requestTwo.response(), is(nullValue()));
    }

}
