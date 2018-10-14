package com.echsylon.atlantis.filter;

import com.echsylon.atlantis.MockResponse;

import java.util.List;
import java.util.Random;

/**
 * This class implements random filter behaviour of a {@link
 * MockResponse.Filter}. It returns a random response from the available ones,
 * or null if there are no responses to pick from.
 */
public class RandomResponseFilter implements MockResponse.Filter {

    /**
     * Returns a random response from the available set.
     *
     * @param responses All available responses.
     * @return The current response.
     */
    @Override
    public MockResponse findResponse(final List<MockResponse> responses) {
        return responses != null && !responses.isEmpty() ?
                responses.get(new Random().nextInt(responses.size())) :
                null;
    }
}
