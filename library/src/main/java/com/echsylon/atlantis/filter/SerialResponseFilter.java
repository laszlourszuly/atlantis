package com.echsylon.atlantis.filter;

import com.echsylon.atlantis.MockResponse;

import java.util.List;

/**
 * This class enables serial filter behaviour of a {@link MockResponse.Filter}.
 * It returns the next response from the available ones relative to the
 * previously returned response for a given request, or null if there are no
 * responses to pick from.
 */
public class SerialResponseFilter implements MockResponse.Filter {
    private int index = 0;

    /**
     * Returns the next response for a given request.
     *
     * @param responses All available responses.
     * @return The next response.
     */
    @Override
    public MockResponse findResponse(final List<MockResponse> responses) {
        if (responses == null || responses.isEmpty())
            return null;

        if (++index >= responses.size())
            index = 0;

        return responses.get(index);
    }
}
