package com.echsylon.atlantis;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.echsylon.atlantis.LogUtils.info;
import static com.echsylon.atlantis.Utils.notEmpty;

/**
 * This class provides a set of custom JSON deserializers.
 */
class JsonSerializers {

    /**
     * Returns a new JSON deserializer, specialized for {@link Configuration}
     * objects.
     *
     * @return The deserializer to parse {@code Configuration} JSON with.
     */
    static JsonDeserializer<Configuration> newConfigurationDeserializer() {
        return (json, typeOfT, context) -> {
            if (json == null)
                return null;

            JsonObject jsonObject = json.getAsJsonObject();
            Configuration.Builder builder = new Configuration.Builder();
            builder.setFallbackBaseUrl(jsonObject.get("fallbackBaseUrl").getAsString());

            if (jsonObject.has("requestFilter"))
                try {
                    String filterClassName = jsonObject.get("requestFilter").getAsString();
                    MockRequest.Filter filter = (MockRequest.Filter) Class.forName(filterClassName).newInstance();
                    builder.setRequestFilter(filter);
                } catch (Exception e) {
                    info(e, "Couldn't deserialize request filter");
                }

            if (jsonObject.has("transformationHelper"))
                try {
                    String helperClassName = jsonObject.get("transformationHelper").getAsString();
                    Atlantis.TransformationHelper helper = (Atlantis.TransformationHelper) Class.forName(helperClassName).newInstance();
                    builder.setTransformationHelper(helper);
                } catch (Exception e) {
                    info(e, "Couldn't deserialize transformation helper");
                }

            if (jsonObject.has("defaultResponseHeaders"))
                try {
                    JsonObject headers = jsonObject.get("defaultResponseHeaders").getAsJsonObject();
                    builder.addDefaultResponseHeaders(context.deserialize(headers, LinkedHashMap.class));
                } catch (Exception e) {
                    info(e, "Couldn't deserialize default response headers");
                }

            if (jsonObject.has("defaultResponseSettings"))
                try {
                    JsonObject settings = jsonObject.get("defaultResponseSettings").getAsJsonObject();
                    builder.addDefaultResponseSettings(context.deserialize(settings, LinkedHashMap.class));
                } catch (Exception e) {
                    info(e, "Couldn't deserialize default response settings");
                }

            if (jsonObject.has("requests"))
                try {
                    JsonArray requests = jsonObject.get("requests").getAsJsonArray();
                    for (int i = 0, c = requests.size(); i < c; i++)
                        builder.addRequest(context.deserialize(requests.get(i), MockRequest.class));
                } catch (Exception e) {
                    info(e, "Couldn't deserialize requests");
                }

            return builder.build();
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
            if (configuration == null)
                return null;

            MockRequest.Filter filter = configuration.requestFilter();
            Atlantis.TransformationHelper helper = configuration.transformationHelper();

            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("fallbackBaseUrl", configuration.fallbackBaseUrl());
            jsonObject.addProperty("requestFilter", filter != null ? filter.getClass().getCanonicalName() : null);
            jsonObject.addProperty("transformationHelper", helper != null ? helper.getClass().getCanonicalName() : null);
            jsonObject.add("defaultResponseHeaders", context.serialize(configuration.defaultResponseHeaders()));
            jsonObject.add("defaultResponseSettings", context.serialize(configuration.defaultResponseSettings()));
            jsonObject.add("requests", context.serialize(configuration.requests()));

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
            if (json == null)
                return null;

            normalizeHeaders(json);

            JsonObject jsonObject = json.getAsJsonObject();
            MockRequest.Builder builder = new MockRequest.Builder();
            builder.setMethod(jsonObject.get("method").getAsString());
            builder.setUrl(jsonObject.get("url").getAsString());

            if (jsonObject.has("responseFilter"))
                try {
                    String filterClassName = jsonObject.get("responseFilter").getAsString();
                    MockResponse.Filter filter = (MockResponse.Filter) Class.forName(filterClassName).newInstance();
                    builder.setResponseFilter(filter);
                } catch (Exception e) {
                    info(e, "Couldn't deserialize response filter");
                }

            if (jsonObject.has("headers"))
                try {
                    JsonObject headers = jsonObject.get("headers").getAsJsonObject();
                    builder.addHeaders(context.deserialize(headers, LinkedHashMap.class));
                } catch (Exception e) {
                    info(e, "Couldn't deserialize headers");
                }

            if (jsonObject.has("responses"))
                try {
                    JsonArray responses = jsonObject.get("responses").getAsJsonArray();
                    for (int i = 0, c = responses.size(); i < c; i++)
                        builder.addResponse(context.deserialize(responses.get(i), MockResponse.class));
                } catch (Exception e) {
                    info(e, "Couldn't deserialize responses");
                }

            return builder.build();
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
            if (request == null)
                return null;

            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("method", request.method());
            jsonObject.addProperty("url", request.url());

            MockResponse.Filter filter = request.responseFilter();
            if (filter != null)
                jsonObject.addProperty("responseFilter", filter.getClass().getCanonicalName());

            Map<String, String> headers = request.headers();
            if (notEmpty(headers))
                jsonObject.add("headers", context.serialize(headers));

            List<MockResponse> responses = request.responses();
            if (notEmpty(responses))
                jsonObject.add("responses", context.serialize(responses));

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
            if (json == null)
                return null;

            normalizeHeaders(json);
            normalizeResponseCode(json);

            JsonObject jsonObject = json.getAsJsonObject();
            MockResponse.Builder builder = new MockResponse.Builder();
            builder.setStatus(jsonObject.get("code").getAsInt(), jsonObject.get("phrase").getAsString());
            builder.setBody(jsonObject.get("text").getAsString());

            if (jsonObject.has("stateHelper"))
                try {
                    String helperClassName = jsonObject.get("stateHelper").getAsString();
                    MockResponse.StateHelper helper = (MockResponse.StateHelper) Class.forName(helperClassName).newInstance();
                    builder.setStateHelper(helper);
                } catch (Exception e) {
                    info(e, "Couldn't deserialize state helper");
                }

            if (jsonObject.has("headers"))
                try {
                    JsonObject headers = jsonObject.get("headers").getAsJsonObject();
                    builder.addHeaders(context.deserialize(headers, LinkedHashMap.class));
                } catch (Exception e) {
                    info(e, "Couldn't deserialize headers");
                }

            if (jsonObject.has("settings"))
                try {
                    JsonArray settings = jsonObject.get("settings").getAsJsonArray();
                    builder.addSettings(context.deserialize(settings, LinkedHashMap.class));
                } catch (Exception e) {
                    info(e, "Couldn't deserialize responses");
                }

            return builder.build();
        };
    }

    /**
     * Returns a new JSON serializer, specialized for {@link MockResponse}
     * objects.
     *
     * @return The serializer to serialize {@code MockResponse} objects with.
     */
    static JsonSerializer<MockResponse> newResponseSerializer() {
        return (response, typeOfObject, context) -> {
            if (response == null)
                return null;

            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("code", response.code());
            jsonObject.addProperty("phrase", response.phrase());

            String text = response.body();
            if (notEmpty(text))
                jsonObject.addProperty("text", text);

            MockResponse.StateHelper stateHelper = response.stateHelper();
            if (stateHelper != null)
                jsonObject.addProperty("stateHelper", stateHelper.getClass().getCanonicalName());

            Map<String, String> headers = response.headers();
            if (notEmpty(headers))
                jsonObject.add("headers", context.serialize(headers));

            Map<String, String> settings = response.settings();
            if (notEmpty(settings))
                jsonObject.add("settings", context.serialize(settings));

            return jsonObject;
        };
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
