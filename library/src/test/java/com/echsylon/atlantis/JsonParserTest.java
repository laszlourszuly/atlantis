package com.echsylon.atlantis;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class JsonParserTest {

    @Test
    public void public_canParseSettingsManager() {
        String json = "{" +
                "\"followRedirects\": false," +
                "\"fallbackBaseUrl\": \"abc\"," +
                "\"throttleByteCount\": 1," +
                "\"throttleMinDelayMillis\": 2," +
                "\"throttleMaxDelayMillis\": 3" +
                "}";

        SettingsManager result = JsonParser.fromJson(json, SettingsManager.class);
        assertThat(result.entryCount(), is(5));
        assertThat(result.followRedirects(), is(false));
        assertThat(result.fallbackBaseUrl(), is("abc"));
        assertThat(result.throttleByteCount(), is(1L));
        assertThat(result.throttleDelayMillis(), anyOf(is(2L), is(3L)));
    }

    @Test
    public void public_canParseMockResponse() {
        String json = "{" +
                "\"code\": 200," +
                "\"phrase\": \"OK\"," +
                "\"settings\": {" +
                "\"key\": \"value\"" +
                "}}";

        MockResponse result = JsonParser.fromJson(json, MockResponse.class);
        assertThat(result.code(), is(200));
        assertThat(result.phrase(), is("OK"));
        assertThat(result.settingsManager(), is(notNullValue()));
    }

    @Test
    public void public_canParseMockRequest() {
        String json = "{" +
                "\"method\": \"PUT\"," +
                "\"url\": \"/path/to/resource\"," +
                "\"settings\": {" +
                "\"key\": \"value\"" +
                "}," +
                "\"responses\": [{" +
                "\"code\": 204," +
                "\"phrase\": \"No Content\"," +
                "\"settings\": {" +
                "\"key\": \"value\"" +
                "}}]}";

        MockRequest result = JsonParser.fromJson(json, MockRequest.class);

        assertThat(result.method(), is("PUT"));
        assertThat(result.url(), is("/path/to/resource"));
        assertThat(result.settingsManager(), is(notNullValue()));
        assertThat(result.responses().size(), is(1));

        MockResponse response = result.response();
        assertThat(response.code(), is(204));
        assertThat(response.phrase(), is("No Content"));
        assertThat(response.settingsManager(), is(notNullValue()));
    }
}
