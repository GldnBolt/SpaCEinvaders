package com.spaceinvaders.model;

import com.spaceinvaders.patterns.factory.EntityFactory;
import com.spaceinvaders.protocol.MessageBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringJoiner;

/**
 * Mantiene el estado completo del juego.
 * Toda la lógica principal se ejecuta aquí en el servidor Java.
 */
public class GameState {
    private static final int SCREEN_WIDTH = 800;
    private static final int SCREEN_HEIGHT = 480;

    private static final int PLAYER_Y = 430;
    private static final int PLAYER_WIDTH = 40;
    private static final int PLAYER_HEIGHT = 25;
    private static final int PLAYER_STEP = 20;

    private static final int ALIEN_WIDTH = 30;
    private static final int ALIEN_HEIGHT = 25;
    private static final int ALIEN_STEP = 15;
    private static final int ALIEN_DROP = 20;

    private static final int BUNKER_WIDTH = 60;
    private static final int BUNKER_HEIGHT = 35;

    private static final int PLAYER_SHOT_SPEED = -18;
    private static final int ENEMY_SHOT_SPEED = 10;

    private final EntityFactory entityFactory;
    private final Random random;

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

    private int alienDirection;
    private int alienMoveCounter;
    private int enemyFireCounter;

    public GameState() {
        this.entityFactory = new EntityFactory();
        this.random = new Random();

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

        this.alienDirection = 1;
        this.alienMoveCounter = 0;
        this.enemyFireCounter = 0;

        createInitialBunkers();
        createInitialAliens();
    }

    private void createInitialBunkers() {
        bunkers.clear();

        bunkers.add(entityFactory.createBunker(1, 100, 380, 100));
        bunkers.add(entityFactory.createBunker(2, 250, 380, 100));
        bunkers.add(entityFactory.createBunker(3, 400, 380, 100));
        bunkers.add(entityFactory.createBunker(4, 550, 380, 100));
    }

    private void createInitialAliens() {
        aliens.clear();

        int startX = 80;
        int startY = 70;
        int spacingX = 55;
        int spacingY = 40;

        for (int row = 0; row < 3; row++) {
            int points;

            if (row == 0) {
                points = 40;
            } else if (row == 1) {
                points = 20;
            } else {
                points = 10;
            }

            for (int col = 0; col < 6; col++) {
                int x = startX + col * spacingX;
                int y = startY + row * spacingY;

                aliens.add(entityFactory.createAlien(nextAlienId++, x, y, points));
            }
        }
    }

    public synchronized Player addPlayer(int id, String name) {
        int initialX = id == 1 ? 120 : 300;

        Player player = entityFactory.createPlayer(id, name, initialX, PLAYER_Y);
        players.put(id, player);

        if (!status.equals("GAME_OVER")) {
            status = "RUNNING";
        }

        tick++;

        return player;
    }

    public synchronized void removePlayer(int playerId) {
        if (playerId > 0) {
            players.remove(playerId);
        }

        if (players.isEmpty() && !status.equals("GAME_OVER")) {
            status = "WAITING";
        }

        tick++;
    }

    public synchronized int getPlayerCount() {
        return players.size();
    }

    public synchronized void movePlayerLeft(int playerId) {
        Player player = players.get(playerId);

        if (player != null && status.equals("RUNNING")) {
            player.moveLeft();
            tick++;
        }
    }

    public synchronized void movePlayerRight(int playerId) {
        Player player = players.get(playerId);

        if (player != null && status.equals("RUNNING")) {
            player.moveRight();
            tick++;
        }
    }

    public synchronized void firePlayerShot(int playerId) {
        Player player = players.get(playerId);

        if (player != null && status.equals("RUNNING")) {
            Projectile shot = entityFactory.createProjectile(
                    nextProjectileId++,
                    playerId,
                    player.getX() + PLAYER_WIDTH / 2,
                    player.getY() - 10,
                    true
            );

            playerShots.add(shot);
            tick++;
        }
    }

    public synchronized Alien createAlien(int x, int y, int points) {
        Alien alien = entityFactory.createAlien(nextAlienId++, x, y, points);
        aliens.add(alien);

        if (!players.isEmpty() && !status.equals("GAME_OVER")) {
            status = "RUNNING";
        }

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

        checkRoundRestart();
    }

    /**
     * Actualiza un frame del juego.
     * Retorna true si el estado cambió y debe notificarse a los clientes.
     */
    public synchronized boolean updateFrame() {
        if (!status.equals("RUNNING")) {
            return false;
        }

        boolean changed = false;

        changed |= updatePlayerShots();
        changed |= updateEnemyShots();
        changed |= updateAliens();
        changed |= updateUFO();
        changed |= generateEnemyShot();

        checkAliensReachedPlayers();
        checkPlayersAlive();
        checkRoundRestart();

        if (changed) {
            tick++;
        }

        return changed;
    }

    private boolean updatePlayerShots() {
        boolean changed = false;

        for (Projectile shot : playerShots) {
            if (!shot.isActive()) {
                continue;
            }

            shot.move(PLAYER_SHOT_SPEED);
            changed = true;

            if (shot.getY() < 0) {
                shot.deactivate();
                continue;
            }

            checkPlayerShotAgainstAliens(shot);
            checkPlayerShotAgainstUFO(shot);
            checkProjectileAgainstBunkers(shot, 10);
        }

        playerShots.removeIf(shot -> !shot.isActive());

        return changed;
    }

    private void checkPlayerShotAgainstAliens(Projectile shot) {
        for (Alien alien : aliens) {
            if (!alien.isAlive()) {
                continue;
            }

            if (pointInsideRectangle(
                    shot.getX(),
                    shot.getY(),
                    alien.getX(),
                    alien.getY(),
                    ALIEN_WIDTH,
                    ALIEN_HEIGHT
            )) {
                alien.destroy();
                shot.deactivate();

                Player owner = players.get(shot.getOwnerId());

                if (owner != null) {
                    owner.addScore(alien.getPoints());
                }

                break;
            }
        }
    }

    private void checkPlayerShotAgainstUFO(Projectile shot) {
        if (!ufo.isActive()) {
            return;
        }

        if (pointInsideRectangle(
                shot.getX(),
                shot.getY(),
                ufo.getX(),
                ufo.getY(),
                50,
                25
        )) {
            ufo.deactivate();
            shot.deactivate();

            Player owner = players.get(shot.getOwnerId());

            if (owner != null) {
                owner.addScore(ufo.getPoints());
            }
        }
    }

    private boolean updateEnemyShots() {
        boolean changed = false;

        for (Projectile shot : enemyShots) {
            if (!shot.isActive()) {
                continue;
            }

            shot.move(ENEMY_SHOT_SPEED);
            changed = true;

            if (shot.getY() > SCREEN_HEIGHT) {
                shot.deactivate();
                continue;
            }

            checkEnemyShotAgainstPlayers(shot);
            checkProjectileAgainstBunkers(shot, 15);
        }

        enemyShots.removeIf(shot -> !shot.isActive());

        return changed;
    }

    private void checkEnemyShotAgainstPlayers(Projectile shot) {
        for (Player player : players.values()) {
            if (player.getLives() <= 0) {
                continue;
            }

            if (pointInsideRectangle(
                    shot.getX(),
                    shot.getY(),
                    player.getX(),
                    player.getY(),
                    PLAYER_WIDTH,
                    PLAYER_HEIGHT
            )) {
                player.loseLife();
                shot.deactivate();
                break;
            }
        }
    }

    private void checkProjectileAgainstBunkers(Projectile shot, int damage) {
        for (Bunker bunker : bunkers) {
            if (bunker.getHealth() <= 0) {
                continue;
            }

            if (pointInsideRectangle(
                    shot.getX(),
                    shot.getY(),
                    bunker.getX(),
                    bunker.getY(),
                    BUNKER_WIDTH,
                    BUNKER_HEIGHT
            )) {
                bunker.setHealth(bunker.getHealth() - damage);
                shot.deactivate();
                break;
            }
        }
    }

    private boolean updateAliens() {
        alienMoveCounter++;

        int framesBetweenMoves = Math.max(1, 20 - (speed / 10));

        if (alienMoveCounter < framesBetweenMoves) {
            return false;
        }

        alienMoveCounter = 0;

        boolean mustDrop = false;

        for (Alien alien : aliens) {
            if (!alien.isAlive()) {
                continue;
            }

            int nextX = alien.getX() + (ALIEN_STEP * alienDirection);

            if (nextX < 0 || nextX + ALIEN_WIDTH > SCREEN_WIDTH) {
                mustDrop = true;
                break;
            }
        }

        if (mustDrop) {
            alienDirection *= -1;

            for (Alien alien : aliens) {
                if (alien.isAlive()) {
                    alien.move(0, ALIEN_DROP);
                }
            }
        } else {
            for (Alien alien : aliens) {
                if (alien.isAlive()) {
                    alien.move(ALIEN_STEP * alienDirection, 0);
                }
            }
        }

        return true;
    }

    private boolean updateUFO() {
        if (!ufo.isActive()) {
            return false;
        }

        if (ufo.getDirection().equalsIgnoreCase("I-D")) {
            ufo.move(5);

            if (ufo.getX() > SCREEN_WIDTH) {
                ufo.deactivate();
            }
        } else {
            ufo.move(-5);

            if (ufo.getX() < -60) {
                ufo.deactivate();
            }
        }

        return true;
    }

    private boolean generateEnemyShot() {
        enemyFireCounter++;

        if (enemyFireCounter < 25) {
            return false;
        }

        enemyFireCounter = 0;

        List<Alien> aliveAliens = new ArrayList<>();

        for (Alien alien : aliens) {
            if (alien.isAlive()) {
                aliveAliens.add(alien);
            }
        }

        if (aliveAliens.isEmpty()) {
            return false;
        }

        Alien shooter = aliveAliens.get(random.nextInt(aliveAliens.size()));

        Projectile shot = entityFactory.createProjectile(
                nextProjectileId++,
                shooter.getId(),
                shooter.getX() + ALIEN_WIDTH / 2,
                shooter.getY() + ALIEN_HEIGHT,
                false
        );

        enemyShots.add(shot);

        return true;
    }

    private void checkAliensReachedPlayers() {
        for (Alien alien : aliens) {
            if (alien.isAlive() && alien.getY() + ALIEN_HEIGHT >= PLAYER_Y) {
                status = "GAME_OVER";
                return;
            }
        }
    }

    private void checkPlayersAlive() {
        if (players.isEmpty()) {
            return;
        }

        for (Player player : players.values()) {
            if (player.getLives() > 0) {
                return;
            }
        }

        status = "GAME_OVER";
    }

    private void checkRoundRestart() {
        if (!status.equals("RUNNING")) {
            return;
        }

        if (hasAliveAliens()) {
            return;
        }

        for (Player player : players.values()) {
            player.addLife();
        }

        speed += 20;
        playerShots.clear();
        enemyShots.clear();
        ufo = entityFactory.createUFO(false, 0, 0, "NONE", 0);

        createInitialAliens();

        tick++;
    }

    private boolean hasAliveAliens() {
        for (Alien alien : aliens) {
            if (alien.isAlive()) {
                return true;
            }
        }

        return false;
    }

    private boolean pointInsideRectangle(
            int pointX,
            int pointY,
            int rectX,
            int rectY,
            int rectWidth,
            int rectHeight
    ) {
        return pointX >= rectX &&
                pointX <= rectX + rectWidth &&
                pointY >= rectY &&
                pointY <= rectY + rectHeight;
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
            if (projectile.isActive()) {
                joiner.add(
                        projectile.getId() + "," +
                                projectile.getX() + "," +
                                projectile.getY()
                );
            }
        }

        return joiner.toString();
    }
}