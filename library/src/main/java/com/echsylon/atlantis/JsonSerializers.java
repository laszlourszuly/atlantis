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
import static com.echsylon.atlantis.Utils.isEmpty;
import static com.echsylon.atlantis.Utils.notEmpty;

/**
 * This class provides a set of custom JSON deserializers.
 */
class JsonSerializers {

    /**
     * Returns a new JSON deserializer, specialized for {@link HeaderManager}
     * objects.
     *
     * @return The deserializer to parse {@code HeaderManager} JSON with.
     */
    static JsonDeserializer<HeaderManager> newHeaderDeserializer() {
        return (json, typeOfT, context) -> {
            if (json == null)
                return null;

            HeaderManager headerManager = new HeaderManager();
            if (json.isJsonObject()) {
                // Recommended dictionary style headers
                JsonObject jsonObject = json.getAsJsonObject();
                for (Map.Entry<String, JsonElement> property : jsonObject.entrySet()) {
                    String key = property.getKey();
                    JsonElement jsonElement = property.getValue();

                    if (jsonElement.isJsonPrimitive()) {
                        // Single header value for the key
                        headerManager.add(key, jsonElement.getAsString());
                    } else if (jsonElement.isJsonArray()) {
                        // Multiple header values for the key
                        JsonArray jsonArray = jsonElement.getAsJsonArray();
                        for (JsonElement e : jsonArray)
                            if (e.isJsonPrimitive())
                                headerManager.add(key, e.getAsString());
                    }
                }
            } else if (json.isJsonArray()) {
                // List of objects  with "key" + "value" properties
                JsonArray jsonArray = json.getAsJsonArray();
                for (JsonElement jsonElement : jsonArray) {
                    if (jsonElement.isJsonObject()) {
                        JsonObject jsonObject = jsonElement.getAsJsonObject();
                        if (jsonObject.has("key") && jsonObject.has("value")) {
                            String key = jsonObject.get("key").getAsString();
                            String value = jsonObject.get("value").getAsString();
                            headerManager.add(key, value);
                        }
                    }
                }
            } else if (json.isJsonPrimitive()) {
                // New-Line separated headers (as expressed by Postman v1)
                String[] strings = json.getAsString().split("\n");

                for (String string : strings) {
                    int index = string.indexOf(':');
                    int maxIndex = string.length() - 1;
                    if (index != -1 && index < maxIndex) {
                        // Everything before the first ':' is seen as a key and
                        // everything after it is considered a value
                        String key = string.substring(0, index).trim();
                        String value = string.substring(index + 1).trim();
                        headerManager.add(key, value);
                    }
                }

            }

            return headerManager;
        };
    }

    /**
     * Returns a new JSON serializer, specialized for {@link HeaderManager}
     * objects.
     *
     * @return The serializer to serialize {@code HeaderManager} objects with.
     */
    static JsonSerializer<HeaderManager> newHeaderSerializer() {
        return (headerManager, typeOfObject, context) -> {
            if (headerManager == null)
                return null;

            Map<String, List<String>> headers = headerManager.getAllAsMultiMap();
            if (isEmpty(headers))
                return null;

            JsonObject jsonObject = new JsonObject();
            for (Map.Entry<String, List<String>> header : headers.entrySet()) {
                List<String> values = header.getValue();
                int count = values.size();

                if (count == 1) {
                    // Single value; add as string
                    jsonObject.addProperty(header.getKey(), values.get(0));
                } else if (count > 1) {
                    // Multiple values; add as array
                    JsonArray jsonArray = new JsonArray();
                    for (String value : values)
                        jsonArray.add(value);
                    jsonObject.add(header.getKey(), jsonArray);
                }
            }

            return jsonObject;
        };
    }

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

            if (jsonObject.has("tokenHelper"))
                try {
                    String helperClassName = jsonObject.get("tokenHelper").getAsString();
                    Atlantis.TokenHelper helper = (Atlantis.TokenHelper) Class.forName(helperClassName).newInstance();
                    builder.setTokenHelper(helper);
                } catch (Exception e) {
                    info(e, "Couldn't deserialize token helper");
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
                    HeaderManager headerManager = context.deserialize(jsonObject.get("defaultResponseHeaders"), HeaderManager.class);
                    builder.setDefaultResponseHeaderManager(headerManager);
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

            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("fallbackBaseUrl", configuration.fallbackBaseUrl());

            MockRequest.Filter filter = configuration.requestFilter();
            if (filter != null)
                jsonObject.addProperty("requestFilter", filter.getClass().getCanonicalName());

            Atlantis.TokenHelper tokenHelper = configuration.tokenHelper();
            if (tokenHelper != null)
                jsonObject.addProperty("tokenHelper", tokenHelper.getClass().getCanonicalName());

            Atlantis.TransformationHelper transformationHelper = configuration.transformationHelper();
            if (transformationHelper != null)
                jsonObject.addProperty("transformationHelper", transformationHelper.getClass().getCanonicalName());

            HeaderManager defaultResponseHeaderManager = configuration.defaultResponseHeaderManager();
            if (defaultResponseHeaderManager.keyCount() > 0)
                jsonObject.add("defaultResponseHeaders", context.serialize(defaultResponseHeaderManager));

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
                    HeaderManager headerManager = context.deserialize(jsonObject.get("headers"), HeaderManager.class);
                    builder.setHeaderManager(headerManager);
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

            HeaderManager headerManager = request.headerManager();
            if (headerManager.keyCount() > 0)
                jsonObject.add("headers", context.serialize(headerManager));

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

            normalizeResponseCode(json);

            JsonObject jsonObject = json.getAsJsonObject();
            MockResponse.Builder builder = new MockResponse.Builder();
            builder.setStatus(jsonObject.get("code").getAsInt(), jsonObject.get("phrase").getAsString());
            builder.setBody(jsonObject.get("text").getAsString());

            if (jsonObject.has("headers"))
                try {
                    HeaderManager headerManager = context.deserialize(jsonObject.get("header"), HeaderManager.class);
                    builder.setHeaderManager(headerManager);
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

            HeaderManager headerManager = response.headerManager();
            if (headerManager.keyCount() > 0)
                jsonObject.add("headers", context.serialize(headerManager));

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

}
