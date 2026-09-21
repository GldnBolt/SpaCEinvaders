package com.spaceinvaders.protocol;

/**
 * Convierte comandos administrativos pedidos en el PDF al formato interno del servidor.
 *
 * Comandos esperados:
 * Crear (X,Y,Pts)
 * OVNI I-D 1500
 * OVNI D-I 1500
 * Velocidad 100
 * Bunkers 70%
 */
public class AdminCommandParser {

    public Message parseAdminCommand(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Comando administrativo vacío");
        }

        String command = input.trim();

        if (command.startsWith("Crear")) {
            return parseCreateAlien(command);
        }

        if (command.startsWith("OVNI")) {
            return parseCreateUfo(command);
        }

        if (command.startsWith("Velocidad")) {
            return parseSpeed(command);
        }

        if (command.startsWith("Bunkers")) {
            return parseBunkers(command);
        }

        throw new IllegalArgumentException("Comando administrativo no reconocido: " + input);
    }

    private Message parseCreateAlien(String command) {
        int start = command.indexOf('(');
        int end = command.indexOf(')');

        if (start == -1 || end == -1 || end <= start) {
            throw new IllegalArgumentException("Formato esperado: Crear (X,Y,Pts)");
        }

        String content = command.substring(start + 1, end);
        String[] values = content.split(",");

        if (values.length != 3) {
            throw new IllegalArgumentException("Crear requiere X,Y,Pts");
        }

        int x = Integer.parseInt(values[0].trim());
        int y = Integer.parseInt(values[1].trim());
        int points = Integer.parseInt(values[2].trim());

        return new MessageBuilder("ADMIN_CREATE_ALIEN")
                .addInt("x", x)
                .addInt("y", y)
                .addInt("points", points)
                .buildMessage();
    }

    private Message parseCreateUfo(String command) {
        String[] parts = command.split("\\s+");

        if (parts.length != 3) {
            throw new IllegalArgumentException("Formato esperado: OVNI I-D 1500");
        }

        String direction = parts[1].trim();

        if (!direction.equalsIgnoreCase("I-D") && !direction.equalsIgnoreCase("D-I")) {
            throw new IllegalArgumentException("Dirección de OVNI esperada: I-D o D-I");
        }

        int points = Integer.parseInt(parts[2]);

        return new MessageBuilder("ADMIN_CREATE_UFO")
                .add("direction", direction)
                .addInt("points", points)
                .buildMessage();
    }

    private Message parseSpeed(String command) {
        String[] parts = command.split("\\s+");

        if (parts.length != 2) {
            throw new IllegalArgumentException("Formato esperado: Velocidad 100");
        }

        int value = Integer.parseInt(parts[1]);

        return new MessageBuilder("ADMIN_SET_SPEED")
                .addInt("value", value)
                .buildMessage();
    }

    private Message parseBunkers(String command) {
        String[] parts = command.split("\\s+");

        if (parts.length != 2) {
            throw new IllegalArgumentException("Formato esperado: Bunkers 70%");
        }

        String value = parts[1].replace("%", "");
        int health = Integer.parseInt(value);

        return new MessageBuilder("ADMIN_SET_BUNKERS")
                .addInt("health", health)
                .buildMessage();
    }
}
