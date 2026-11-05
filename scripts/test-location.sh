#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
"${JAVA_HOME:+$JAVA_HOME/bin/}javac" -d "$out" app/src/main/java/org/local/karbalascreen/LocationProfile.java app/src/main/java/org/local/karbalascreen/AdhanPolicy.java app/src/main/java/org/local/karbalascreen/CalculatedPrayerValidation.java tests/location/LocationProfileTest.java
"${JAVA_HOME:+$JAVA_HOME/bin/}java" -cp "$out" org.local.karbalascreen.LocationProfileTest
