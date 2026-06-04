package com.spaceinvaders.patterns.factory;

import com.spaceinvaders.model.Alien;
import com.spaceinvaders.model.Bunker;
import com.spaceinvaders.model.Player;
import com.spaceinvaders.model.Projectile;
import com.spaceinvaders.model.UFO;

/**
 * Patrón Factory.
 * Centraliza la creación de entidades del juego.
 *
 * Esto ayuda a que el servidor no cree objetos directamente en todas partes,
 * sino que use una clase especializada para construir las entidades.
 */
public class EntityFactory {

    public Player createPlayer(int id, String name, int x, int y) {
        return new Player(id, name, x, y);
    }

    public Alien createAlien(int id, int x, int y, int points) {
        return new Alien(id, x, y, points);
    }

    public Bunker createBunker(int id, int x, int y, int health) {
        return new Bunker(id, x, y, health);
    }

    public UFO createUFO(boolean active, int x, int y, String direction, int points) {
        return new UFO(active, x, y, direction, points);
    }

    public Projectile createProjectile(int id, int ownerId, int x, int y, boolean fromPlayer) {
        return new Projectile(id, ownerId, x, y, fromPlayer);
    }
}