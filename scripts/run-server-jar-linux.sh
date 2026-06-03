#!/usr/bin/env bash
cd "$(dirname "$0")/.." || exit 1
java -jar target/spaceinvaders-server-1.0-SNAPSHOT.jar
