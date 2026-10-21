#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="$ROOT_DIR/build/ffmpeg"
ASSETS="$ROOT_DIR/codecpack/src/main/assets"
JNI_LIBS="$ROOT_DIR/codecpack/src/main/jniLibs"
bash "$ROOT_DIR/scripts/build-ffmpeg.sh" "$OUT"
bash "$ROOT_DIR/scripts/check-ffmpeg-license.sh" --binaries "$JNI_LIBS"
mkdir -p "$ASSETS"
cp "$OUT/src/ffmpeg/COPYING.LGPLv2.1" "$ASSETS/COPYING.LGPLv2.1"
cp "$OUT/src/dav1d/COPYING" "$ASSETS/COPYING.dav1d"
printf 'FFmpeg %s\nlibdav1d %s (static, BSD-2-Clause)\n' "$(cat "$OUT/FFMPEG_VERSION.txt")" "$(cat "$OUT/DAV1D_VERSION.txt")" > "$ASSETS/BUNDLED_VERSIONS.txt"
echo "FFmpeg bundled into codecpack (jniLibs + assets). Build the app normally now."
