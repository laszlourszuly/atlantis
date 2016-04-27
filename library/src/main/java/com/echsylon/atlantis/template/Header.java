package com.echsylon.atlantis.template;

import java.io.Serializable;

/**
 * This class represents a header.
 */
public final class Header implements Serializable {
    private final String key;
    private final String value;

    public Header(String key, String value) {
        this.key = key.trim();
        this.value = value.trim();
    }

    /**
     * Returns the header key.
     *
     * @return The header key.
     */
    public String key() {
        return key;
    }

    /**
     * Returns the header value.
     *
     * @return The header value.
     */
    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o instanceof Header) {
            Header other = (Header) o;
            return key.equalsIgnoreCase(other.key) && value.equalsIgnoreCase(other.value);
        }

        return false;
    }

}
