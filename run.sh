#!/bin/bash
# Run the app with correct classpath (driver in lib/).
set -e
cd "$(dirname "$0")"
if [[ ! -f lib/postgresql-42.7.10.jar ]]; then
  echo "Error: lib/postgresql-42.7.10.jar not found. Add the JDBC driver there."
  exit 1
fi
if [[ ! -f out/Main.class ]]; then
  echo "Error: out/Main.class not found. Run ./compile.sh first."
  exit 1
fi
java -cp "out:lib/postgresql-42.7.10.jar" Main
