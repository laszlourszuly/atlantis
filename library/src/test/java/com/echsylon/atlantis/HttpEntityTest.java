package com.echsylon.atlantis;

import com.google.gson.Gson;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Verifies expected behavior on any class implementing the {@link HttpEntity}
 * feature.
 */
public class HttpEntityTest {

    @Test
    public void headers_providesNonNullHeaders() throws Exception {
        HttpEntity httpEntityOne = new Gson().fromJson("{headers: {Content-Type: 'text/plain'}}", HttpEntity.class);
        assertThat(httpEntityOne.headers(), notNullValue());

        HttpEntity httpEntityTwo = new Gson().fromJson("{}", HttpEntity.class);
        assertThat(httpEntityTwo.headers(), notNullValue());
    }

    @Test
    public void headers_providesExpectedHeaders() throws Exception {
        HttpEntity httpEntityOne = new Gson().fromJson("{headers: {Content-Type: 'text/plain'}}", HttpEntity.class);
        assertThat(httpEntityOne.headers().size(), is(1));
        assertThat(httpEntityOne.headers().get("Content-Type"), is("text/plain"));
        assertThat(httpEntityOne.headers().get("Invalid-Header"), is(nullValue()));
    }

}
