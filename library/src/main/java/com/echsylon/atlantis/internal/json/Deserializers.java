package com.echsylon.atlantis.internal.json;

import com.echsylon.atlantis.internal.Utils;
import com.echsylon.atlantis.template.Configuration;
import com.echsylon.atlantis.template.Request;
import com.echsylon.atlantis.template.Response;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * This class provides a set of custom JSON deserializers.
 */
class Deserializers {

    /**
     * Returns a new JSON deserializer, specialized for {@link Configuration} objects.
     *
     * @return The deserializer to parse {@code Configuration} JSON with.
     */
    static JsonDeserializer<Configuration> newConfigurationDeserializer() {
        return (json, typeOfT, context) -> deserializeConfiguration(json, context);
    }

    /**
     * Returns a new JSON deserializer, specialized for {@link Request} objects.
     *
     * @return The deserializer to parse {@code Request} JSON with.
     */
    static JsonDeserializer<Request> newRequestDeserializer() {
        return (json, typeOfT, context) -> deserializeRequest(json, context);
    }

    /**
     * Returns a new JSON deserializer, specialized for {@link Response} objects.
     *
     * @return The deserializer to parse {@code Response} objects with.
     */
    static JsonDeserializer<Object> newResponseDeserializer() {
        return (json, typeOfT, context) -> deserializeResponse(json, context);
    }

    // De-serializes a configuration JSON object, preparing any header attributes as well as
    // instantiating any request filter classes.
    private static Configuration deserializeConfiguration(JsonElement json, JsonDeserializationContext context) {
        // Remove the 'requestFilter' attribute as it's a string in the JSON. The Configuration
        // object expects it to be a Java object. We'll later parse the removed string (which just
        // happens to be the name of the class to instantiate) and set the object field manually.
        JsonObject jsonObject = json.getAsJsonObject();
        String filterClassName = jsonObject.has("requestFilter") ?
                jsonObject.remove("requestFilter").getAsString() :
                null;

        // Create the Configuration object from the JSON.
        Configuration.Builder configuration = context.deserialize(jsonObject, Configuration.Builder.class);

        // If there was no 'requestFilter' attribute in the JSON, we're done.
        if (Utils.isEmpty(filterClassName))
            return configuration;

        // Otherwise we'll need to instantiate a suitable class and assign it to the corresponding
        // field on the Configuration object.
        try {
            Request.Filter filter = (Request.Filter) Class.forName(filterClassName).newInstance();
            return configuration.withRequestFilter(filter);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    // De-serializes a response JSON object, preparing any header attributes.
    private static Response deserializeResponse(JsonElement json, JsonDeserializationContext context) {
        JsonObject jsonObject = prepareHeaders(json);
        return context.deserialize(jsonObject, Response.Builder.class);
    }

    // De-serializes a request JSON object, preparing any header attributes as well as instantiating
    // any response filter classes.
    private static Request deserializeRequest(JsonElement json, JsonDeserializationContext context) {
        // Remove the 'responseFilter' attribute as it's a string in the JSON. The Configuration
        // object expects it to be a Java object. We'll later parse the removed string (which just
        // happens to be the name of the class to instantiate) and set the object field manually.
        JsonObject jsonObject = prepareHeaders(json);
        String filterClassName = jsonObject.has("responseFilter") ?
                jsonObject.remove("responseFilter").getAsString() :
                null;

        // Create the Request object from the JSON.
        Request.Builder request = context.deserialize(jsonObject, Request.Builder.class);

        // If there was no 'responseFilter' attribute in the JSON, we're done.
        if (Utils.isEmpty(filterClassName))
            return request;

        // Otherwise we'll need to instantiate a suitable class and assign it to the corresponding
        // field on the Request object.
        try {
            Response.Filter filter = (Response.Filter) Class.forName(filterClassName).newInstance();
            return request.withResponseFilter(filter);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    // Ensures any header attribute in the JSON object is formatted properly as a dictionary.
    // Headers can appear as '\n' separated strings (keys and values separated by ':'), or as an
    // array of objects with "key" and "value" attributes, or as a dictionary. This method will
    // transform the two former patterns to the latter.
    private static JsonObject prepareHeaders(JsonElement json) {
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

        return jsonObject;
    }

    // Splits a header string into a JSON dictionary.
    private static JsonElement splitHeaders(String headerString) {
        JsonObject jsonObject = new JsonObject();
        String[] splitHeaders = headerString.split("\n");

        for (String header : splitHeaders) {
            int firstIndex = header.indexOf(':');
            if (firstIndex != -1) {
                String key = header.substring(0, firstIndex).trim();
                String value = header.substring(firstIndex + 1).trim();
                if (Utils.notEmpty(key) && Utils.notEmpty(value))
                    jsonObject.addProperty(key, value);
            }
        }

        return jsonObject;
    }

    // Transforms an array of header objects into a JSON dictionary
    private static JsonElement transformHeaders(JsonArray headerArray) {
        JsonObject jsonObject = new JsonObject();

        for (JsonElement header : headerArray) {
            if (header.isJsonObject()) {
                JsonObject o = header.getAsJsonObject();
                if (o.has("key") && o.has("value")) {
                    String key = o.get("key").getAsString();
                    String value = o.get("value").getAsString();
                    if (Utils.notEmpty(key) && Utils.notEmpty(value))
                        jsonObject.addProperty(key, value);
                }
            }
        }

        return jsonObject;
    }

}
