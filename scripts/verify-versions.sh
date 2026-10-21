#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FAIL=0
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

fetch_url() {
  local url="$1" out="$2"
  curl -fsSL --location --retry 2 --retry-delay 1 --connect-timeout 10 --max-time 30 "$url" -o "$out"
}

maven_latest() {
  local group="$1" artifact="$2"
  local base="${group//.//}" url
  for url in "https://dl.google.com/dl/android/maven2/${base}/${artifact}/maven-metadata.xml" "https://repo1.maven.org/maven2/${base}/${artifact}/maven-metadata.xml"; do
    if fetch_url "$url" "$TMP/meta.xml"; then
      python3 - "$TMP/meta.xml" <<'PY'
import re
import sys
import xml.etree.ElementTree as ET
root = ET.parse(sys.argv[1]).getroot()
bad = re.compile(r'(alpha|beta|rc|snapshot|dev|canary|preview|eap|m\d+)', re.I)
versions = [v.text.strip() for v in root.findall('.//version') if v.text and not bad.search(v.text)]
def key(v):
    out=[]
    for part in re.split(r'[.-]', v):
        out.append((0,int(part)) if part.isdigit() else (1,part))
    return out
print(max(versions, key=key) if versions else '')
PY
      return 0
    fi
  done
  return 1
}

check_maven() {
  local label="$1" group="$2" artifact="$3" current="$4" latest
  if ! latest=$(maven_latest "$group" "$artifact"); then
    echo "FAIL $label: unable to verify $group:$artifact"
    FAIL=1
    return
  fi
  if [ -z "$latest" ]; then
    echo "FAIL $label: source returned no stable version"
    FAIL=1
    return
  fi
  if [ "$latest" != "$current" ]; then echo "STALE $label: current=$current latest=$latest"; FAIL=1; else echo "OK $label: $current"; fi
}

check_maven_pinned() {
  local label="$1" group="$2" artifact="$3" current="$4" latest
  if ! latest=$(maven_latest "$group" "$artifact"); then
    echo "FAIL $label: unable to verify $group:$artifact"
    FAIL=1
    return
  fi
  if [ -z "$latest" ]; then
    echo "FAIL $label: source returned no stable version"
    FAIL=1
    return
  fi
  local base="${group//.//}"
  if ! fetch_url "https://dl.google.com/dl/android/maven2/$base/$artifact/$current/$artifact-$current.pom" "$TMP/pinned.pom" && ! fetch_url "https://repo1.maven.org/maven2/$base/$artifact/$current/$artifact-$current.pom" "$TMP/pinned.pom"; then
    echo "FAIL $label: pinned version $current is not available"
    FAIL=1
    return
  fi
  if [[ "$current" =~ (alpha|beta|rc|snapshot|dev|canary|preview|eap|m[0-9]+) ]]; then
    echo "FAIL $label: pinned version is not stable: $current"
    FAIL=1
    return
  fi
  if [ "$latest" = "$current" ]; then echo "OK $label: $current"; else echo "PINNED $label: current=$current latest=$latest (compatibility pin)"; fi
}

check_url_version() {
  local label="$1" url="$2" current="$3" latest
  if ! fetch_url "$url" "$TMP/url.json"; then
    echo "FAIL $label: unable to verify"
    FAIL=1
    return
  fi
  if ! latest=$(python3 - "$TMP/url.json" <<'PY'
import json
import sys
with open(sys.argv[1], encoding='utf-8') as f:
    value = json.load(f).get('version', '')
print(value)
PY
); then
    echo "FAIL $label: invalid verification response"
    FAIL=1
    return
  fi
  if [ -z "$latest" ]; then
    echo "FAIL $label: missing version field"
    FAIL=1
    return
  fi
  if [ "$latest" != "$current" ]; then echo "STALE $label: current=$current latest=$latest"; FAIL=1; else echo "OK $label: $current"; fi
}

echo "=== Frame out version verification ==="
if ! fetch_url "https://services.gradle.org/versions/current" "$TMP/gradle.json"; then echo "FAIL Gradle: unable to verify"; FAIL=1; else python3 - "$TMP/gradle.json" <<'PY'
import json,sys
v=json.load(open(sys.argv[1],encoding="utf-8")).get("version","")
print(f"LATEST Gradle: {v}")
PY
fi
check_maven_pinned AGP com.android.tools.build gradle 9.4.0
check_maven_pinned Kotlin org.jetbrains.kotlin kotlin-gradle-plugin 2.4.20
if [ "$FAIL" -eq 0 ]; then
  python3 - <<'PY'
from pathlib import Path
import re
text = Path("gradle/libs.versions.toml").read_text()
versions = dict(re.findall(r'^(agp|kotlin)\s*=\s*"([^"]+)"', text, re.M))
expected = {"agp": "9.4.0", "kotlin": "2.4.20"}
if versions != expected:
    raise SystemExit(f"Unsupported Kotlin/AGP pair: {versions}; expected compatible pair {expected}")
PY
  if ! grep -R -q 'gradle-version: "9.7.1"' .github/workflows/*.yml; then
    echo "FAIL Gradle CI pin: expected 9.7.1"
    FAIL=1
  fi
fi
check_maven KSP com.google.devtools.ksp symbol-processing-gradle-plugin 2.3.12
check_maven Compose-BOM androidx.compose compose-bom 2026.09.00
check_maven Core-KTX androidx.core core-ktx 1.19.1
check_maven Coroutines org.jetbrains.kotlinx kotlinx-coroutines-android 1.11.0
check_maven Lifecycle androidx.lifecycle lifecycle-runtime-ktx 2.11.0
check_maven Activity androidx.activity activity-compose 1.13.0
check_maven Paging androidx.paging paging-runtime 3.5.1
check_maven Room3 androidx.room3 room3-runtime 3.0.3
check_maven WorkManager androidx.work work-runtime-ktx 2.12.0
check_maven Media3 androidx.media3 media3-exoplayer 1.11.1
check_maven ProfileInstaller androidx.profileinstaller profileinstaller 1.4.1
check_maven Test-JUnit androidx.test.ext junit 1.3.0
check_maven Test-Runner androidx.test runner 1.7.0

bash "$ROOT_DIR/scripts/verify-native-versions.sh"

bash "$ROOT_DIR/scripts/verify-actions.sh"
status=$?
if [ "$status" -ne 0 ]; then FAIL=1; fi
if [ "$FAIL" -ne 0 ]; then echo "=== Version verification FAILED ==="; exit 1; fi
echo "=== Version verification passed ==="
