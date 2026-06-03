package com.spaceinvaders.model;

/**
 * Representa el platillo volador.
 */
public class UFO {
    private boolean active;
    private int x;
    private int y;
    private String direction;
    private int points;

    public UFO(boolean active, int x, int y, String direction, int points) {
        this.active = active;
        this.x = x;
        this.y = y;
        this.direction = direction;
        this.points = points;
    }

    public boolean isActive() {
        return active;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public String getDirection() {
        return direction;
    }

    public int getPoints() {
        return points;
    }

    public void move(int deltaX) {
        x += deltaX;
    }

    public void deactivate() {
        active = false;
    }
}
