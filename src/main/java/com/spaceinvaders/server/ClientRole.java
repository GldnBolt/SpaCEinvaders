package com.spaceinvaders.server;

/**
 * Define el tipo de cliente conectado al servidor.
 */
public enum ClientRole {
    PLAYER,
    SPECTATOR;

    public static ClientRole fromText(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Rol vacío");
        }

        return switch (text.toUpperCase()) {
            case "PLAYER" -> PLAYER;
            case "SPECTATOR" -> SPECTATOR;
            default -> throw new IllegalArgumentException("Rol no válido: " + text);
        };
    }
}
