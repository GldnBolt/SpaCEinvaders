package com.spaceinvaders.patterns.adapter;

/**
 * Representa un comando interno del servidor.
 *
 * Este comando ya no depende directamente del texto recibido por socket.
 */
public class NetworkCommand {
    private final NetworkCommandType type;
    private final int alienId;

    public NetworkCommand(NetworkCommandType type) {
        this(type, -1);
    }

    public NetworkCommand(NetworkCommandType type, int alienId) {
        this.type = type;
        this.alienId = alienId;
    }

    public NetworkCommandType getType() {
        return type;
    }

    public int getAlienId() {
        return alienId;
    }
}