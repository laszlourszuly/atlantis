package com.echsylon.atlantis;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

/**
 * This class is responsible for serializing and de-serializing json data.
 */
class JsonParser {

    /**
     * This instance of a runtime exception represents an error caused while
     * parsing JSON data.
     */
    private static final class JsonException extends RuntimeException {
        private JsonException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * Tries to parse a JSON string into a Java object.
     *
     * @param json               The JSON string to parse.
     * @param expectedResultType The Java object implementation to parse into.
     * @param <T>                The type of class.
     * @return An instance of the requested Java object representing the JSON
     * data.
     * @throws JsonException If anything would go wrong during the parse
     *                       attempt.
     */
    <T> T fromJson(String json, Class<T> expectedResultType) throws JsonException {
        try {
            return new GsonBuilder()
                    .registerTypeAdapter(Configuration.class, JsonSerializers.newConfigurationDeserializer())
                    .registerTypeAdapter(MockResponse.class, JsonSerializers.newResponseDeserializer())
                    .registerTypeAdapter(MockRequest.class, JsonSerializers.newRequestDeserializer())
                    .create()
                    .fromJson(json, expectedResultType);
        } catch (IllegalArgumentException | JsonSyntaxException e) {
            throw new JsonException(e);
        }
    }

    /**
     * Serializes the given object into a JSON string.
     *
     * @param object        The object to serialize.
     * @param classOfObject The desired type of the object being serialized.
     * @param <T>           The generic class type.
     * @return The JSON string notation of the given object.
     */
    <T> String toJson(Object object, Class<T> classOfObject) {
        return new GsonBuilder()
                .registerTypeAdapter(Configuration.class, JsonSerializers.newConfigurationSerializer())
                .registerTypeAdapter(MockResponse.class, JsonSerializers.newResponseSerializer())
                .registerTypeAdapter(MockRequest.class, JsonSerializers.newRequestSerializer())
                .disableHtmlEscaping()
                .setPrettyPrinting()
                .create()
                .toJson(object, classOfObject);
    }
}
