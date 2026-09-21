package com.spaceinvaders.patterns.observer;

/**
 * Patrón Observer.
 * Cada cliente conectado actúa como observador del estado del juego.
 */
public interface GameObserver {
    void onGameMessage(String message);
}
