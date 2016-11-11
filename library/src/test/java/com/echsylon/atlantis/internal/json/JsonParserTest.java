package com.echsylon.atlantis.internal.json;

import com.echsylon.atlantis.Configuration;
import com.echsylon.atlantis.Request;
import com.echsylon.atlantis.Response;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.isA;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Verifies expected behavior on the {@link JsonParser} class.
 */
public class JsonParserTest {

    @SuppressWarnings("unused")
    public static final class TestResponseFilter implements Response.Filter {
        @Override
        public Response getResponse(Request request, List<Response> responses) {
            return null;
        }
    }

    @SuppressWarnings("unused")
    public static final class TestRequestFilter implements Request.Filter {
        @Override
        public Request getRequest(List<Request> requests, String url, String method, Map<String, String> headers) {
            return null;
        }
    }

    @Test
    public void read_canParseStringHeadersForRequest() throws Exception {
        Request request = new JsonParser().fromJson("{headers: 'h1: v1 \n h2: v2'}", Request.class);
        assertThat(request.headers(), notNullValue());
        assertThat(request.headers().size(), is(2));
        assertThat(request.headers().get("h1"), is("v1"));
        assertThat(request.headers().get("h2"), is("v2"));
    }

    @Test
    public void read_canParseStringHeadersForResponse() throws Exception {
        Response response = new JsonParser().fromJson("{headers: 'h1: v1 \n h2: v2'}", Response.class);
        assertThat(response.headers(), notNullValue());
        assertThat(response.headers().size(), is(2));
        assertThat(response.headers().get("h1"), is("v1"));
        assertThat(response.headers().get("h2"), is("v2"));
    }

    @Test
    public void read_canParseDictionaryHeadersForRequest() throws Exception {
        Request request = new JsonParser().fromJson("{headers: { h1: 'v1', h2: 'v2' }}", Request.class);
        assertThat(request.headers(), notNullValue());
        assertThat(request.headers().size(), is(2));
        assertThat(request.headers().get("h1"), is("v1"));
        assertThat(request.headers().get("h2"), is("v2"));
    }

    @Test
    public void read_canParseDictionaryHeadersForResponse() throws Exception {
        Response response = new JsonParser().fromJson("{headers: { h1: 'v1', h2: 'v2' }}", Response.class);
        assertThat(response.headers(), notNullValue());
        assertThat(response.headers().size(), is(2));
        assertThat(response.headers().get("h1"), is("v1"));
        assertThat(response.headers().get("h2"), is("v2"));
    }

    @Test
    public void read_canParseObjectArrayHeadersForRequest() throws Exception {
        Request request = new JsonParser().fromJson("{headers: [{ key: 'h1', value: 'v1' }, { key: 'h2', value: 'v2' }]}", Request.class);
        assertThat(request.headers(), notNullValue());
        assertThat(request.headers().size(), is(2));
        assertThat(request.headers().get("h1"), is("v1"));
        assertThat(request.headers().get("h2"), is("v2"));
    }

    @Test
    public void read_canParseObjectArrayHeadersForResponse() throws Exception {
        Response response = new JsonParser().fromJson("{headers: [{ key: 'h1', value: 'v1' }, { key: 'h2', value: 'v2' }]}", Response.class);
        assertThat(response.headers(), notNullValue());
        assertThat(response.headers().size(), is(2));
        assertThat(response.headers().get("h1"), is("v1"));
        assertThat(response.headers().get("h2"), is("v2"));
    }

    @Test
    public void read_providesProperDefaultRequestHeadersIfNoneParsed() throws Exception {
        Request request = new JsonParser().fromJson("{}", Request.class);
        assertThat(request.headers(), notNullValue());
        assertThat(request.headers().size(), is(0));
    }

    @Test
    public void read_providesProperDefaultResponseHeadersIfNoneParsed() throws Exception {
        Response response = new JsonParser().fromJson("{}", Response.class);
        assertThat(response.headers(), notNullValue());
        assertThat(response.headers().size(), is(0));
    }

    @Test
    public void read_canParseCustomRequestFilter() throws Exception {
        Configuration configuration = new JsonParser().fromJson("{requestFilter: 'com.echsylon.atlantis.internal.json.JsonParserTest$TestRequestFilter'}", Configuration.class);
        assertThat(configuration.requestFilter(), isA(Request.Filter.class));
    }

    @Test
    public void read_canParseCustomResponseFilter() throws Exception {
        Request request = new JsonParser().fromJson("{responseFilter: 'com.echsylon.atlantis.internal.json.JsonParserTest$TestResponseFilter'}", Request.class);
        assertThat(request.responseFilter(), isA(Response.Filter.class));
    }

    @Test
    public void read_willThrowJsonExceptionOnIllegalRequestFilter() throws Exception {
        assertThatThrownBy(() -> new JsonParser().fromJson("{requestFilter: 'illegal.RequestFilter'}", Configuration.class))
                .isInstanceOf(JsonParser.JsonException.class);
    }

    @Test
    public void read_willThrowJsonExceptionOnIllegalResponseFilter() throws Exception {
        assertThatThrownBy(() -> new JsonParser().fromJson("{responseFilter: 'illegal.ResponseFilter'}", Request.class))
                .isInstanceOf(JsonParser.JsonException.class);
    }

    @Test
    public void write_canSerializeRequest() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("h0", "v0");

        Request request = new Request.Builder()
                .withResponseFilter(new TestResponseFilter())
                .withResponse(new Response.Builder().build())
                .withHeaders(headers)
                .withHeader("h1", "v1")
                .withMethod("method")
                .withUrl("url")
                .build();

        String json = new JsonParser().toJson(request, Request.class);
        JsonObject jsonObject = new com.google.gson.JsonParser().parse(json).getAsJsonObject();

        assertThat(jsonObject.get("url").getAsString(), is("url"));
        assertThat(jsonObject.get("method").getAsString(), is("method"));
        assertThat(jsonObject.get("responseFilter").getAsString(), is("com.echsylon.atlantis.internal.json.JsonParserTest$TestResponseFilter"));

        JsonArray serializedResponses = jsonObject.get("responses").getAsJsonArray();
        assertThat(serializedResponses.size(), is(1));

        JsonObject serializedHeaders = jsonObject.get("headers").getAsJsonObject();
        assertThat(serializedHeaders.size(), is(2));
        assertThat(serializedHeaders.get("h0").getAsString(), is("v0"));
        assertThat(serializedHeaders.get("h1").getAsString(), is("v1"));
    }

    @Test
    public void write_canSerializeResponse() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("h0", "v0");

        Response response = new Response.Builder()
                .withHeaders(headers)
                .withHeader("h1", "v1")
                .withAsset("asset".getBytes())
                .withAsset("file://asset")
                .withContent("text")
                .withDelay(1, 2)
                .withMimeType("mime")
                .withStatus(2, "CUSTOM_OK")
                .build();

        String json = new JsonParser().toJson(response, Response.class);
        JsonObject jsonObject = new com.google.gson.JsonParser().parse(json).getAsJsonObject();

        assertThat(jsonObject.has("assetBytes"), is(false));
        assertThat(jsonObject.get("asset").getAsString(), is("file://asset"));
        assertThat(jsonObject.get("mime").getAsString(), is("mime"));
        assertThat(jsonObject.get("text").getAsString(), is("text"));
        assertThat(jsonObject.get("delay").getAsInt(), is(1));
        assertThat(jsonObject.get("maxDelay").getAsInt(), is(2));

        JsonObject responseCode = jsonObject.get("responseCode").getAsJsonObject();
        assertThat(responseCode.get("code").getAsInt(), is(2));
        assertThat(responseCode.get("name").getAsString(), is("CUSTOM_OK"));

        JsonObject serializedHeaders = jsonObject.get("headers").getAsJsonObject();
        assertThat(serializedHeaders.size(), is(2));
        assertThat(serializedHeaders.get("h0").getAsString(), is("v0"));
        assertThat(serializedHeaders.get("h1").getAsString(), is("v1"));
    }

    @Test
    public void write_canSerializeConfiguration() {
        Configuration configuration = new Configuration.Builder()
                .withFallbackBaseUrl("fallbackUrl")
                .withRequestFilter(new TestRequestFilter())
                .withRequest(new Request.Builder().build())
                .build();

        String json = new JsonParser().toJson(configuration, Configuration.class);
        JsonObject jsonObject = new com.google.gson.JsonParser().parse(json).getAsJsonObject();

        assertThat(jsonObject.get("fallbackBaseUrl").getAsString(), is("fallbackUrl"));
        assertThat(jsonObject.get("requestFilter").getAsString(), is("com.echsylon.atlantis.internal.json.JsonParserTest$TestRequestFilter"));

        JsonArray serializedRequests = jsonObject.get("requests").getAsJsonArray();
        assertThat(serializedRequests.size(), is(1));
    }

}
