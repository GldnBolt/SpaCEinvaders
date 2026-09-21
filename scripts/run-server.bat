@echo off
echo ==========================================
echo   Servidor Java - spaCEinvaders
echo ==========================================
echo.

cd /d "%~dp0.."

echo Compilando proyecto...
call mvn clean package

if errorlevel 1 (
    echo.
    echo ERROR: No se pudo compilar el servidor.
    pause
    exit /b 1
)

echo.
echo Iniciando servidor en puerto 5000...
echo.
java -jar target/spaceinvaders-server-1.0-SNAPSHOT.jar

pause