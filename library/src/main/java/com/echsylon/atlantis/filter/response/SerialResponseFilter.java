package com.echsylon.atlantis.filter.response;

import com.echsylon.atlantis.ResponseFilter;
import com.echsylon.atlantis.internal.Utils;
import com.echsylon.atlantis.template.Request;
import com.echsylon.atlantis.template.Response;

import java.util.HashMap;
import java.util.List;

/**
 * This class implements serial filter behaviour of a {@link ResponseFilter}. It returns the next
 * response from the available ones relative to the previously returned response for a given
 * request, or null if there are no responses to pick from.
 */
public class SerialResponseFilter implements ResponseFilter {
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
