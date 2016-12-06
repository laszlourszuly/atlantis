package com.echsylon.atlantis.filter;

import com.echsylon.atlantis.MockResponse;

import java.util.List;

public class DefaultResponseFilter implements MockResponse.Filter {

    @Override
    public MockResponse findResponse(List<MockResponse> mockResponses) {
        return null;
    }
}
