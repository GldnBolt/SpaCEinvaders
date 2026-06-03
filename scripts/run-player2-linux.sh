#!/usr/bin/env bash
cd "$(dirname "$0")/.." || exit 1
mvn exec:java -Dexec.mainClass="com.spaceinvaders.tools.ConsoleTestClient" -Dexec.args="127.0.0.1 5000 PLAYER Jugador2"
