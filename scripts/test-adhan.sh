#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
adhan_javac="${JAVA_HOME:+$JAVA_HOME/bin/}javac"
adhan_java="${JAVA_HOME:+$JAVA_HOME/bin/}java"
out="$(mktemp -d "${TMPDIR:-/tmp}/karbala-adhan.XXXXXX")"
trap 'rm -rf "$out"' EXIT
"$adhan_javac" -d "$out" app/src/main/java/org/local/karbalascreen/AdhanPolicy.java app/src/main/java/org/local/karbalascreen/LocationProfile.java tests/adhan/AdhanPolicyTest.java
"$adhan_java" -cp "$out" org.local.karbalascreen.AdhanPolicyTest
