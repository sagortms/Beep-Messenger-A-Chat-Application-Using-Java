@echo off
cd /d "%~dp0"
if not exist out\production\Beep_Messenger mkdir out\production\Beep_Messenger
javac -nowarn -source 8 -target 8 -d out\production\Beep_Messenger src\common\*.java src\serverend\*.java src\clientend\*.java >nul 2>nul
if errorlevel 1 echo (no JDK found, using the ready-made classes in out)
start "Beep server" java -cp out\production\Beep_Messenger serverend.ServerSide
timeout /t 2 /nobreak >nul
start "Beep client 1" java -cp out\production\Beep_Messenger clientend.ClientSide
timeout /t 1 /nobreak >nul
start "Beep client 2" java -cp out\production\Beep_Messenger clientend.ClientSide
