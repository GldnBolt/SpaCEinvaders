package com.spaceinvaders.server;

/**
 * Resultado de intentar registrar un cliente en el servidor.
 */
public class RegistrationResult {
    private final boolean success;
    private final int playerId;
    private final String message;

    private RegistrationResult(boolean success, int playerId, String message) {
        this.success = success;
        this.playerId = playerId;
        this.message = message;
    }

    public static RegistrationResult success(int playerId) {
        return new RegistrationResult(true, playerId, "Registro exitoso");
    }

    public static RegistrationResult failure(String message) {
        return new RegistrationResult(false, 0, message);
    }

    public boolean isSuccess() {
        return success;
    }

    public int getPlayerId() {
        return playerId;
    }

    public String getMessage() {
        return message;
    }
}
