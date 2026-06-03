package com.spaceinvaders.model;

import com.spaceinvaders.patterns.factory.EntityFactory;
import com.spaceinvaders.protocol.MessageBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Mantiene el estado principal del juego.
 * En spaCEinvaders, el servidor Java es dueño de toda la lógica del juego.
 */
public class GameState {
    private static final int SCREEN_WIDTH = 800;
    private static final int PLAYER_Y = 430;

    private final EntityFactory entityFactory;

    private final Map<Integer, Player> players;
    private final List<Alien> aliens;
    private final List<Bunker> bunkers;
    private final List<Projectile> playerShots;
    private final List<Projectile> enemyShots;

    private UFO ufo;

    private int tick;
    private int speed;
    private String status;

    private int nextAlienId;
    private int nextProjectileId;

    public GameState() {
        this.entityFactory = new EntityFactory();

        this.players = new LinkedHashMap<>();
        this.aliens = new ArrayList<>();
        this.bunkers = new ArrayList<>();
        this.playerShots = new ArrayList<>();
        this.enemyShots = new ArrayList<>();

        this.ufo = entityFactory.createUFO(false, 0, 0, "NONE", 0);

        this.tick = 0;
        this.speed = 100;
        this.status = "WAITING";

        this.nextAlienId = 1;
        this.nextProjectileId = 1;

        createInitialBunkers();
        createInitialAliens();
    }

    private void createInitialBunkers() {
        bunkers.add(entityFactory.createBunker(1, 100, 380, 100));
        bunkers.add(entityFactory.createBunker(2, 250, 380, 100));
        bunkers.add(entityFactory.createBunker(3, 400, 380, 100));
        bunkers.add(entityFactory.createBunker(4, 550, 380, 100));
    }

    /**
     * Crea una matriz inicial sencilla de aliens para que el cliente tenga algo que dibujar.
     * Más adelante se puede reemplazar por una creación configurable desde la consola.
     */
    private void createInitialAliens() {
        int startX = 80;
        int startY = 70;
        int gapX = 55;
        int gapY = 40;

        for (int row = 0; row < 3; row++) {
            int points = switch (row) {
                case 0 -> 40; // pulpo
                case 1 -> 20; // cangrejo
                default -> 10; // calamar
            };

            for (int col = 0; col < 6; col++) {
                aliens.add(entityFactory.createAlien(
                        nextAlienId++,
                        startX + col * gapX,
                        startY + row * gapY,
                        points
                ));
            }
        }
    }

    public synchronized Player addPlayer(int id, String name) {
        int initialX = id == 1 ? 120 : 300;

        Player player = entityFactory.createPlayer(id, name, initialX, PLAYER_Y);
        players.put(id, player);

        status = "RUNNING";
        tick++;

        return player;
    }

    public synchronized void removePlayer(int playerId) {
        if (playerId > 0) {
            players.remove(playerId);
        }

        if (players.isEmpty()) {
            status = "WAITING";
        }

        tick++;
    }

    public synchronized int getPlayerCount() {
        return players.size();
    }

    public synchronized void movePlayerLeft(int playerId) {
        Player player = players.get(playerId);

        if (player != null) {
            player.moveLeft();
            tick++;
        }
    }

    public synchronized void movePlayerRight(int playerId) {
        Player player = players.get(playerId);

        if (player != null) {
            player.moveRight();
            tick++;
        }
    }

    public synchronized void firePlayerShot(int playerId) {
        Player player = players.get(playerId);

        if (player != null) {
            Projectile shot = entityFactory.createProjectile(
                    nextProjectileId++,
                    player.getX() + 15,
                    player.getY() - 20
            );

            playerShots.add(shot);
            tick++;
        }
    }

    public synchronized Alien createAlien(int x, int y, int points) {
        Alien alien = entityFactory.createAlien(nextAlienId++, x, y, points);
        aliens.add(alien);
        tick++;

        return alien;
    }

    public synchronized void createUFO(String direction, int points) {
        int startX = direction.equalsIgnoreCase("I-D") ? 0 : SCREEN_WIDTH;
        this.ufo = entityFactory.createUFO(true, startX, 40, direction, points);
        tick++;
    }

    public synchronized void setSpeed(int speed) {
        if (speed < 1) {
            this.speed = 1;
        } else {
            this.speed = speed;
        }

        tick++;
    }

    public synchronized void setBunkersHealth(int health) {
        for (Bunker bunker : bunkers) {
            bunker.setHealth(health);
        }

        tick++;
    }

    /**
     * En este avance el cliente puede informar un impacto, pero el servidor es quien valida
     * que el alien exista y esté vivo antes de sumar puntos.
     */
    public synchronized void registerAlienHit(int playerId, int alienId) {
        Player player = players.get(playerId);

        if (player == null) {
            return;
        }

        for (Alien alien : aliens) {
            if (alien.getId() == alienId && alien.isAlive()) {
                alien.destroy();
                player.addScore(alien.getPoints());
                tick++;
                break;
            }
        }
    }

    public synchronized String toProtocolMessage() {
        return new MessageBuilder("STATE")
                .addInt("tick", tick)
                .add("status", status)
                .addInt("speed", speed)
                .add("players", buildPlayersText())
                .add("aliens", buildAliensText())
                .add("bunkers", buildBunkersText())
                .add("ufo", buildUfoText())
                .add("playerShots", buildProjectilesText(playerShots))
                .add("enemyShots", buildProjectilesText(enemyShots))
                .build();
    }

    private String buildPlayersText() {
        StringJoiner joiner = new StringJoiner(";");

        for (Player player : players.values()) {
            joiner.add(
                    player.getId() + "," +
                            player.getX() + "," +
                            player.getY() + "," +
                            player.getLives() + "," +
                            player.getScore()
            );
        }

        return joiner.toString();
    }

    private String buildAliensText() {
        StringJoiner joiner = new StringJoiner(";");

        for (Alien alien : aliens) {
            joiner.add(
                    alien.getId() + "," +
                            alien.getX() + "," +
                            alien.getY() + "," +
                            alien.getPoints() + "," +
                            (alien.isAlive() ? 1 : 0)
            );
        }

        return joiner.toString();
    }

    private String buildBunkersText() {
        StringJoiner joiner = new StringJoiner(";");

        for (Bunker bunker : bunkers) {
            joiner.add(
                    bunker.getId() + "," +
                            bunker.getX() + "," +
                            bunker.getY() + "," +
                            bunker.getHealth()
            );
        }

        return joiner.toString();
    }

    private String buildUfoText() {
        return (ufo.isActive() ? 1 : 0) + "," +
                ufo.getX() + "," +
                ufo.getY() + "," +
                ufo.getDirection() + "," +
                ufo.getPoints();
    }

    private String buildProjectilesText(List<Projectile> projectiles) {
        StringJoiner joiner = new StringJoiner(";");

        for (Projectile projectile : projectiles) {
            joiner.add(
                    projectile.getId() + "," +
                            projectile.getX() + "," +
                            projectile.getY()
            );
        }

        return joiner.toString();
    }
}
