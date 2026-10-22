@echo off
cd /d "%~dp0"
if not exist out\production\Beep_Messenger mkdir out\production\Beep_Messenger
javac -nowarn -source 8 -target 8 -d out\production\Beep_Messenger src\common\*.java src\serverend\*.java src\clientend\*.java >nul 2>nul
if errorlevel 1 echo (no JDK found, using the ready-made classes in out)
java -cp out\production\Beep_Messenger clientend.ClientSide %*
