#!/bin/sh
# starts the server. usage: ./run-server.sh [port] [dataFolder]
cd "$(dirname "$0")"
OUT=out/production/Beep_Messenger
mkdir -p "$OUT"
# compile if a JDK is installed, otherwise use the ready classes in out/
if ! javac -nowarn -source 8 -target 8 -d "$OUT" src/common/*.java src/serverend/*.java src/clientend/*.java >/dev/null 2>&1; then
  echo "(no JDK found, using the ready-made classes in out/)"
fi
exec java -cp "$OUT" serverend.ServerSide "$@"
