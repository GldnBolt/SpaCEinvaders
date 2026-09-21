package com.spaceinvaders.server;

import com.spaceinvaders.protocol.AdminCommandParser;
import com.spaceinvaders.protocol.Message;

import java.util.Scanner;

/**
 * Consola administrativa del servidor.
 *
 * Permite ingresar los comandos administrativos solicitados:
 * - Crear (X,Y,Pts)
 * - OVNI I-D 1500
 * - Velocidad 100
 * - Bunkers 70%
 */
public class AdminConsole implements Runnable {
    private final GameServer server;
    private final AdminCommandParser adminCommandParser;

    public AdminConsole(GameServer server) {
        this.server = server;
        this.adminCommandParser = new AdminCommandParser();
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);

        printHelp();

        while (true) {
            try {
                System.out.print("admin> ");

                if (!scanner.hasNextLine()) {
                    server.stop();
                    break;
                }

                String line = scanner.nextLine().trim();

                if (line.isEmpty()) {
                    continue;
                }

                if (line.equalsIgnoreCase("salir")) {
                    System.out.println("Cerrando servidor...");
                    server.stop();
                    break;
                }

                if (line.equalsIgnoreCase("ayuda")) {
                    printHelp();
                    continue;
                }

                Message message = adminCommandParser.parseAdminCommand(line);
                server.processAdminMessage(message);

            } catch (Exception exception) {
                System.out.println("Comando inválido: " + exception.getMessage());
            }
        }
    }

    private void printHelp() {
        System.out.println("Comandos administrativos disponibles:");
        System.out.println("  Crear (X,Y,Pts)");
        System.out.println("  OVNI I-D 1500");
        System.out.println("  OVNI D-I 1500");
        System.out.println("  Velocidad 100");
        System.out.println("  Bunkers 70%");
        System.out.println("  ayuda");
        System.out.println("  salir");
    }
}