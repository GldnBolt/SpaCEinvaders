@echo off
cd /d "%~dp0.."

if not exist client\graphics\spaceinvaders_graphic.exe (
    echo No existe el ejecutable grafico. Compilando...
    call scripts\build-graphic-client.bat
)

echo Ejecutando cliente grafico como SPECTATOR...
client\graphics\spaceinvaders_graphic.exe spectator