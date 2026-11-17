#!/bin/bash
# Usage: ./run.sh          interactive menu
#        ./run.sh --demo   scripted end-to-end demo
cd "$(dirname "$0")"
java -cp out:../lib/mariadb-java-client.jar com.vcet.transport.ui.Main "$@"
