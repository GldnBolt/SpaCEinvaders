package com.spaceinvaders.server;

import com.spaceinvaders.game.GameLoop;
import com.spaceinvaders.model.Alien;
import com.spaceinvaders.model.GameState;
import com.spaceinvaders.patterns.adapter.NetworkCommand;
import com.spaceinvaders.patterns.adapter.NetworkCommandAdapter;
import com.spaceinvaders.patterns.observer.GameObserver;
import com.spaceinvaders.patterns.observer.GameSubject;
import com.spaceinvaders.protocol.Message;
import com.spaceinvaders.protocol.MessageBuilder;
import com.spaceinvaders.protocol.MessageParser;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servidor principal del juego.
 *
 * Responsabilidades:
 * - abrir el socket del servidor,
 * - aceptar clientes,
 * - registrar jugadores y espectadores,
 * - procesar comandos,
 * - actualizar el estado del juego,
 * - notificar a los clientes conectados.
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

    private volatile boolean running;
    private ServerSocket serverSocket;

    public GameServer(int port) {
        this.port = port;
        this.gameState = new GameState();
        this.messageParser = new MessageParser();
        this.commandAdapter = new NetworkCommandAdapter();

        this.observers = new CopyOnWriteArrayList<>();
        this.clients = new CopyOnWriteArrayList<>();

        this.nextPlayerId = new AtomicInteger(1);
        this.running = false;
    }

    public void start() throws IOException {
        /*
         * Importante:
         * Primero intentamos abrir el ServerSocket.
         * Si el puerto está ocupado, fallará aquí y NO se abrirá AdminConsole.
         * Esto evita hilos pegados cuando hay error de puerto ocupado.
         */
        serverSocket = new ServerSocket(port);
        running = true;

        Thread gameLoopThread = new Thread(new GameLoop(this), "GameLoop");
        gameLoopThread.setDaemon(true);
        gameLoopThread.start();

        Thread adminThread = new Thread(new AdminConsole(this), "AdminConsole");
        adminThread.start();

        System.out.println("Servidor spaCEinvaders iniciado en puerto " + port);
        System.out.println("Esperando clientes...");

        while (running) {
            try {
                Socket socket = serverSocket.accept();

                ClientHandler handler = new ClientHandler(socket, this);
                clients.add(handler);

                Thread clientThread = new Thread(handler, "ClientHandler");
                clientThread.start();

            } catch (SocketException exception) {
                if (running) {
                    System.err.println("Error de socket en servidor: " + exception.getMessage());
                }
            }
        }

        System.out.println("Servidor detenido.");
    }

    public synchronized void stop() {
        if (!running) {
            return;
        }

        running = false;

        System.out.println("Cerrando clientes conectados...");

        for (ClientHandler client : clients) {
            client.close();
        }

        clients.clear();
        observers.clear();

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException exception) {
            System.err.println("Error cerrando ServerSocket: " + exception.getMessage());
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

        if (role == ClientRole.SPECTATOR) {
            addObserver(clientHandler);

            System.out.println("Espectador registrado: " + name);

            return RegistrationResult.success(0);
        }

        return RegistrationResult.failure("Rol no reconocido");
    }

    public synchronized void unregisterClient(ClientHandler clientHandler) {
        if (clientHandler == null) {
            return;
        }

        removeObserver(clientHandler);
        clients.remove(clientHandler);

        if (clientHandler.getRole() == ClientRole.PLAYER) {
            gameState.removePlayer(clientHandler.getPlayerId());
            broadcastState();
            System.out.println("Jugador desconectado: " + clientHandler.getName());
        } else if (clientHandler.getRole() == ClientRole.SPECTATOR) {
            System.out.println("Espectador desconectado: " + clientHandler.getName());
        } else {
            System.out.println("Cliente desconectado antes de registrarse.");
        }
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

                    if (direction == null) {
                        System.out.println("Dirección de OVNI inválida");
                        return;
                    }

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
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
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