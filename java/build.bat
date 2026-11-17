@echo off
cd /d "%~dp0"

if not exist out mkdir out

dir /s /b src\*.java > sources.txt

javac -d out -cp "..\lib\mariadb-java-client.jar" @sources.txt

if %errorlevel% equ 0 (
    echo Compiled OK
) else (
    echo Compilation Failed
)

pause