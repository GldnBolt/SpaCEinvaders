package com.spaceinvaders.model;

/**
 * Representa a un jugador conectado a la partida.
 * El jugador inicia con 3 vidas, como solicita la especificación.
 */
public class Player {
    public static final int MIN_X = 0;
    public static final int MAX_X = 760;
    public static final int STEP = 20;

    private final int id;
    private final String name;
    private int x;
    private int y;
    private int lives;
    private int score;

    public Player(int id, String name, int x, int y) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.lives = 3;
        this.score = 0;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getLives() {
        return lives;
    }

    public int getScore() {
        return score;
    }

    public void moveLeft() {
        x -= STEP;
        if (x < MIN_X) {
            x = MIN_X;
        }
    }

    public void moveRight() {
        x += STEP;
        if (x > MAX_X) {
            x = MAX_X;
        }
    }

    public void addScore(int points) {
        if (points > 0) {
            score += points;
        }
    }

    public void loseLife() {
        lives--;
        if (lives < 0) {
            lives = 0;
        }
    }

    public void addLife() {
        lives++;
    }
}
