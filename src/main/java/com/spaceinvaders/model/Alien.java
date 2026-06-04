package com.spaceinvaders.model;

/**
 * Representa un extraterrestre del juego.
 */
public class Alien {
    private final int id;
    private int x;
    private int y;
    private final int points;
    private boolean alive;

    public Alien(int id, int x, int y, int points) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.points = points;
        this.alive = true;
    }

    public int getId() {
        return id;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getPoints() {
        return points;
    }

    public boolean isAlive() {
        return alive;
    }

    public void move(int dx, int dy) {
        this.x += dx;
        this.y += dy;
    }

    public void destroy() {
        alive = false;
    }
}