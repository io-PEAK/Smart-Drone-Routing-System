#!/bin/bash
# compile.sh — Compiles all Java source files in src/ into out/
set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC="$SCRIPT_DIR/src"
OUT="$SCRIPT_DIR/out"
mkdir -p "$OUT"
echo "Compiling Smart Drone Routing & Geofencing System..."
javac -d "$OUT" "$SRC"/*.java
echo "✅  Compilation successful! Run with: ./run.sh"
