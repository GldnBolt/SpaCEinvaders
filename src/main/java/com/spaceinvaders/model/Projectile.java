package com.spaceinvaders.model;

/**
 * Representa un disparo.
 *
 * ownerId indica quién disparó.
 * fromPlayer indica si el disparo viene de un jugador o de un alien.
 */
public class Projectile {
    private final int id;
    private final int ownerId;
    private int x;
    private int y;
    private final boolean fromPlayer;
    private boolean active;

    public Projectile(int id, int ownerId, int x, int y, boolean fromPlayer) {
        this.id = id;
        this.ownerId = ownerId;
        this.x = x;
        this.y = y;
        this.fromPlayer = fromPlayer;
        this.active = true;
    }

    public int getId() {
        return id;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean isFromPlayer() {
        return fromPlayer;
    }

    public boolean isActive() {
        return active;
    }

    public void move(int dy) {
        this.y += dy;
    }

    public void deactivate() {
        this.active = false;
    }
}