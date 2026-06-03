package com.spaceinvaders.protocol;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Representa un mensaje recibido por sockets.
 * Ejemplo: ACTION|playerId=1|cmd=MOVE_LEFT
 */
public class Message {
    private final String type;
    private final Map<String, String> fields;

    public Message(String type, Map<String, String> fields) {
        this.type = type;
        this.fields = new HashMap<>(fields);
    }

    public String getType() {
        return type;
    }

    public String get(String key) {
        return fields.get(key);
    }

    public int getInt(String key, int defaultValue) {
        try {
            String value = fields.get(key);

            if (value == null) {
                return defaultValue;
            }

            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    public Map<String, String> getFields() {
        return Collections.unmodifiableMap(fields);
    }

    @Override
    public String toString() {
        return "Message{type='" + type + "', fields=" + fields + "}";
    }
}
