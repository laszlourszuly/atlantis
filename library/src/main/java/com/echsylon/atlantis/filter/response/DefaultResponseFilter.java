package com.echsylon.atlantis.filter.response;

import com.echsylon.atlantis.internal.Utils;
import com.echsylon.atlantis.template.Request;
import com.echsylon.atlantis.template.Response;

import java.util.List;

/**
 * This class implements the default behaviour of a {@link Response.Filter}. It returns the first
 * response available or null if there are no responses to pick from.
 */
public class DefaultResponseFilter implements Response.Filter {

    /**
     * Returns the first available response.
     *
     * @param request   The request to find a response for.
     * @param responses All available responses.
     * @return The current response.
     */
    @Override
    public Response getResponse(Request request, List<Response> responses) {
        return Utils.notEmpty(responses) ?
                responses.get(0) :
                null;
    }

}
