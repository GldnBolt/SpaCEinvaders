package com.spaceinvaders.model;

/**
 * Representa un disparo del jugador o de los enemigos.
 */
public class Projectile {
    private final int id;
    private int x;
    private int y;

    public Projectile(int id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;
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

    public void moveY(int deltaY) {
        y += deltaY;
    }
}
