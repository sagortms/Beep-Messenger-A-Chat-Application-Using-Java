#!/bin/sh
# starts the server and two client windows, so you can test chat with
# mutant and neon on one computer. close this terminal to stop everything.
cd "$(dirname "$0")"
OUT=out/production/Beep_Messenger
mkdir -p "$OUT"
if ! javac -nowarn -source 8 -target 8 -d "$OUT" src/common/*.java src/serverend/*.java src/clientend/*.java >/dev/null 2>&1; then
  echo "(no JDK found, using the ready-made classes in out/)"
fi
java -cp "$OUT" serverend.ServerSide &
SERVER=$!
trap 'kill $SERVER 2>/dev/null' EXIT INT TERM
sleep 1
java -cp "$OUT" clientend.ClientSide &
sleep 1
java -cp "$OUT" clientend.ClientSide &
wait
