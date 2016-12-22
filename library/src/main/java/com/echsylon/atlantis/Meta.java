package com.echsylon.atlantis;

import java.util.Collections;
import java.util.Map;

import static com.echsylon.atlantis.Utils.getNonNull;

/**
 * This class represents the intercepted meta data for a request from the HTTP
 * client. This information should be enough to identify a request template in
 * the Atlantis context and provide a mocked response for it.
 */
class Meta {
    private String method;
    private String url;
    private String protocol;
    private final HeaderManager headers = new HeaderManager();


    /**
     * Returns the intercepted request method.
     *
     * @return The HTTP method e.g. "GET" .
     */
    String method() {
        return getNonNull(method, "");
    }

    /**
     * Returns the intercepted request url.
     *
     * @return The url string e.g. "/path/to/resource".
     */
    String url() {
        return getNonNull(url, "");
    }

    /**
     * Returns the intercepted request protocol.
     *
     * @return The request protocol e.g. "HTTP/1.1".
     */
    String protocol() {
        return getNonNull(protocol, "");
    }

    /**
     * Returns any intercepted request headers.
     *
     * @return The request headers. The map may be empty but never null.
     */
    Map<String, String> headers() {
        return Collections.unmodifiableMap(headers);
    }


    /**
     * Sets the intercepted method.
     *
     * @param method The HTTP method.
     */
    void setMethod(final String method) {
        this.method = method;
    }

    /**
     * Sets the intercepted url.
     *
     * @param url The url.
     */
    void setUrl(final String url) {
        this.url = url;
    }

    /**
     * Sets the intercepted protocol.
     *
     * @param protocol The protocol.
     */
    void setProtocol(final String protocol) {
        this.protocol = protocol;
    }

    /**
     * Adds an intercepted header.
     *
     * @param key   The header key.
     * @param value The header value.
     */
    void addHeader(final String key, final String value) {
        headers.put(key, value);
    }

    /**
     * Returns a boolean flag indicating whether there is a "Content-Length"
     * header with a value greater than 0.
     *
     * @return Boolean true if there is a "Content-Length" header and a
     * corresponding value greater than 0, false otherwise.
     */
    boolean isExpectedToHaveBody() {
        return headers.isExpectedToHaveBody();
    }

    /**
     * Returns a boolean flag indicating whether there is an "Expect" header
     * with a "100-continue" value.
     *
     * @return Boolean true if there is an "Expect" header and a corresponding
     * "100-Continue" value, false otherwise.
     */
    boolean isExpectedToContinue() {
        return headers.isExpectedToContinue();
    }

    /**
     * Returns a boolean flag indicating whether there is a "Transfer-Encoding"
     * header with a "chunked" value.
     *
     * @return Boolean true if there is a "Transfer-Encoding" header and a
     * corresponding "chunked" value, false otherwise.
     */
    boolean isExpectedToBeChunked() {
        return headers.isExpectedToBeChunked();
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(method).append(' ')
                .append(url).append(' ')
                .append(protocol).append('\n');

        for (Map.Entry<String, String> entry : headers.entrySet())
            stringBuilder.append(entry.getKey()).append(": ")
                    .append(entry.getValue()).append('\n');

        return stringBuilder.toString();
    }
}
