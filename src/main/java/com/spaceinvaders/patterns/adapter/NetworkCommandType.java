package com.spaceinvaders.patterns.adapter;

/**
 * Tipos de comandos internos que entiende el servidor.
 */
public enum NetworkCommandType {
    MOVE_LEFT,
    MOVE_RIGHT,
    FIRE,
    ALIEN_HIT,
    DISCONNECT,
    UNKNOWN
}