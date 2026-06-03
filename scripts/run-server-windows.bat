@echo off
cd /d %~dp0\..
mvn exec:java
pause
