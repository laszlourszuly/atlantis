package com.echsylon.atlantis.internal.json;

import com.echsylon.atlantis.Configuration;
import com.echsylon.atlantis.Request;
import com.echsylon.atlantis.Response;
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
                    .registerTypeAdapter(Configuration.class, Deserializers.newConfigurationDeserializer())
                    .registerTypeAdapter(Response.class, Deserializers.newResponseDeserializer())
                    .registerTypeAdapter(Request.class, Deserializers.newRequestDeserializer())
                    .create()
                    .fromJson(json, expectedResultType);
        } catch (IllegalArgumentException | JsonSyntaxException e) {
            throw new JsonException(e);
        }
    }

}
