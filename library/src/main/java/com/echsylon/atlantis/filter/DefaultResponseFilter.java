package com.echsylon.atlantis.filter;

import com.echsylon.atlantis.MockResponse;

import java.util.List;

public class DefaultResponseFilter implements MockResponse.Filter {

    /**
     * Returns the first mock response in the given list.
     *
     * @param mockResponses All available mockResponse to pick a candidate from.
     *                      Null and empty lists are handled gracefully.
     * @return The first mock response or null if the list is null or empty.
     */
    @Override
    public MockResponse findResponse(List<MockResponse> mockResponses) {
        return mockResponses != null && !mockResponses.isEmpty() ?
                mockResponses.get(0) :
                null;
    }
}
