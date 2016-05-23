package com.echsylon.atlantis.template;

import com.google.gson.Gson;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Verifies expected behavior on the {@link Configuration} class.
 */
public class ConfigurationTest {

    @Test
    @SuppressWarnings({"unchecked", "ArraysAsListWithZeroOrOneArgument"})
    public void config_FindResponse() throws Exception {
        Configuration config = new Gson().fromJson("{requests: [{" +
                "  headers: { Content-Type: 'text/plain' }, " +
                "  method: 'post', " +
                "  url: 'scheme://host/path?q1=v1&q2=v2', " +
                "  responses: [{" +
                "    responseCode: { code: 200, name: 'OK' }, " +
                "    mime: 'application/json', " +
                "    text: '{}' " +
                "  }]" +
                "}]}", Configuration.class);

        // Verify empty url doesn't return result when there is no matching result.
        assertThat(config.findRequest("", "POST", null), is(nullValue()));

        // Verify query string is matched exactly
        assertThat(config.findRequest("scheme://host/path", "POST", null), is(nullValue()));                    // no query
        assertThat(config.findRequest("scheme://host/path?q1=v1", "POST", null), is(nullValue()));              // too short query
        assertThat(config.findRequest("scheme://host/path?q1=v1&q3=v3", "POST", null), is(nullValue()));        // no matching query
        assertThat(config.findRequest("scheme://host/path?q1=v1&q2=v2&q3=v3", "POST", null), is(nullValue()));  // too long query

        // Verify method is matched
        assertThat(config.findRequest("scheme://host/path?q1=v1&q2=v2", "PUT", null), is(nullValue()));

        // Verify headers are matched if given (even if empty)
        assertThat(config.findRequest("scheme://host/path?q1=v1&q2=v2", "POST", Collections.EMPTY_MAP), is(nullValue()));

        // Verify success
        HashMap<String, String> exactHeaders = new HashMap<>();
        exactHeaders.put("Content-Type", "text/plain");
        assertThat(config.findRequest("scheme://host/path?q1=v1&q2=v2", "POST", null), is(notNullValue()));         // ignore headers
        assertThat(config.findRequest("scheme://host/path?q1=v1&q2=v2", "POST", exactHeaders), is(notNullValue())); // exact headers
    }

}
