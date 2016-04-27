package com.echsylon.atlantis.template;

import com.google.gson.Gson;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Verifies expected behavior on the {@link Response} class.
 */
public class ResponseTest {
    private static final Response GOOD_RESPONSE;
    private static final Response BAD_RESPONSE;

    static {
        GOOD_RESPONSE = new Gson().fromJson("{" +
                "  \"responseCode\": {" +
                "    \"code\": 200, " +
                "    \"name\": \"OK\"" +
                "  }, " +
                "  \"mime\": \"application/json\", " +
                "  \"text\": \"\\\"{}\\\"\", " +
                "  \"headers\": [{" +
                "    \"key\": \"k1\", " +
                "    \"value\": \"v1\"" +
                "  }]" +
                "}", Response.class);

        BAD_RESPONSE = new Gson().fromJson("{}",
                Response.class);
    }

    @Test
    public void testResponseCode() throws Exception {
        assertThat(GOOD_RESPONSE.statusCode(), is(200));
        assertThat(BAD_RESPONSE.statusCode(), is(0));
    }

    @Test
    public void testHeaders() throws Exception {
        assertThat(GOOD_RESPONSE.headers(), notNullValue());
        assertThat(GOOD_RESPONSE.headers().size(), is(1));
        assertThat(GOOD_RESPONSE.headers("k1"), notNullValue());
        assertThat(GOOD_RESPONSE.headers("k1").size(), is(1));
        assertThat(GOOD_RESPONSE.headers("invalid_key"), notNullValue());
        assertThat(GOOD_RESPONSE.headers("invalid_key").size(), is(0));

        assertThat(BAD_RESPONSE.headers(), notNullValue());
        assertThat(BAD_RESPONSE.headers().size(), is(0));
        assertThat(BAD_RESPONSE.headers("invalid_key"), notNullValue());
        assertThat(BAD_RESPONSE.headers("invalid_key").size(), is(0));
    }

    @Test
    public void testMimeType() throws Exception {
        assertThat(GOOD_RESPONSE.mimeType(), is("application/json"));
        assertThat(BAD_RESPONSE.mimeType(), nullValue());
    }

    @Test
    public void testContent() throws Exception {
        assertThat(GOOD_RESPONSE.content(), is("\"{}\""));
        assertThat(BAD_RESPONSE.content(), nullValue());
    }

}
