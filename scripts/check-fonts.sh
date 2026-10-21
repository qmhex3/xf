#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DIR="$ROOT/designsystem/src/main/res/font"
FAILED=0
for name in regular medium semibold; do
  file="$DIR/ibm_plex_sans_arabic_${name}.ttf"
  if [ ! -s "$file" ]; then
    echo "FAIL: missing ${file#$ROOT/}" >&2
    FAILED=1
  fi
done
if [ "$FAILED" -ne 0 ]; then
  echo "Add the three font files to designsystem/src/main/res/font/ and commit them" >&2
  exit 1
fi
echo "OK: fonts present"
