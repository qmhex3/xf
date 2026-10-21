#!/usr/bin/env bash
set -euo pipefail
: "${ANDROID_HOME:=${ANDROID_SDK_ROOT:-}}"
BT_DIR="$(dirname "$(find "$ANDROID_HOME/build-tools" -name zipalign -type f | sort -V | tail -1)")"
ZIPALIGN="$BT_DIR/zipalign"
APKSIGNER="$BT_DIR/apksigner"
KEYSTORE="${KEYSTORE_FILE:-app/release.keystore}"
APK_DIR="${APK_DIR:-app/build/outputs/apk/release}"
test -x "$ZIPALIGN"
test -x "$APKSIGNER"
test -f "$KEYSTORE"
: "${KEYSTORE_PASSWORD:?}" "${KEY_PASSWORD:?}" "${KEY_ALIAS:?}"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
TOTAL_BEFORE=0
TOTAL_AFTER=0
found=0
for apk in "$APK_DIR"/*.apk; do
  [ -f "$apk" ] || continue
  found=1
  name="$(basename "$apk")"
  before="$(stat -c%s "$apk")"
  cp "$apk" "$WORK/in.apk"
  zip -q -d "$WORK/in.apk" 'META-INF/*.SF' 'META-INF/*.RSA' 'META-INF/*.DSA' 'META-INF/*.EC' 'META-INF/MANIFEST.MF' >/dev/null 2>&1 || true
  rm -f "$WORK/aligned.apk"
  "$ZIPALIGN" -f -z -P 16 -v 4 "$WORK/in.apk" "$WORK/aligned.apk" >/dev/null
  "$APKSIGNER" sign \
    --ks "$KEYSTORE" \
    --ks-key-alias "$KEY_ALIAS" \
    --ks-pass env:KEYSTORE_PASSWORD \
    --key-pass env:KEY_PASSWORD \
    --min-sdk-version 24 \
    --out "$WORK/signed.apk" \
    "$WORK/aligned.apk"
  "$APKSIGNER" verify --min-sdk-version 24 "$WORK/signed.apk"
  "$ZIPALIGN" -c -P 16 -v 4 "$WORK/signed.apk" >/dev/null
  unzip -v "$WORK/signed.apk" resources.arsc | grep -q Stored
  unzip -v "$WORK/signed.apk" classes.dex >/dev/null
  if [ -n "${CERT_SHA256:-}" ]; then
    actual="$("$APKSIGNER" verify --print-certs "$WORK/signed.apk" | sed -n 's/.*certificate SHA-256 digest: //p' | head -n1 | tr 'A-F' 'a-f')"
    test "$actual" = "$(printf '%s' "$CERT_SHA256" | tr 'A-F' 'a-f' | tr -cd '0-9a-f')"
  fi
  after="$(stat -c%s "$WORK/signed.apk")"
  if [ "$after" -lt "$before" ]; then
    cp "$WORK/signed.apk" "$apk"
    rm -f "$apk.idsig"
  else
    after="$before"
  fi
  TOTAL_BEFORE=$((TOTAL_BEFORE + before))
  TOTAL_AFTER=$((TOTAL_AFTER + after))
  echo "[ZOPFLI] $name: $before -> $after bytes"
done
if [ "$found" -ne 1 ]; then
  echo "FAIL: no APKs found in $APK_DIR"
  exit 1
fi
echo "=== Zopfli recompress done: $TOTAL_BEFORE -> $TOTAL_AFTER bytes ==="
