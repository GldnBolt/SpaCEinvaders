package com.spaceinvaders.protocol;

import java.util.HashMap;
import java.util.Map;

/**
 * Convierte una línea recibida por socket en un Message.
 * Formato: TIPO|clave=valor|clave=valor
 */
public class MessageParser {

    public Message parse(String line) {
        if (line == null || line.trim().isEmpty()) {
            throw new IllegalArgumentException("Mensaje vacío");
        }

        String cleanLine = line.trim();
        String[] parts = cleanLine.split("\\|");

        String type = parts[0].trim();

        if (type.isEmpty()) {
            throw new IllegalArgumentException("El mensaje no tiene tipo");
        }

        Map<String, String> fields = new HashMap<>();

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            int equalsIndex = part.indexOf('=');

            if (equalsIndex == -1) {
                throw new IllegalArgumentException("Campo inválido: " + part);
            }

            String key = part.substring(0, equalsIndex).trim();
            String value = part.substring(equalsIndex + 1).trim();

            if (key.isEmpty()) {
                throw new IllegalArgumentException("Clave vacía en campo: " + part);
            }

            fields.put(key, value);
        }

        return new Message(type, fields);
    }
}
