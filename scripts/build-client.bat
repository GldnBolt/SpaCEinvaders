@echo off
cd /d "%~dp0\..\client"
gcc -Wall -Wextra -Iinclude src\main.c src\game.c src\network.c -lws2_32 -o client.exe
if errorlevel 1 (
    echo Error compilando cliente C.
    exit /b 1
)
echo Cliente C compilado correctamente: client\client.exe
