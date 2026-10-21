#!/usr/bin/env bash
set -euo pipefail
LIBS=(libavcodec.so libavformat.so libavutil.so libswscale.so libswresample.so libframeout_ffmpeg.so)
ASSETS=(assets/COPYING.LGPLv2.1 assets/COPYING.dav1d assets/BUNDLED_VERSIONS.txt)
FOUND=0
FAILED=0
while IFS= read -r apk; do
  FOUND=1
  # here-strings avoid SIGPIPE from grep -q under pipefail
  listing="$(unzip -Z1 "$apk")"
  abi="$(sed -n 's#^lib/\([^/]*\)/libframeout_ffmpeg\.so$#\1#p' <<<"$listing" | head -n1)"
  if [ -z "$abi" ]; then
    echo "FAIL: $apk has no libframeout_ffmpeg.so"
    FAILED=1
    continue
  fi
  for lib in "${LIBS[@]}"; do
    grep -qxF "lib/$abi/$lib" <<<"$listing" || { echo "FAIL: $apk ($abi) is missing $lib"; FAILED=1; }
  done
  for asset in "${ASSETS[@]}"; do
    grep -qxF "$asset" <<<"$listing" || { echo "FAIL: $apk is missing $asset"; FAILED=1; }
  done
  [ "$FAILED" -eq 0 ] && echo "OK: $apk bundles FFmpeg ($abi)"
done < <(find app/build/outputs/apk -type f -name "*.apk" | sort)
if [ "$FOUND" -eq 0 ]; then echo "FAIL: no APKs found"; exit 1; fi
if [ "$FAILED" -ne 0 ]; then exit 1; fi
