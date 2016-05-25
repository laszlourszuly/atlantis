package com.echsylon.atlantis.internal.json;

import com.echsylon.atlantis.Request;
import com.echsylon.atlantis.Response;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Verifies expected behavior on the {@link JsonParser} class.
 */
public class JsonParserTest {

    @Test
    @SuppressWarnings("ConstantConditions")
    public void read_canParseStringHeadersCorrectly() throws Exception {
        validate(new JsonParser().fromJson("{headers: 'Content-Type: text/plain'}", Request.class));
        validate(new JsonParser().fromJson("{headers: 'Content-Type: text/plain'}", Response.class));
    }

    @Test
    public void read_canParseDictionaryHeadersCorrectly() throws Exception {
        validate(new JsonParser().fromJson("{headers: {Content-Type: 'text/plain'}}", Request.class));
        validate(new JsonParser().fromJson("{headers: {Content-Type: 'text/plain'}}", Response.class));
    }

    @Test
    public void read_canParseObjectArrayHeadersCorrectly() throws Exception {
        validate(new JsonParser().fromJson("{headers: [{key: 'Content-Type', value: 'text/plain'}]}", Request.class));
        validate(new JsonParser().fromJson("{headers: [{key: 'Content-Type', value: 'text/plain'}]}", Response.class));
    }

    @Test
    public void read_providesProperDefaultHeadersIfNoneParsed() throws Exception {
        Request request = new JsonParser().fromJson("{}", Request.class);
        assertThat(request.headers(), notNullValue());
        assertThat(request.headers().size(), is(0));
        assertThat(request.headers().get("Any-Header"), is(nullValue()));
        Response response = new JsonParser().fromJson("{}", Response.class);
        assertThat(response.headers(), notNullValue());
        assertThat(response.headers().size(), is(0));
        assertThat(response.headers().get("Any-Header"), is(nullValue()));
    }

    private void validate(Request entity) {
        assertThat(entity, notNullValue());
        assertThat(entity.headers(), notNullValue());
        assertThat(entity.headers().size(), is(1));
        assertThat(entity.headers().get("Content-Type"), is("text/plain"));
        assertThat(entity.headers().get("Invalid-Header"), is(nullValue()));
    }

    private void validate(Response entity) {
        assertThat(entity, notNullValue());
        assertThat(entity.headers(), notNullValue());
        assertThat(entity.headers().size(), is(1));
        assertThat(entity.headers().get("Content-Type"), is("text/plain"));
        assertThat(entity.headers().get("Invalid-Header"), is(nullValue()));
    }
}
