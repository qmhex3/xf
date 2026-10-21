#!/usr/bin/env bash
set -euo pipefail
AAPT2="$(find "${ANDROID_HOME:-${ANDROID_SDK_ROOT:-/usr/local/lib/android/sdk}}/build-tools" -type f -name aapt2 2>/dev/null | sort -V | tail -n 1)"
if [ -z "$AAPT2" ]; then
  echo "FAIL: aapt2 not found"
  exit 1
fi
FOUND=0
FAILED=0
while IFS= read -r apk; do
  FOUND=1
  perms="$("$AAPT2" dump permissions "$apk")"
  if echo "$perms" | grep -Eq "android.permission.(ACCESS_WIFI_STATE|CHANGE_NETWORK_STATE|CHANGE_WIFI_STATE|CHANGE_WIFI_MULTICAST_STATE)"; then
    echo "FAIL: network permission in $apk"
    echo "$perms" | grep -E "android.permission.(ACCESS_WIFI_STATE|CHANGE_NETWORK_STATE|CHANGE_WIFI_STATE|CHANGE_WIFI_MULTICAST_STATE)"
    FAILED=1
  fi
done < <(find app/build/outputs/apk -type f -name "*.apk" | sort)
if [ "$FOUND" -eq 0 ]; then
  echo "FAIL: no APKs found"
  exit 1
fi
if [ "$FAILED" -ne 0 ]; then
  exit 1
fi
echo "OK: APKs declare no network permissions beyond INTERNET"
