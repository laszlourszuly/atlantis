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
                "  \"headers\": \"Content-Type: text/plain\n\", " +
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
    public void testHeaders() throws Exception {
        assertThat(GOOD_REQUEST.headers(), notNullValue());
        assertThat(GOOD_REQUEST.headers().size(), is(1));
        assertThat(GOOD_REQUEST.headers().get(0).key(), is("Content-Type"));
        assertThat(GOOD_REQUEST.headers().get(0).value(), is("text/plain"));

        assertThat(BAD_REQUEST.headers(), notNullValue());
        assertThat(BAD_REQUEST.headers().size(), is(0));
    }

    @Test
    public void testMethod() throws Exception {
        assertThat(GOOD_REQUEST.method(), is("POST"));
        assertThat(BAD_REQUEST.method(), nullValue());
    }

    @Test
    public void testUrl() throws Exception {
        assertThat(GOOD_REQUEST.url(), is("scheme://host/path?q1=v1&q2=v2"));
        assertThat(BAD_REQUEST.url(), nullValue());
    }

    @Test
    public void testResponse() throws Exception {
        assertThat(GOOD_REQUEST.response(), notNullValue());
        assertThat(BAD_REQUEST.response(), notNullValue());
    }

}
