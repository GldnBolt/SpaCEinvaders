package com.spaceinvaders;

import com.spaceinvaders.server.GameServer;

/**
 * Punto de entrada del servidor Java.
 *
 * Permite iniciar el servidor en el puerto por defecto 5000
 * o indicar otro puerto por argumentos.
 *
 * Ejemplo:
 * java -jar spaceinvaders-server-1.0-SNAPSHOT.jar
 *
 * Ejemplo con puerto:
 * java -jar spaceinvaders-server-1.0-SNAPSHOT.jar 6000
 */
public class MainServer {
    private static final int DEFAULT_PORT = 5000;

    public static void main(String[] args) {
        int port = parsePort(args);

        GameServer server = new GameServer(port);

        try {
            server.start();
        } catch (Exception exception) {
            System.err.println("Error al iniciar el servidor: " + exception.getMessage());
        }
    }

    private static int parsePort(String[] args) {
        if (args.length == 0) {
            return DEFAULT_PORT;
        }

        try {
            int port = Integer.parseInt(args[0]);

            if (port <= 0 || port > 65535) {
                System.out.println("Puerto fuera de rango. Se usará el puerto por defecto " + DEFAULT_PORT + ".");
                return DEFAULT_PORT;
            }

            return port;

        } catch (NumberFormatException exception) {
            System.out.println("Puerto inválido. Se usará el puerto por defecto " + DEFAULT_PORT + ".");
            return DEFAULT_PORT;
        }
    }
}