#!/bin/bash
# Compile all sources into ./out
cd "$(dirname "$0")"
mkdir -p out
javac -d out -cp ../lib/mariadb-java-client.jar $(find src -name '*.java') && echo "Compiled OK"
