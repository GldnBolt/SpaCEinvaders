package com.spaceinvaders;

import com.spaceinvaders.server.GameServer;

/**
 * Punto de entrada del servidor Java de spaCEinvaders.
 */
public class MainServer {
    public static void main(String[] args) {
        int port = 5000;

        if (args.length >= 1) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException exception) {
                System.err.println("Puerto inválido. Se usará el puerto por defecto 5000.");
            }
        }

        GameServer server = new GameServer(port);

        try {
            server.start();
        } catch (Exception exception) {
            System.err.println("Error al iniciar el servidor: " + exception.getMessage());
            exception.printStackTrace();
        }
    }
}
