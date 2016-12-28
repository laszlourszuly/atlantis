package com.echsylon.atlantis;

import java.util.List;
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
    private final HeaderManager headerManager = new HeaderManager();


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
     * Returns the header manager holding any intercepted request headers.
     *
     * @return The request header manager.
     */
    HeaderManager headerManager() {
        return headerManager;
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
        headerManager.add(key, value);
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(method).append(' ')
                .append(url).append(' ')
                .append(protocol).append('\n');

        if (headerManager.keyCount() > 0) {
            Map<String, List<String>> headers = headerManager.getAllAsMultiMap();
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                String key = entry.getKey();
                for (String value : entry.getValue())
                    stringBuilder.append(key).append(": ").append(value).append('\n');
            }
        }

        return stringBuilder.toString();
    }
}
