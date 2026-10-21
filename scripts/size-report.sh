#!/usr/bin/env bash
set -euo pipefail
FOUND=0
echo "=== APK Size Report (no size limit) ==="
while IFS= read -r apk; do
  FOUND=1
  size=$(stat -c%s "$apk" 2>/dev/null || stat -f%z "$apk")
  human=$(numfmt --to=iec-i --suffix=B "$size" 2>/dev/null || echo "${size}B")
  echo "[SIZE] $human  $apk"
  listing=$(unzip -lv "$apk" | sort -k3 -n -r)
  printf '%s\n' "$listing" | sed -n '1,12p'
done < <(find . -type f -name "*.apk" -path "*/build/outputs/apk/release/*.apk" 2>/dev/null | sort)
if [ "$FOUND" -eq 0 ]; then
  echo "FAIL: no release APKs found"
  exit 1
fi
EXPECTED_ABIS=("arm64-v8a" "armeabi-v7a" "x86_64")
for abi in "${EXPECTED_ABIS[@]}"; do
  found=0
  while IFS= read -r apk; do
    if unzip -Z1 "$apk" 2>/dev/null | grep -q "^lib/$abi/.*\.so$"; then
      echo "[ABI OK] $abi  $apk"
      found=1
      break
    fi
  done < <(find . -type f -name "*.apk" -path "*/build/outputs/apk/release/*.apk" -print | sort)
  if [ "$found" -ne 1 ]; then
    echo "FAIL: no release APK contains native libraries for ABI $abi"
    exit 1
  fi
done
echo "=== Size report done ==="
