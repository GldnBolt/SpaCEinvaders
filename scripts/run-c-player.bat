@echo off
cd /d "%~dp0\..\client"
if not exist client.exe (
    call "%~dp0\build-client.bat"
)
client.exe
pause
