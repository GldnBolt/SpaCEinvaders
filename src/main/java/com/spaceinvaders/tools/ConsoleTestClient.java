package com.spaceinvaders.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Cliente de consola temporal para probar el servidor Java.
 *
 * Este NO es el cliente final del proyecto.
 * El cliente final debe hacerse en C.
 */
public class ConsoleTestClient {
    private static boolean summaryMode = false;
    private static int stateCounter = 0;

    public static void main(String[] args) {
        String host = args.length >= 1 ? args[0] : "127.0.0.1";
        int port = args.length >= 2 ? parsePort(args[1]) : 5000;
        String role = args.length >= 3 ? args[2] : "PLAYER";
        String name = args.length >= 4 ? args[3] : "ClienteJava";
        summaryMode = args.length >= 5 && args[4].equalsIgnoreCase("summary");

        try (
                Socket socket = new Socket(host, port);
                BufferedReader serverInput = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
                );
                PrintWriter serverOutput = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
                BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in))
        ) {
            System.out.println("Cliente de prueba conectado como " + role);
            System.out.println("Modo resumen STATE: " + (summaryMode ? "ACTIVADO" : "DESACTIVADO"));
            System.out.println("Comandos rápidos:");
            System.out.println("  a = MOVE_LEFT");
            System.out.println("  d = MOVE_RIGHT");
            System.out.println("  f = FIRE");
            System.out.println("  q = DISCONNECT");
            System.out.println("  También puedes escribir mensajes completos como ALIEN_HIT|playerId=1|alienId=3");
            System.out.println();

            serverOutput.println("HELLO|role=" + role + "|name=" + name);

            Thread readerThread = new Thread(() -> readFromServer(serverInput), "ServerReader");
            readerThread.setDaemon(true);
            readerThread.start();

            String line;

            while ((line = consoleInput.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty()) {
                    continue;
                }

                if (line.equalsIgnoreCase("q")) {
                    serverOutput.println("DISCONNECT|playerId=0");
                    break;
                }

                if (line.equalsIgnoreCase("a")) {
                    serverOutput.println("ACTION|playerId=0|cmd=MOVE_LEFT");
                    continue;
                }

                if (line.equalsIgnoreCase("d")) {
                    serverOutput.println("ACTION|playerId=0|cmd=MOVE_RIGHT");
                    continue;
                }

                if (line.equalsIgnoreCase("f")) {
                    serverOutput.println("ACTION|playerId=0|cmd=FIRE");
                    continue;
                }

                serverOutput.println(line);
            }

        } catch (IOException exception) {
            System.err.println("Error en cliente de prueba: " + exception.getMessage());
        }
    }

    private static int parsePort(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException exception) {
            System.out.println("Puerto inválido. Se usará el puerto por defecto 5000.");
            return 5000;
        }
    }

    private static void readFromServer(BufferedReader serverInput) {
        try {
            String line;

            while ((line = serverInput.readLine()) != null) {
                if (summaryMode && line.startsWith("STATE|")) {
                    printStateSummary(line);
                } else {
                    System.out.println("SERVER: " + line);
                }
            }
        } catch (IOException exception) {
            System.out.println("Conexión cerrada con el servidor.");
        }
    }

    private static void printStateSummary(String stateLine) {
        stateCounter++;

        // Imprimir solo 1 de cada 8 STATE para no saturar la consola.
        if (stateCounter % 8 != 0) {
            return;
        }

        String status = getField(stateLine, "status");
        String speed = getField(stateLine, "speed");
        String players = getField(stateLine, "players");
        String aliens = getField(stateLine, "aliens");
        String bunkers = getField(stateLine, "bunkers");
        String playerShots = getField(stateLine, "playerShots");
        String enemyShots = getField(stateLine, "enemyShots");

        int aliveAliens = countAliveAliens(aliens);
        int playerCount = countItems(players);
        int playerShotCount = countItems(playerShots);
        int enemyShotCount = countItems(enemyShots);

        System.out.println(
                "STATE resumen -> " +
                        "status=" + status +
                        ", speed=" + speed +
                        ", players=" + playerCount +
                        ", aliensVivos=" + aliveAliens +
                        ", disparosJugador=" + playerShotCount +
                        ", disparosEnemigos=" + enemyShotCount +
                        ", bunkers=" + summarizeBunkers(bunkers)
        );
    }

    private static String getField(String line, String key) {
        String[] parts = line.split("\\|");

        for (String part : parts) {
            if (part.startsWith(key + "=")) {
                return part.substring((key + "=").length());
            }
        }

        return "";
    }

    private static int countItems(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }

        return text.split(";").length;
    }

    private static int countAliveAliens(String aliens) {
        if (aliens == null || aliens.isBlank()) {
            return 0;
        }

        String[] items = aliens.split(";");
        int alive = 0;

        for (String item : items) {
            String[] fields = item.split(",");

            if (fields.length >= 5 && fields[4].equals("1")) {
                alive++;
            }
        }

        return alive;
    }

    private static String summarizeBunkers(String bunkers) {
        if (bunkers == null || bunkers.isBlank()) {
            return "sin datos";
        }

        StringBuilder builder = new StringBuilder();
        String[] items = bunkers.split(";");

        for (String item : items) {
            String[] fields = item.split(",");

            if (fields.length >= 4) {
                if (!builder.isEmpty()) {
                    builder.append(" ");
                }

                builder.append("B")
                        .append(fields[0])
                        .append("=")
                        .append(fields[3])
                        .append("%");
            }
        }

        return builder.toString();
    }
}