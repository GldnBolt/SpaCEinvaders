@echo off
cd /d "%~dp0.."

echo Compilando cliente grafico C con raylib...

gcc client\graphics\main_raylib.c client\src\game.c client\src\network.c ^
-Iclient\include -Iclient\src ^
-o client\graphics\spaceinvaders_graphic.exe ^
-lraylib -lopengl32 -lgdi32 -lwinmm -lws2_32

if %errorlevel% neq 0 (
    echo.
    echo ERROR: No se pudo compilar el cliente grafico.
    pause
    exit /b 1
)

echo.
echo Cliente grafico compilado correctamente:
echo client\graphics\spaceinvaders_graphic.exe