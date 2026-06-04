package com.spaceinvaders.game;

import com.spaceinvaders.server.GameServer;

/**
 * Ciclo principal del juego.
 *
 * Este hilo actualiza periódicamente el estado del juego en el servidor:
 * - movimiento de disparos,
 * - movimiento de aliens,
 * - colisiones,
 * - disparos enemigos,
 * - reinicio de ronda.
 */
public class GameLoop implements Runnable {
    private static final int FRAME_DELAY_MS = 120;

    private final GameServer server;
    private boolean running;

    public GameLoop(GameServer server) {
        this.server = server;
        this.running = true;
    }

    @Override
    public void run() {
        while (running) {
            try {
                server.updateGameFrame();
                Thread.sleep(FRAME_DELAY_MS);
            } catch (InterruptedException exception) {
                running = false;
                Thread.currentThread().interrupt();
            }
        }
    }

    public void stop() {
        running = false;
    }
}