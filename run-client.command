#!/bin/sh
# starts one client window. run it again for a second user.
# usage: ./run-client.sh [serverHost] [serverPort]
cd "$(dirname "$0")"
OUT=out/production/Beep_Messenger
mkdir -p "$OUT"
if ! javac -nowarn -source 8 -target 8 -d "$OUT" src/common/*.java src/serverend/*.java src/clientend/*.java >/dev/null 2>&1; then
  echo "(no JDK found, using the ready-made classes in out/)"
fi
exec java -cp "$OUT" clientend.ClientSide "$@"
