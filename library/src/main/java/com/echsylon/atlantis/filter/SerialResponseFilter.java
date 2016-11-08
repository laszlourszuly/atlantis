package com.echsylon.atlantis.filter;

import com.echsylon.atlantis.Request;
import com.echsylon.atlantis.Response;
import com.echsylon.atlantis.internal.Utils;

import java.util.HashMap;
import java.util.List;

/**
 * This class implements serial filter behaviour of a {@link Response.Filter}.
 * It returns the next response from the available ones relative to the
 * previously returned response for a given request, or null if there are no
 * responses to pick from.
 */
public class SerialResponseFilter implements Response.Filter {
    private final HashMap<Request, Integer> indexMap = new HashMap<>();

    /**
     * Returns the next response for a given request.
     *
     * @param request   The request to find a response for.
     * @param responses All available responses.
     * @return The next response.
     */
    @Override
    public Response getResponse(Request request, List<Response> responses) {
        if (Utils.isEmpty(responses))
            return null;

        if (!indexMap.containsKey(request))
            indexMap.put(request, 0);

        int index = indexMap.get(request);
        if (++index >= responses.size())
            index = 0;

        indexMap.put(request, index);
        return responses.get(index);
    }

}
