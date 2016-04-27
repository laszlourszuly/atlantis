package com.echsylon.atlantis.template;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Verifies expected behavior on the {@link Header} class.
 */
public class HeaderTest {

    @Test
    public void testHeaders() throws Exception {
        Header header1 = new Header("KEY", "VALUE");
        Header header2 = new Header("key", "value");
        Header header3 = new Header("k", "v");

        assertThat(header1.key(), is("KEY"));
        assertThat(header1.value(), is("VALUE"));
        assertThat(header1, is(equalTo(header2)));
        assertThat(header1, is(not(equalTo(header3))));
    }

}
