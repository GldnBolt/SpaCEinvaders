@echo off
cd /d %~dp0\..
java -cp target\classes com.spaceinvaders.tools.ConsoleTestClient 127.0.0.1 5000 SPECTATOR Espectador1
pause
