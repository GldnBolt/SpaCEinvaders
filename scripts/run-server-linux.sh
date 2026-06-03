#!/usr/bin/env bash
cd "$(dirname "$0")/.." || exit 1
mvn exec:java
