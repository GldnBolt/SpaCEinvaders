#!/usr/bin/env bash
cd "$(dirname "$0")/.." || exit 1
java -cp target/classes com.spaceinvaders.tools.ConsoleTestClient 127.0.0.1 5000 PLAYER Jugador2
