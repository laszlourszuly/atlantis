package com.echsylon.atlantis.internal.json;

import com.echsylon.atlantis.template.Request;
import com.echsylon.atlantis.template.Response;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

/**
 * This class is responsible for serializing and de-serializing json data.
 */
public class JsonParser {

    /**
     * This instance of a runtime exception represents an error caused while parsing JSON data.
     */
    public static final class JsonException extends RuntimeException {
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
     * @return An instance of the requested Java object representing the JSON data.
     * @throws JsonException If anything would go wrong during the parse attempt.
     */
    public <T> T fromJson(String json, Class<T> expectedResultType) throws JsonException {
        try {
            return new GsonBuilder()
                    .registerTypeAdapter(Request.class, HeaderDeserializer.newRequestHeaderDeserializer())
                    .registerTypeAdapter(Response.class, HeaderDeserializer.newResponseHeaderDeserializer())
                    .create()
                    .fromJson(json, expectedResultType);
        } catch (JsonSyntaxException e) {
            throw new JsonException(e);
        }
    }

    /**
     * Tries to write a JSON string based on a Java object.
     *
     * @param object The Java object to serialize into a JSON string.
     * @return The JSON string, never null. Handles null pointers gracefully.
     */
    public String toJson(Object object) {
        return new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create()
                .toJson(object);
    }

}
