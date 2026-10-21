#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DIST="${1:-$ROOT_DIR/dist/codecpack}"
ABIS=("arm64-v8a" "armeabi-v7a" "x86_64")
LIBS=("libavutil.so" "libswresample.so" "libswscale.so" "libavcodec.so" "libavformat.so" "libframeout_ffmpeg.so")
SIGNING_KEY="${CODECPACK_SIGNING_KEY_PEM:-}"
test -n "$SIGNING_KEY"
SERIAL="${CODECPACK_SERIAL:-$(date -u +%Y%m%d%H%M%S)}"
[[ "$SERIAL" =~ ^[0-9]{1,18}$ ]]
SIGNING_KEY_FILE="$ROOT_DIR/build/codecpack-signing-key.pem"
PUBLIC_KEY_FILE="$ROOT_DIR/build/codecpack-signing-public-key.pem"
trap 'rm -f "$SIGNING_KEY_FILE" "$PUBLIC_KEY_FILE"' EXIT
mkdir -p "$ROOT_DIR/build"
printf '%s\n' "$SIGNING_KEY" > "$SIGNING_KEY_FILE"
openssl pkey -in "$SIGNING_KEY_FILE" -pubout -out "$PUBLIC_KEY_FILE" >/dev/null
EMBEDDED_KEY_FILE="$ROOT_DIR/build/embedded-codecpack-public.pem"
python3 - "$ROOT_DIR/codecpack/src/main/java/com/frameout/codecpack/CodecPackVerifier.kt" "$EMBEDDED_KEY_FILE" <<'PY'
from pathlib import Path
import re,sys
text=Path(sys.argv[1]).read_text(encoding="utf-8")
key=re.search(r'private const val PUBLIC_KEY_PEM = """(.*?)"""',text,re.S)
if not key: raise SystemExit("Embedded codec pack public key is missing")
Path(sys.argv[2]).write_text(key.group(1).strip()+"\n",encoding="utf-8")
PY
if ! diff -u <(openssl pkey -pubin -in "$PUBLIC_KEY_FILE" -outform DER 2>/dev/null | openssl base64 -A) <(openssl pkey -pubin -in "$EMBEDDED_KEY_FILE" -outform DER 2>/dev/null | openssl base64 -A) >/dev/null; then
  echo "Embedded codec pack public key does not match CODECPACK_SIGNING_KEY_PEM" >&2
  exit 1
fi
RSA_BITS="$(openssl pkey -in "$SIGNING_KEY_FILE" -text -noout 2>/dev/null | sed -n 's/.*(\([0-9]*\) bit.*/\1/p' | head -n1)"
test "$RSA_BITS" = "3072"
EMBEDDED_BITS="$(openssl pkey -pubin -in "$EMBEDDED_KEY_FILE" -text -noout 2>/dev/null | sed -n 's/.*(\([0-9]*\) bit.*/\1/p' | head -n1)"
test "$EMBEDDED_BITS" = "3072"
rm -rf "$DIST"
mkdir -p "$DIST"
find "$ROOT_DIR/codecpack/build" -type f -name 'libframeout_ffmpeg.so' -print > "$ROOT_DIR/build/wrapper-libs.txt"
for abi in "${ABIS[@]}"; do
  out="$DIST/$abi"
  mkdir -p "$out"
  for lib in "${LIBS[@]}"; do
    src=""
    if [ "$lib" = "libframeout_ffmpeg.so" ]; then
      src="$(grep "/$abi/" "$ROOT_DIR/build/wrapper-libs.txt" | grep "stripped_native_libs" | grep -i "release" | LC_ALL=C sort | head -n1 || true)"
      if [ -z "$src" ]; then src="$(grep "/$abi/" "$ROOT_DIR/build/wrapper-libs.txt" | grep "stripped_native_libs" | LC_ALL=C sort | head -n1 || true)"; fi
      if [ -z "$src" ]; then echo "No stripped $lib for $abi" >&2; exit 1; fi
    else
      src="$ROOT_DIR/codecpack/src/main/jniLibs/$abi/$lib"
    fi
    test -n "$src" && test -f "$src"
    cp "$src" "$out/$lib"
  done
  (cd "$out" && sha256sum *.so > SHA256SUMS)
done
FFMPEG_VERSION="$(cat "$ROOT_DIR/build/ffmpeg/FFMPEG_VERSION.txt")"
DAV1D_VERSION="$(cat "$ROOT_DIR/build/ffmpeg/DAV1D_VERSION.txt")"
LICENSE_SRC="$ROOT_DIR/build/ffmpeg/src/ffmpeg/COPYING.LGPLv2.1"
DAV1D_LICENSE_SRC="$ROOT_DIR/build/ffmpeg/src/dav1d/COPYING"
test -f "$LICENSE_SRC"
test -f "$DAV1D_LICENSE_SRC"
bash "$ROOT_DIR/scripts/check-ffmpeg-license.sh" --binaries "$DIST"
cp "$LICENSE_SRC" "$DIST/COPYING.LGPLv2.1"
cp "$DAV1D_LICENSE_SRC" "$DIST/COPYING.dav1d"
printf 'Frame out codec pack\nFFmpeg %s\nlibdav1d %s (static, BSD-2-Clause)\nSerial: %s\nABIs: %s\nLicense: LGPLv2.1 or later\n' "$FFMPEG_VERSION" "$DAV1D_VERSION" "$SERIAL" "${ABIS[*]}" > "$DIST/MANIFEST.txt"
{
  cat "$DIST/MANIFEST.txt"
  for abi in "${ABIS[@]}"; do cat "$DIST/$abi/SHA256SUMS"; done
  (cd "$DIST" && sha256sum COPYING.LGPLv2.1 COPYING.dav1d)
} > "$DIST/SIGNING_PAYLOAD.txt"
openssl dgst -sha256 -sign "$SIGNING_KEY_FILE" -out "$DIST/SIGNATURE.bin" "$DIST/SIGNING_PAYLOAD.txt"
PACK_ZIP="$DIST/../frameout-codecpack.zip"
rm -f "$PACK_ZIP"
(cd "$DIST" && zip -qrD "$PACK_ZIP" .)
ENTRY_COUNT="$(unzip -Z1 "$PACK_ZIP" | wc -l)"
test "$ENTRY_COUNT" -le 64
test "$(stat -c %s "$PACK_ZIP")" -le $((48 * 1024 * 1024))
TOTAL_BYTES=0
while IFS= read -r file; do
  size="$(stat -c %s "$file")"
  test "$size" -le $((10 * 1024 * 1024))
  TOTAL_BYTES=$((TOTAL_BYTES + size))
done < <(find "$DIST" -type f)
test "$TOTAL_BYTES" -le $((64 * 1024 * 1024))
bash "$ROOT_DIR/scripts/check-ffmpeg-license.sh" --pack "$PACK_ZIP"
echo "Created $DIST/../frameout-codecpack.zip"
