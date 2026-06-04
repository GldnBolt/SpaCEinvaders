package com.spaceinvaders.server;

import com.spaceinvaders.game.GameLoop;
import com.spaceinvaders.model.Alien;
import com.spaceinvaders.model.GameState;
import com.spaceinvaders.patterns.observer.GameObserver;
import com.spaceinvaders.patterns.observer.GameSubject;
import com.spaceinvaders.protocol.Message;
import com.spaceinvaders.protocol.MessageBuilder;
import com.spaceinvaders.protocol.MessageParser;
import com.spaceinvaders.patterns.adapter.NetworkCommand;
import com.spaceinvaders.patterns.adapter.NetworkCommandAdapter;
import com.spaceinvaders.patterns.adapter.NetworkCommandType;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servidor principal del juego.
 * Acepta clientes, procesa comandos y notifica cambios de estado.
 */
public class GameServer implements GameSubject {
    private static final int MAX_PLAYERS = 2;

    private final int port;
    private final GameState gameState;
    private final MessageParser messageParser;
    private final NetworkCommandAdapter commandAdapter;

    private final List<GameObserver> observers;
    private final List<ClientHandler> clients;

    private final AtomicInteger nextPlayerId;

    public GameServer(int port) {
        this.port = port;
        this.gameState = new GameState();
        this.messageParser = new MessageParser();
        this.commandAdapter = new NetworkCommandAdapter();

        this.observers = new CopyOnWriteArrayList<>();
        this.clients = new CopyOnWriteArrayList<>();

        this.nextPlayerId = new AtomicInteger(1);
    }

    public void start() throws IOException {
        Thread gameLoopThread = new Thread(new GameLoop(this), "GameLoop");
        gameLoopThread.setDaemon(true);
        gameLoopThread.start();

        Thread adminThread = new Thread(new AdminConsole(this), "AdminConsole");
        adminThread.start();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Servidor spaCEinvaders iniciado en puerto " + port);
            System.out.println("Esperando clientes...");

            while (true) {
                Socket socket = serverSocket.accept();

                ClientHandler handler = new ClientHandler(socket, this);
                clients.add(handler);

                Thread clientThread = new Thread(handler, "ClientHandler");
                clientThread.start();
            }
        }
    }

    public synchronized RegistrationResult registerClient(
            ClientHandler clientHandler,
            ClientRole role,
            String name
    ) {
        if (role == ClientRole.PLAYER) {
            if (gameState.getPlayerCount() >= MAX_PLAYERS) {
                return RegistrationResult.failure("La partida ya tiene 2 jugadores");
            }

            int playerId = nextPlayerId.getAndIncrement();
            gameState.addPlayer(playerId, name);

            addObserver(clientHandler);

            System.out.println("Jugador registrado: " + name + " con ID " + playerId);

            return RegistrationResult.success(playerId);
        }

        addObserver(clientHandler);

        System.out.println("Espectador registrado: " + name);

        return RegistrationResult.success(0);
    }

    public synchronized void unregisterClient(ClientHandler clientHandler) {
        removeObserver(clientHandler);
        clients.remove(clientHandler);

        if (clientHandler.getRole() == ClientRole.PLAYER) {
            gameState.removePlayer(clientHandler.getPlayerId());
            broadcastState();
        }

        System.out.println("Cliente desconectado");
    }

    public synchronized void processClientLine(ClientHandler clientHandler, String line) {
        try {
            Message message = messageParser.parse(line);

            if (clientHandler.getRole() == ClientRole.SPECTATOR &&
                    (message.getType().equals("ACTION") || message.getType().equals("ALIEN_HIT"))) {

                clientHandler.sendError(
                        "SPECTATOR_CANNOT_PLAY",
                        "El espectador no puede enviar acciones"
                );

                return;
            }

            NetworkCommand command = commandAdapter.adapt(message);

            switch (command.getType()) {
                case MOVE_LEFT -> {
                    gameState.movePlayerLeft(clientHandler.getPlayerId());
                    broadcastState();
                }

                case MOVE_RIGHT -> {
                    gameState.movePlayerRight(clientHandler.getPlayerId());
                    broadcastState();
                }

                case FIRE -> {
                    gameState.firePlayerShot(clientHandler.getPlayerId());
                    broadcastState();
                }

                case ALIEN_HIT -> {
                    if (command.getAlienId() == -1) {
                        clientHandler.sendError("BAD_ALIEN_HIT", "Falta alienId");
                        return;
                    }

                    gameState.registerAlienHit(clientHandler.getPlayerId(), command.getAlienId());
                    broadcastState();
                }

                case DISCONNECT -> clientHandler.close();

                case UNKNOWN -> clientHandler.sendError(
                        "UNKNOWN_MESSAGE",
                        "Mensaje no reconocido"
                );
            }

        } catch (Exception exception) {
            clientHandler.sendError("BAD_MESSAGE", exception.getMessage());
        }
    }

    private void processAction(ClientHandler clientHandler, Message message) {
        String command = message.get("cmd");
        int playerId = clientHandler.getPlayerId();

        if (command == null) {
            clientHandler.sendError("BAD_ACTION", "La acción no tiene cmd");
            return;
        }

        switch (command) {
            case "MOVE_LEFT" -> gameState.movePlayerLeft(playerId);
            case "MOVE_RIGHT" -> gameState.movePlayerRight(playerId);
            case "FIRE" -> gameState.firePlayerShot(playerId);
            default -> {
                clientHandler.sendError("BAD_ACTION", "Acción no reconocida: " + command);
                return;
            }
        }

        broadcastState();
    }

    private void processAlienHit(ClientHandler clientHandler, Message message) {
        int alienId = message.getInt("alienId", -1);

        if (alienId == -1) {
            clientHandler.sendError("BAD_ALIEN_HIT", "Falta alienId");
            return;
        }

        gameState.registerAlienHit(clientHandler.getPlayerId(), alienId);
        broadcastState();
    }

    public synchronized void processAdminMessage(Message message) {
        try {
            switch (message.getType()) {
                case "ADMIN_CREATE_ALIEN" -> {
                    int x = message.getInt("x", 0);
                    int y = message.getInt("y", 0);
                    int points = message.getInt("points", 10);

                    Alien alien = gameState.createAlien(x, y, points);

                    notifyObservers(
                            new MessageBuilder("CREATE_ALIEN")
                                    .addInt("id", alien.getId())
                                    .addInt("x", alien.getX())
                                    .addInt("y", alien.getY())
                                    .addInt("points", alien.getPoints())
                                    .build()
                    );

                    broadcastState();
                }

                case "ADMIN_CREATE_UFO" -> {
                    String direction = message.get("direction");
                    int points = message.getInt("points", 0);

                    gameState.createUFO(direction, points);

                    notifyObservers(
                            new MessageBuilder("CREATE_UFO")
                                    .add("direction", direction)
                                    .addInt("points", points)
                                    .build()
                    );

                    broadcastState();
                }

                case "ADMIN_SET_SPEED" -> {
                    int value = message.getInt("value", 100);

                    gameState.setSpeed(value);

                    notifyObservers(
                            new MessageBuilder("SET_SPEED")
                                    .addInt("value", value)
                                    .build()
                    );

                    broadcastState();
                }

                case "ADMIN_SET_BUNKERS" -> {
                    int health = message.getInt("health", 100);

                    gameState.setBunkersHealth(health);

                    notifyObservers(
                            new MessageBuilder("SET_BUNKERS")
                                    .addInt("health", health)
                                    .build()
                    );

                    broadcastState();
                }

                default -> System.out.println("Comando administrativo no reconocido");
            }

        } catch (Exception exception) {
            System.out.println("Error procesando comando administrativo: " + exception.getMessage());
        }
    }

    /**
     * Método llamado por GameLoop.
     * Si el estado cambió, se notifica a todos los clientes.
     */
    public synchronized void updateGameFrame() {
        boolean changed = gameState.updateFrame();

        if (changed) {
            broadcastState();
        }
    }

    public synchronized String getCurrentStateMessage() {
        return gameState.toProtocolMessage();
    }

    public synchronized void broadcastState() {
        notifyObservers(gameState.toProtocolMessage());
    }

    @Override
    public void addObserver(GameObserver observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(GameObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(String message) {
        for (GameObserver observer : observers) {
            observer.onGameMessage(message);
        }
    }
}