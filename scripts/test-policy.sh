#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
policy_javac="${JAVA_HOME:+$JAVA_HOME/bin/}javac"
policy_java="${JAVA_HOME:+$JAVA_HOME/bin/}java"
out="$(mktemp -d "${TMPDIR:-/tmp}/karbala-policy.XXXXXX")"
trap 'rm -rf "$out"' EXIT
"$policy_javac" -d "$out" app/src/main/java/org/local/karbalascreen/ScreenPolicy.java app/src/test/java/org/local/karbalascreen/ScreenPolicyTest.java
"$policy_java" -cp "$out" org.local.karbalascreen.ScreenPolicyTest
