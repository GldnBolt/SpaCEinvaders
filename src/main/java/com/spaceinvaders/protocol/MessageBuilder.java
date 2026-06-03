package com.spaceinvaders.protocol;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Construye mensajes del servidor hacia los clientes.
 */
public class MessageBuilder {
    private final String type;
    private final Map<String, String> fields;

    public MessageBuilder(String type) {
        this.type = type;
        this.fields = new LinkedHashMap<>();
    }

    public MessageBuilder add(String key, String value) {
        fields.put(key, value == null ? "" : value);
        return this;
    }

    public MessageBuilder addInt(String key, int value) {
        fields.put(key, String.valueOf(value));
        return this;
    }

    public String build() {
        StringBuilder builder = new StringBuilder(type);

        for (Map.Entry<String, String> entry : fields.entrySet()) {
            builder.append("|")
                    .append(entry.getKey())
                    .append("=")
                    .append(entry.getValue());
        }

        return builder.toString();
    }

    public Message buildMessage() {
        return new Message(type, fields);
    }
}
