package com.echsylon.atlantis.filter;

import com.echsylon.atlantis.MockRequest;

import java.util.List;
import java.util.Map;

public class DefaultRequestFilter implements MockRequest.Filter {

    @Override
    public MockRequest findRequest(final String method,
                                   final String url,
                                   final Map<String, String> headers,
                                   final List<MockRequest> mockRequests) {
        return null;
    }
}
