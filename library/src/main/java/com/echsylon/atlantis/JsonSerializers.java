package com.echsylon.atlantis;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializer;

import static com.echsylon.atlantis.Utils.notEmpty;

/**
 * This class provides a set of custom JSON deserializers.
 */
class JsonSerializers {

    // This is a marker class preventing eternal circular parse loops in gson.
    private static final class _Configuration extends Configuration {
    }

    // This is a marker class preventing eternal circular parse loops in gson.
    private static final class _MockResponse extends MockResponse {
    }

    // This is a marker class preventing eternal circular parse loops in gson.
    private static final class _MockRequest extends MockRequest {
    }


    /**
     * Returns a new JSON deserializer, specialized for {@link Configuration}
     * objects.
     *
     * @return The deserializer to parse {@code Configuration} JSON with.
     */
    static JsonDeserializer<Configuration> newConfigurationDeserializer() {
        return (json, typeOfT, context) -> {
            // Remove the 'requestFilter' string attribute from the JSON. We'll
            // later parse the removed string and set the object field manually.
            JsonObject jsonObject = json.getAsJsonObject();
            String filterClassName = jsonObject.has("requestFilter") ?
                    jsonObject.remove("requestFilter").getAsString() :
                    null;

            Configuration configuration = context.deserialize(jsonObject, _Configuration.class);
            if (notEmpty(filterClassName))
                try {
                    MockRequest.Filter filter = (MockRequest.Filter) Class.forName(filterClassName).newInstance();
                    configuration.setRequestFilter(filter);
                } catch (Exception e) {
                    throw new IllegalArgumentException(e);
                }

            return configuration;
        };
    }

    /**
     * Returns a new JSON serializer, specialized for {@link Configuration}
     * objects.
     *
     * @return The serializer to serialize {@code Configuration} objects with.
     */
    static JsonSerializer<Configuration> newConfigurationSerializer() {
        return (configuration, typeOfObject, context) -> {
            JsonElement jsonElement = context.serialize(configuration, _Configuration.class);
            if (jsonElement == null)
                return null;

            JsonObject jsonObject = jsonElement.getAsJsonObject();
            MockRequest.Filter filter = configuration.requestFilter();
            if (filter != null)
                jsonObject.addProperty("requestFilter", filter.getClass().getName());

            return jsonObject;
        };
    }

    /**
     * Returns a new JSON deserializer, specialized for {@link MockRequest}
     * objects.
     *
     * @return The deserializer to parse {@code MockRequest} JSON with.
     */
    static JsonDeserializer<MockRequest> newRequestDeserializer() {
        return (json, typeOfT, context) -> {
            // Remove the 'responseFilter' string attribute from the JSON. We'll
            // later parse the removed string and set the object field manually.
            normalizeHeaders(json);
            JsonObject jsonObject = json.getAsJsonObject();
            String filterClassName = jsonObject.has("responseFilter") ?
                    jsonObject.remove("responseFilter").getAsString() :
                    null;

            MockRequest mockRequest = context.deserialize(jsonObject, _MockRequest.class);
            if (notEmpty(filterClassName))
                try {
                    MockResponse.Filter filter = (MockResponse.Filter) Class.forName(filterClassName).newInstance();
                    mockRequest.setResponseFilter(filter);
                } catch (Exception e) {
                    throw new IllegalArgumentException(e);
                }

            return mockRequest;
        };
    }

    /**
     * Returns a new JSON serializer, specialized for {@link MockRequest}
     * objects.
     *
     * @return The serializer to serialize {@code MockRequest} objects with.
     */
    static JsonSerializer<MockRequest> newRequestSerializer() {
        return (request, typeOfObject, context) -> {
            JsonElement jsonElement = context.serialize(request, _MockRequest.class);
            if (jsonElement == null)
                return null;

            JsonObject jsonObject = jsonElement.getAsJsonObject();
            MockResponse.Filter filter = request.responseFilter();
            if (filter != null) {
                jsonObject.addProperty("responseFilter", filter.getClass().getName());
            }

            return jsonObject;
        };
    }

    /**
     * Returns a new JSON deserializer, specialized for {@link MockResponse}
     * objects.
     *
     * @return The deserializer to parse {@code MockResponse} objects with.
     */
    static JsonDeserializer<MockResponse> newResponseDeserializer() {
        return (json, typeOfT, context) -> {
            normalizeHeaders(json);
            normalizeResponseCode(json);
            return context.deserialize(json, _MockResponse.class);
        };
    }

    /**
     * Returns a new JSON serializer, specialized for {@link MockResponse}
     * objects.
     *
     * @return The serializer to serialize {@code MockResponse} objects with.
     */
    static JsonSerializer<MockResponse> newResponseSerializer() {
        return (response, typeOfObject, context) -> context.serialize(response, _MockResponse.class);
    }


    /**
     * Converts any Postman v1 response code objects to expected member fields
     * of the {@code Atlantis} {@code MockResponse} object.
     *
     * @param json The un-parsed response JSON element.
     */
    private static void normalizeResponseCode(JsonElement json) {
        JsonObject jsonObject = json.getAsJsonObject();
        if (jsonObject.has("responseCode")) {
            JsonObject responseCode = jsonObject.remove("responseCode").getAsJsonObject();

            if (responseCode.has("code"))
                jsonObject.addProperty("code", responseCode.get("code").getAsNumber());

            if (responseCode.has("name"))
                jsonObject.addProperty("phrase", responseCode.get("name").getAsString());
        }
    }

    /**
     * Ensures any header attribute in the JSON object is formatted properly as
     * a dictionary. Headers can appear as '\n' separated strings (keys and
     * values separated by ':'), or as an array of objects with "key" and
     * "value" attributes, or as a dictionary. This method will transform the
     * two former patterns to the latter.
     *
     * @param json The json element holding the headers structure to transform.
     */
    private static void normalizeHeaders(JsonElement json) {
        JsonObject jsonObject = json.getAsJsonObject();
        JsonElement headers = jsonObject.get("headers");

        if (headers != null) {
            if (headers.isJsonPrimitive()) {
                String headerString = headers.getAsString();
                JsonElement headersJsonElement = splitHeaders(headerString);
                jsonObject.add("headers", headersJsonElement);
            } else if (headers.isJsonArray()) {
                JsonArray headerArray = headers.getAsJsonArray();
                JsonElement headersJsonElement = transformHeaders(headerArray);
                jsonObject.add("headers", headersJsonElement);
            }
        }
    }

    /**
     * Splits a header string into a JSON dictionary.
     *
     * @param headerString The string of all headers to split.
     * @return The transformed json dictionary element.
     */
    private static JsonElement splitHeaders(String headerString) {
        JsonObject jsonObject = new JsonObject();
        String[] splitHeaders = headerString.split("\n");

        for (String header : splitHeaders) {
            int firstIndex = header.indexOf(':');
            if (firstIndex != -1) {
                String key = header.substring(0, firstIndex).trim();
                String value = header.substring(firstIndex + 1).trim();
                if (notEmpty(key) && notEmpty(value))
                    jsonObject.addProperty(key, value);
            }
        }

        return jsonObject;
    }

    /**
     * Transforms an array of header objects into a JSON dictionary
     *
     * @param headerArray The array of json objects to transform into a json
     *                    dictionary.
     * @return The transformed json dictionary element.
     */
    private static JsonElement transformHeaders(JsonArray headerArray) {
        JsonObject jsonObject = new JsonObject();

        for (JsonElement header : headerArray) {
            if (header.isJsonObject()) {
                JsonObject o = header.getAsJsonObject();
                if (o.has("key") && o.has("value")) {
                    String key = o.get("key").getAsString();
                    String value = o.get("value").getAsString();
                    if (notEmpty(key) && notEmpty(value))
                        jsonObject.addProperty(key, value);
                }
            }
        }

        return jsonObject;
    }
}
