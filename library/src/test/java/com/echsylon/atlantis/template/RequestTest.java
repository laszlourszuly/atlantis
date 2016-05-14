package com.echsylon.atlantis.template;

import com.echsylon.atlantis.internal.json.JsonParser;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Verifies expected behavior on the {@link Response} class.
 */
public class RequestTest {
    private static final Request GOOD_REQUEST;
    private static final Request BAD_REQUEST;

    static {
        GOOD_REQUEST = new JsonParser().fromJson("{" +
                "  \"method\": \"POST\", " +
                "  \"url\": \"scheme://host/path?q1=v1&q2=v2\", " +
                "  \"responses\": [{" +
                "    \"responseCode\": {" +
                "      \"code\": 200, " +
                "      \"name\": \"OK\"" +
                "    }, " +
                "    \"mime\": \"application/json\", " +
                "    \"text\": \"\\\"{}\\\"\"" +
                "  }]" +
                "}", Request.class);

        BAD_REQUEST = new JsonParser().fromJson("{}", Request.class);
    }

    @Test
    public void method_returnsExpectedMethodOrNull() throws Exception {
        assertThat(GOOD_REQUEST.method(), is("POST"));
        assertThat(BAD_REQUEST.method(), nullValue());
    }

    @Test
    public void url_returnsExpectedUrlOrNull() throws Exception {
        assertThat(GOOD_REQUEST.url(), is("scheme://host/path?q1=v1&q2=v2"));
        assertThat(BAD_REQUEST.url(), nullValue());
    }

    @Test
    public void response_neverReturnsNull() throws Exception {
        assertThat(GOOD_REQUEST.response(), notNullValue());
        assertThat(BAD_REQUEST.response(), notNullValue());
    }

}
