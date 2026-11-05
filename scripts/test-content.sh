#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
content_javac="${JAVA_HOME:+$JAVA_HOME/bin/}javac"
content_java="${JAVA_HOME:+$JAVA_HOME/bin/}java"
out="$(mktemp -d "${TMPDIR:-/tmp}/karbala-content.XXXXXX")"
trap 'rm -rf "$out"' EXIT
"$content_javac" -d "$out" app/src/main/java/org/local/karbalascreen/ScreenPolicy.java app/src/main/java/org/local/karbalascreen/ContentPagination.java app/src/main/java/org/local/karbalascreen/ContentSequence.java app/src/main/java/org/local/karbalascreen/ContentCursor.java tests/content/ContentLayoutTest.java tests/content/ContentCursorTest.java
"$content_java" -cp "$out" org.local.karbalascreen.ContentLayoutTest
"$content_java" -cp "$out" org.local.karbalascreen.ContentCursorTest
python3 scripts/validate-content.py
# Real org.json implementation; Android's compile-time stubs cannot parse JSON.
content_json="${JSON_JAR:-/tmp/islamic-tv-json.jar}"
if [ ! -f "$content_json" ]; then
  content_json="$out/json-20240303.jar"
  curl -fsSL https://repo.maven.apache.org/maven2/org/json/json/20240303/json-20240303.jar -o "$content_json"
fi
python3 - "$content_json" <<'PY'
import hashlib,sys
assert hashlib.sha256(open(sys.argv[1],'rb').read()).hexdigest() == '3cf6cd6892e32e2b4c1c39e0f52f5248a2f5b37646fdfbb79a66b46b618414ed', 'Unexpected org.json test dependency'
PY
"$content_javac" -cp "$out:$content_json" -d "$out" tests/content/stubs/android/content/Context.java tests/content/stubs/android/content/SharedPreferences.java tests/content/stubs/android/content/res/AssetManager.java app/src/main/java/org/local/karbalascreen/StreamText.java app/src/main/java/org/local/karbalascreen/ContentAssetText.java app/src/main/java/org/local/karbalascreen/ContentStore.java app/src/main/java/org/local/karbalascreen/ContentProgress.java tests/content/ContentStoreTest.java
"$content_java" -cp "$out:$content_json" org.local.karbalascreen.ContentStoreTest
