package com.echsylon.atlantis.filter;

import com.echsylon.atlantis.Request;
import com.echsylon.atlantis.Response;
import com.echsylon.atlantis.internal.Utils;

import java.util.List;
import java.util.Random;

/**
 * This class implements random filter behaviour of a {@link Response.Filter}.
 * It returns a random response from the available ones, or null if there are no
 * responses to pick from.
 */
public class RandomResponseFilter implements Response.Filter {

    /**
     * Returns a random response from the available set.
     *
     * @param request   The request to find a response for.
     * @param responses All available responses.
     * @return The current response.
     */
    @Override
    public Response getResponse(Request request, List<Response> responses) {
        return Utils.notEmpty(responses) ?
                responses.get(new Random().nextInt(responses.size())) :
                null;
    }

}
