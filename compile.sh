#!/bin/bash
# Compile all Java sources. Run from project root.
set -e
cd "$(dirname "$0")"
mkdir -p out
javac -d out -cp "lib/postgresql-42.7.10.jar" src/*.java
echo "Compiled successfully. Run with: ./run.sh"
