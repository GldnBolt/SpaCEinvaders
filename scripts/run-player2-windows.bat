@echo off
cd /d %~dp0\..
mvn exec:java -Dexec.mainClass="com.spaceinvaders.tools.ConsoleTestClient" -Dexec.args="127.0.0.1 5000 PLAYER Jugador2"
pause
