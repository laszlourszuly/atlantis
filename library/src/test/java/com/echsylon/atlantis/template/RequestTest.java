package com.echsylon.atlantis.template;

import com.google.gson.Gson;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Verifies expected behavior on the {@link Response} class.
 */
public class RequestTest {

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
    public void response_neverReturnsNullResponse() throws Exception {
        Request requestOne = new Gson().fromJson("{url:'/one', responses:[{text:'{}'}]}", Request.class);
        assertThat(requestOne.response(), notNullValue());

        Request requestTwo = new Gson().fromJson("{url:'/two'}", Request.class);
        assertThat(requestTwo.response(), notNullValue());
    }

}
