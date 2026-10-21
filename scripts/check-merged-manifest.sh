#!/usr/bin/env bash
set -euo pipefail
FORBIDDEN=(
  "android.permission.ACCESS_WIFI_STATE"
  "android.permission.CHANGE_NETWORK_STATE"
  "android.permission.CHANGE_WIFI_STATE"
  "android.permission.CHANGE_WIFI_MULTICAST_STATE"
)
ROOT="app/build/intermediates"
MANIFESTS=()
while IFS= read -r file; do
  MANIFESTS+=("$file")
done < <(find "$ROOT/merged_manifest" "$ROOT/merged_manifests" -type f -name "AndroidManifest.xml" 2>/dev/null | sort)
if [ "${#MANIFESTS[@]}" -eq 0 ]; then
  echo "FAIL: no merged manifest found under $ROOT/merged_manifest or $ROOT/merged_manifests"
  find "$ROOT" -name "AndroidManifest.xml" 2>/dev/null | head -20
  exit 1
fi
FAILED=0
for manifest in "${MANIFESTS[@]}"; do
  for permission in "${FORBIDDEN[@]}"; do
    if grep -Fq "\"$permission\"" "$manifest"; then
      echo "FAIL: $permission present in merged manifest $manifest"
      FAILED=1
    fi
  done
done
if [ "$FAILED" -ne 0 ]; then
  exit 1
fi
echo "OK: merged manifests declare no network permissions beyond INTERNET (${#MANIFESTS[@]} checked)"
