#!/bin/bash
# run.sh — Compiles (if needed) and runs the application
set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC="$SCRIPT_DIR/src"
OUT="$SCRIPT_DIR/out"
mkdir -p "$OUT"
javac -d "$OUT" "$SRC"/*.java
echo "🚁  Launching Smart Drone Routing & Geofencing System..."
java -cp "$OUT" Main
