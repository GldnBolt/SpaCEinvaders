package com.spaceinvaders.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Cliente de prueba en consola.
 * No forma parte del cliente oficial en C, pero sirve para probar el servidor Java sin esperar la GUI.
 */
public class ConsoleTestClient {
    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 5000;
        String role = args.length > 2 ? args[2] : "PLAYER";
        String name = args.length > 3 ? args[3] : "ClienteJavaPrueba";

        try (Socket socket = new Socket(host, port);
             BufferedReader input = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
             );
             PrintWriter output = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),
                     true
             );
             Scanner scanner = new Scanner(System.in)) {

            output.println("HELLO|role=" + role + "|name=" + name);

            Thread readerThread = new Thread(() -> readServerMessages(input));
            readerThread.setDaemon(true);
            readerThread.start();

            printHelp(role);

            while (true) {
                System.out.print("client> ");
                String command = scanner.nextLine();

                if (command.equalsIgnoreCase("salir")) {
                    output.println("DISCONNECT|playerId=0");
                    break;
                }

                switch (command.toLowerCase()) {
                    case "a" -> output.println("ACTION|playerId=0|cmd=MOVE_LEFT");
                    case "d" -> output.println("ACTION|playerId=0|cmd=MOVE_RIGHT");
                    case "f" -> output.println("ACTION|playerId=0|cmd=FIRE");
                    default -> output.println(command);
                }
            }

        } catch (IOException exception) {
            System.err.println("No se pudo conectar al servidor: " + exception.getMessage());
        }
    }

    private static void readServerMessages(BufferedReader input) {
        try {
            String line;
            while ((line = input.readLine()) != null) {
                System.out.println("\nSERVER: " + line);
            }
        } catch (IOException exception) {
            System.out.println("Conexión cerrada por el servidor.");
        }
    }

    private static void printHelp(String role) {
        System.out.println("Cliente de prueba conectado como " + role);
        System.out.println("Comandos rápidos:");
        System.out.println("  a = mover izquierda");
        System.out.println("  d = mover derecha");
        System.out.println("  f = disparar");
        System.out.println("  ALIEN_HIT|playerId=1|alienId=1");
        System.out.println("  salir");
    }
}
