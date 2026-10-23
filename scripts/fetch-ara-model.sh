#!/usr/bin/env bash
set -euo pipefail
DEST="core/src/main/assets/tessdata/ara.traineddata"
URL="https://github.com/tesseract-ocr/tessdata_fast/raw/main/ara.traineddata"
mkdir -p "$(dirname "$DEST")"
if [ -s "$DEST" ]; then
  echo "OK: $DEST already present ($(stat -c%s "$DEST") bytes)"
  exit 0
fi
curl -fsSL --retry 3 "$URL" -o "$DEST"
size=$(stat -c%s "$DEST")
if [ "$size" -lt 500000 ] || [ "$size" -gt 4000000 ]; then
  echo "FAIL: unexpected ara.traineddata size: $size bytes"
  rm -f "$DEST"
  exit 1
fi
sha256sum "$DEST"
echo "OK: fetched tessdata_fast ara ($size bytes)"
