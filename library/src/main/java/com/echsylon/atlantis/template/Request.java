package com.echsylon.atlantis.template;

import java.io.Serializable;

/**
 * This class represents a request template as the {@link Atlantis} local web server expects it. It
 * will use this template when trying to identify which response to serve to a user provided
 * request.
 */
public class Request extends HeaderContainer implements Serializable {
    private final String method = null;
    private final String url = null;
    private final Response[] responses = null;

    /**
     * Returns the method of this request.
     *
     * @return The request method.
     */
    public String method() {
        return method;
    }

    /**
     * Returns the request url.
     *
     * @return The request url.
     */
    public String url() {
        return url;
    }

    /**
     * Returns the corresponding response for this request.
     *
     * @return The response.
     */
    @SuppressWarnings("ConstantConditions")
    public Response response() {
        return responses != null && responses.length > 0 ?
                responses[0] :
                Response.EMPTY;
    }

}
