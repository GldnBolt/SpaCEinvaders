package com.spaceinvaders.patterns.observer;

/**
 * Sujeto observado dentro del patrón Observer.
 */
public interface GameSubject {
    void addObserver(GameObserver observer);

    void removeObserver(GameObserver observer);

    void notifyObservers(String message);
}
