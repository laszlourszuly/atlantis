package com.echsylon.atlantis;

import com.google.gson.Gson;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Verifies expected behavior on the {@link Configuration} class.
 */
public class ConfigurationTest {

    @Test
    public void config_matchesRequestQueryProperly() throws Exception {
        Configuration config = new Gson().fromJson("{requests: [{" +
                "  headers: { Content-Type: 'text/plain' }, " +
                "  method: 'post', " +
                "  url: 'scheme://host/path?q1=v1&q2=v2'" +
                "}]}", Configuration.class);

        // Verify empty url doesn't return result when there is no matching result.
        assertThat(config.findRequest("", "POST", null), is(nullValue()));

        // Verify query string is matched exactly
        assertThat(config.findRequest("scheme://host/path", "POST", null), is(nullValue()));                    // no query
        assertThat(config.findRequest("scheme://host/path?q1=v1", "POST", null), is(nullValue()));              // too short query
        assertThat(config.findRequest("scheme://host/path?q1=v1&q3=v3", "POST", null), is(nullValue()));        // no matching query
        assertThat(config.findRequest("scheme://host/path?q1=v1&q2=v2&q3=v3", "POST", null), is(nullValue()));  // too long query
        assertThat(config.findRequest("scheme://host/path?q1=v1&q2=v2", "POST", null), is(notNullValue()));     // exact query

        // Verify method is matched
        assertThat(config.findRequest("scheme://host/path?q1=v1&q2=v2", "PUT", null), is(nullValue()));         // wrong method
        assertThat(config.findRequest("scheme://host/path?q1=v1&q2=v2", "POST", null), is(notNullValue()));     // correct method

        // Verify headers are matched
        Map<String, String> noMatchHeaders = new HashMap<>();
        noMatchHeaders.put("no-match-key", "no-match-value");

        HashMap<String, String> exactHeaders = new HashMap<>();
        exactHeaders.put("Content-Type", "text/plain");

        assertThat(config.findRequest("scheme://host/path?q1=v1&q2=v2", "POST", noMatchHeaders), is(nullValue()));          // no matching headers constraint
        assertThat(config.findRequest("scheme://host/path?q1=v1&q2=v2", "POST", Collections.emptyMap()), is(nullValue()));  // no headers expected constraint
        assertThat(config.findRequest("scheme://host/path?q1=v1&q2=v2", "POST", null), is(notNullValue()));                 // ignore headers constraint
        assertThat(config.findRequest("scheme://host/path?q1=v1&q2=v2", "POST", exactHeaders), is(notNullValue()));         // exact headers
    }

    @Test
    public void build_canBuildConfiguration() throws Exception {
        // Verify that something is built.
        assertThat(new Configuration.Builder().build(), is(notNullValue()));

        // Verify fallback url is properly set.
        Configuration configuration1 = new Configuration.Builder().withFallbackBaseUrl("scheme://host2").build();
        assertThat(configuration1.fallbackBaseUrl(), is("scheme://host2"));
        assertThat(configuration1.hasAlternativeRoute(), is(true));

        // Verify requests are properly set.
        Request request = mock(Request.class);
        Configuration configuration2 = new Configuration.Builder().withRequest(request).build();
        assertThat(configuration2.requests, is(notNullValue()));
        assertThat(configuration2.requests.size(), is(1));
        assertThat(configuration2.requests.get(0), is(request));

        // Verify request filter is properly set.
        Request.Filter filter = mock(Request.Filter.class);
        Configuration configuration3 = new Configuration.Builder().withRequestFilter(filter).build();
        assertThat(configuration3.requestFilter, is(notNullValue()));
        assertThat(configuration3.requestFilter, is(filter));
    }

}
