package com.echsylon.atlantis.template;

import com.echsylon.atlantis.internal.json.JsonParser;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Verifies expected behavior on any class implementing the {@link HeaderContainer} feature.
 */
public class HeaderTest {

    @Test
    public void headers_canParseStringHeadersCorrectly() throws Exception {
        HeaderContainer headerContainer = new JsonParser()
                .fromJson("{\"headers\": \"Content-Type: text/plain\"}", Request.class);

        assertThat(headerContainer.headers(), notNullValue());
        assertThat(headerContainer.headers().size(), is(1));
        assertThat(headerContainer.headers().get("Content-Type"), is("text/plain"));
        assertThat(headerContainer.headers().get("Invalid-Header"), is(nullValue()));
    }

    @Test
    public void headers_canParseDictionaryHeadersCorrectly() throws Exception {
        HeaderContainer headerContainer = new JsonParser()
                .fromJson("{\"headers\": {\"Content-Type\": \"text/plain\"}}", Request.class);

        assertThat(headerContainer.headers(), notNullValue());
        assertThat(headerContainer.headers().size(), is(1));
        assertThat(headerContainer.headers().get("Content-Type"), is("text/plain"));
        assertThat(headerContainer.headers().get("Invalid-Header"), is(nullValue()));
    }

    @Test
    public void headers_canParseObjectArrayHeadersCorrectly() throws Exception {
        HeaderContainer headerContainer = new JsonParser()
                .fromJson("{\"headers\": [{\"key\": \"Content-Type\", \"value\": \"text/plain\"}]}", Request.class);

        assertThat(headerContainer.headers(), notNullValue());
        assertThat(headerContainer.headers().size(), is(1));
        assertThat(headerContainer.headers().get("Content-Type"), is("text/plain"));
        assertThat(headerContainer.headers().get("Invalid-Header"), is(nullValue()));
    }

    @Test
    public void header_providesProperDefaultHeadersIfNoneParsed() throws Exception {
        HeaderContainer headerContainer = new JsonParser()
                .fromJson("{}", Request.class);

        assertThat(headerContainer.headers(), notNullValue());
        assertThat(headerContainer.headers().size(), is(0));
        assertThat(headerContainer.headers().get("Any-Header"), is(nullValue()));
    }
}
