@echo off
cd /d "%~dp0.."

set PORT=%1

if "%PORT%"=="" (
    set /p PORT=Ingrese el puerto COM de la ESP32, por ejemplo COM5: 
)

if not exist client\graphics\spaceinvaders_graphic.exe (
    echo No existe el ejecutable grafico. Compilando...
    call scripts\build-graphic-client.bat
)

echo Ejecutando cliente grafico con control fisico UART en %PORT%...
client\graphics\spaceinvaders_graphic.exe uart %PORT%