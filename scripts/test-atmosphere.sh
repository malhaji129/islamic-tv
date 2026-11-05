#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
atmosphere_javac="${JAVA_HOME:+$JAVA_HOME/bin/}javac"
atmosphere_java="${JAVA_HOME:+$JAVA_HOME/bin/}java"
out="$(mktemp -d "${TMPDIR:-/tmp}/karbala-atmosphere.XXXXXX")"
trap 'rm -rf "$out"' EXIT
"$atmosphere_javac" -d "$out" app/src/main/java/org/local/karbalascreen/ScreenPolicy.java app/src/main/java/org/local/karbalascreen/AtmospherePolicy.java tests/atmosphere/AtmospherePolicyTest.java
"$atmosphere_java" -cp "$out" org.local.karbalascreen.AtmospherePolicyTest
