package com.spaceinvaders.patterns.adapter;

import com.spaceinvaders.protocol.Message;

/**
 * Patrón Adapter.
 *
 * Convierte los mensajes recibidos por red en comandos internos del juego.
 *
 * Ejemplo:
 * ACTION|playerId=1|cmd=MOVE_LEFT
 *
 * se convierte en:
 * NetworkCommandType.MOVE_LEFT
 */
public class NetworkCommandAdapter {

    public NetworkCommand adapt(Message message) {
        if (message == null) {
            return new NetworkCommand(NetworkCommandType.UNKNOWN);
        }

        return switch (message.getType()) {
            case "ACTION" -> adaptAction(message);
            case "ALIEN_HIT" -> new NetworkCommand(
                    NetworkCommandType.ALIEN_HIT,
                    message.getInt("alienId", -1)
            );
            case "DISCONNECT" -> new NetworkCommand(NetworkCommandType.DISCONNECT);
            default -> new NetworkCommand(NetworkCommandType.UNKNOWN);
        };
    }

    private NetworkCommand adaptAction(Message message) {
        String command = message.get("cmd");

        if (command == null) {
            return new NetworkCommand(NetworkCommandType.UNKNOWN);
        }

        return switch (command) {
            case "MOVE_LEFT" -> new NetworkCommand(NetworkCommandType.MOVE_LEFT);
            case "MOVE_RIGHT" -> new NetworkCommand(NetworkCommandType.MOVE_RIGHT);
            case "FIRE" -> new NetworkCommand(NetworkCommandType.FIRE);
            default -> new NetworkCommand(NetworkCommandType.UNKNOWN);
        };
    }
}