package com.spaceinvaders.model;

/**
 * Representa un bunker o escudo terrestre.
 */
public class Bunker {
    private final int id;
    private final int x;
    private final int y;
    private int health;

    public Bunker(int id, int x, int y, int health) {
        this.id = id;
        this.x = x;
        this.y = y;
        setHealth(health);
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

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        if (health < 0) {
            this.health = 0;
        } else if (health > 100) {
            this.health = 100;
        } else {
            this.health = health;
        }
    }

    public void damage(int amount) {
        setHealth(health - amount);
    }
}
