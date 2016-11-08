package com.echsylon.atlantis.internal.json;

import com.echsylon.atlantis.Configuration;
import com.echsylon.atlantis.Request;
import com.echsylon.atlantis.Response;

import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Verifies expected behavior on the {@link JsonParser} class.
 */
public class JsonParserTest {
    private static final Response MOCK_RESPONSE = new Response.Builder();
    private static final Request MOCK_REQUEST = new Request.Builder();

    @Test
    @SuppressWarnings("ConstantConditions")
    public void read_canParseStringHeadersCorrectly() throws Exception {
        validateRequestHeaders(new JsonParser().fromJson("{headers: 'Content-Type: text/plain'}", Request.class));
        validateResponseHeaders(new JsonParser().fromJson("{headers: 'Content-Type: text/plain'}", Response.class));
    }

    @Test
    public void read_canParseDictionaryHeadersCorrectly() throws Exception {
        validateRequestHeaders(new JsonParser().fromJson("{headers: {Content-Type: 'text/plain'}}", Request.class));
        validateResponseHeaders(new JsonParser().fromJson("{headers: {Content-Type: 'text/plain'}}", Response.class));
    }

    @Test
    public void read_canParseObjectArrayHeadersCorrectly() throws Exception {
        validateRequestHeaders(new JsonParser().fromJson("{headers: [{key: 'Content-Type', value: 'text/plain'}]}", Request.class));
        validateResponseHeaders(new JsonParser().fromJson("{headers: [{key: 'Content-Type', value: 'text/plain'}]}", Response.class));
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

    @Test
    public void read_canParseRequestFilterProperly() throws Exception {
        // Verify custom behavior if custom request filter is defined.
        Configuration configuration1 = new JsonParser().fromJson("{requestFilter: 'com.echsylon.atlantis.internal.json.JsonParserTest$TestRequestFilter'}", Configuration.class);
        assertThat(configuration1.findRequest(null, null, null), is(MOCK_REQUEST));

        // Verify default behavior if no specific request filter is defined.
        Configuration configuration2 = new JsonParser().fromJson("{}", Configuration.class);
        assertThat(configuration2.findRequest(null, null, null), is(nullValue()));
    }

    @Test
    public void read_canParseResponseFilterProperly() throws Exception {
        // Verify custom behavior if custom response filter is defined.
        Request request1 = new JsonParser().fromJson("{responseFilter: 'com.echsylon.atlantis.internal.json.JsonParserTest$TestResponseFilter'}", Request.class);
        assertThat(request1.response(), is(MOCK_RESPONSE));

        // Verify default behavior if no specific response filter is defined.
        Request request2 = new JsonParser().fromJson("{}", Request.class);
        assertThat(request2.response(), is(nullValue()));
    }

    @Test(expected = JsonParser.JsonException.class)
    public void read_willThrowJsonExceptionOnIllegalRequestFilter() throws Exception {
        new JsonParser().fromJson("{requestFilter: 'illegal.RequestFilter'}", Configuration.class);
    }

    @Test(expected = JsonParser.JsonException.class)
    public void read_willThrowJsonExceptionOnIllegalResponseFilter() throws Exception {
        new JsonParser().fromJson("{responseFilter: 'illegal.ResponseFilter'}", Request.class);
    }

    private void validateRequestHeaders(Request entity) {
        assertThat(entity, notNullValue());
        assertThat(entity.headers(), notNullValue());
        assertThat(entity.headers().size(), is(1));
        assertThat(entity.headers().get("Content-Type"), is("text/plain"));
        assertThat(entity.headers().get("Invalid-Header"), is(nullValue()));
    }

    private void validateResponseHeaders(Response entity) {
        assertThat(entity, notNullValue());
        assertThat(entity.headers(), notNullValue());
        assertThat(entity.headers().size(), is(1));
        assertThat(entity.headers().get("Content-Type"), is("text/plain"));
        assertThat(entity.headers().get("Invalid-Header"), is(nullValue()));
    }

    public static final class TestResponseFilter implements Response.Filter {
        @Override
        public Response getResponse(Request request, List<Response> responses) {
            return MOCK_RESPONSE;
        }
    }

    public static final class TestRequestFilter implements Request.Filter {
        @Override
        public Request getRequest(List<Request> requests, String url, String method, Map<String, String> headers) {
            return MOCK_REQUEST;
        }
    }

}
