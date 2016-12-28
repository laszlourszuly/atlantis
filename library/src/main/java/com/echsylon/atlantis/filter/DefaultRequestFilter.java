package com.echsylon.atlantis.filter;

import com.echsylon.atlantis.MockRequest;

import java.util.List;
import java.util.Map;
import java.util.regex.PatternSyntaxException;

public class DefaultRequestFilter implements MockRequest.Filter {

    /**
     * Tries to find a request template that matches a given set of request
     * parameters. The template (if found one) can then be used to pick a mock
     * response from.
     *
     * @param method   The request method ("GET", "POST", etc).
     * @param url      The request url (e.g. "/path/to/resource").
     * @param headers  The request headers manager.
     * @param requests All request templates available.
     * @return A request template that matches the given criteria or null.
     */
    @Override
    public MockRequest findRequest(final String method,
                                   final String url,
                                   final Map<String, String> headers,
                                   final List<MockRequest> requests) {

        if (requests == null || requests.isEmpty())
            return null;

        // Find a template that matches the given request parts.
        for (MockRequest template : requests) {
            if (!method.equalsIgnoreCase(template.method()))
                continue;

            if (!matchesUrlPattern(url, template.url()))
                continue;

            if (!hasRequiredHeaders(headers, template.headerManager().getAllAsMap()))
                continue;

            return template;
        }

        // There is no match. Try not to cry. Cry a lot.
        return null;
    }

    /**
     * Performs a regex validation on the given url.
     *
     * @param url   The url to test.
     * @param regex The regular expression to match.
     * @return Boolean true if the url passes the test, false otherwise.
     */
    private boolean matchesUrlPattern(final String url, final String regex) {
        try {
            return url.matches(regex);
        } catch (PatternSyntaxException | NullPointerException e) {
            return false;
        }
    }

    /**
     * Verifies that all {@code reference} headers exist in the {@code test}
     * map. The verification tests both keys and values, is case sensitive, but
     * ignores order.
     *
     * @param test      The headers being tested.
     * @param reference The required entries to pass the test.
     * @return Boolean true if all required headers exist in the test map, false
     * otherwise.
     */
    private boolean hasRequiredHeaders(final Map<String, String> test, final Map<String, String> reference) {
        if (reference == null || reference.size() == 0)
            return true;

        // Verify all required headers are present.
        for (Map.Entry<String, String> entry : reference.entrySet()) {
            String key = entry.getKey();
            if (!test.containsKey(key))
                return false;
            if (!test.get(key).contains(entry.getValue()))
                return false;
        }

        return true;
    }
}
