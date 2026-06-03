package com.spaceinvaders.server;

import com.spaceinvaders.patterns.observer.GameObserver;
import com.spaceinvaders.protocol.Message;
import com.spaceinvaders.protocol.MessageBuilder;
import com.spaceinvaders.protocol.MessageParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Atiende a un cliente conectado.
 * Cada cliente tiene su propio hilo.
 */
public class ClientHandler implements Runnable, GameObserver {
    private final Socket socket;
    private final GameServer server;
    private final MessageParser messageParser;

    private BufferedReader input;
    private PrintWriter output;

    private ClientRole role;
    private int playerId;
    private String name;

    public ClientHandler(Socket socket, GameServer server) {
        this.socket = socket;
        this.server = server;
        this.messageParser = new MessageParser();

        this.role = null;
        this.playerId = 0;
        this.name = "SinNombre";
    }

    @Override
    public void run() {
        try {
            input = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
            );

            output = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),
                    true
            );

            String helloLine = input.readLine();

            if (!handleHello(helloLine)) {
                close();
                return;
            }

            String line;

            while ((line = input.readLine()) != null) {
                server.processClientLine(this, line);
            }

        } catch (IOException exception) {
            System.out.println("Cliente desconectado inesperadamente: " + exception.getMessage());
        } finally {
            server.unregisterClient(this);
            close();
        }
    }

    private boolean handleHello(String helloLine) {
        try {
            Message message = messageParser.parse(helloLine);

            if (!message.getType().equals("HELLO")) {
                sendError("EXPECTED_HELLO", "El primer mensaje debe ser HELLO");
                return false;
            }

            this.role = ClientRole.fromText(message.get("role"));

            String receivedName = message.get("name");

            if (receivedName != null && !receivedName.isBlank()) {
                this.name = receivedName;
            }

            RegistrationResult result = server.registerClient(this, role, name);

            if (!result.isSuccess()) {
                sendError("REGISTRATION_FAILED", result.getMessage());
                return false;
            }

            this.playerId = result.getPlayerId();

            sendMessage(
                    new MessageBuilder("WELCOME")
                            .add("role", role.name())
                            .addInt("playerId", playerId)
                            .addInt("maxPlayers", 2)
                            .build()
            );

            sendMessage(server.getCurrentStateMessage());

            return true;

        } catch (Exception exception) {
            sendError("BAD_HELLO", exception.getMessage());
            return false;
        }
    }

    public void sendMessage(String message) {
        if (output != null) {
            output.println(message);
            output.flush();
        }
    }

    public void sendError(String code, String message) {
        String safeMessage = message == null ? "Sin_detalle" : message.replace(" ", "_");

        sendMessage(
                new MessageBuilder("ERROR")
                        .add("code", code)
                        .add("message", safeMessage)
                        .build()
        );
    }

    @Override
    public void onGameMessage(String message) {
        sendMessage(message);
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    public ClientRole getRole() {
        return role;
    }

    public int getPlayerId() {
        return playerId;
    }

    public String getName() {
        return name;
    }
}
