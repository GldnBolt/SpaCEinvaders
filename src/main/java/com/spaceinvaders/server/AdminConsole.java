package com.spaceinvaders.server;

import com.spaceinvaders.protocol.AdminCommandParser;
import com.spaceinvaders.protocol.Message;

import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Consola administrativa del servidor.
 * Permite escribir los mensajes pedidos en la especificación del proyecto.
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
                String line = scanner.nextLine();

                if (line.equalsIgnoreCase("salir")) {
                    System.out.println("Cerrando servidor...");
                    System.exit(0);
                }

                if (line.equalsIgnoreCase("ayuda")) {
                    printHelp();
                    continue;
                }

                Message message = adminCommandParser.parseAdminCommand(line);
                server.processAdminMessage(message);

            } catch (NoSuchElementException exception) {
                System.out.println("Entrada estándar cerrada. La consola administrativa se detendrá.");
                break;
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
