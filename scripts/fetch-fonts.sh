#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DIR="$ROOT/designsystem/src/main/res/font"
TMP="${TMPDIR:-/tmp}/frameout-fonts"
VENV="$TMP/venv"
FONT_LOCK="$ROOT/scripts/fonts.lock"
TOOLS_LOCK="$ROOT/scripts/fonttools-requirements.txt"
for lock in "$FONT_LOCK" "$TOOLS_LOCK"; do
  if [ ! -s "$lock" ]; then
    echo "FAIL: missing ${lock#$ROOT/}; run the 'Pin fonts' workflow or scripts/pin-fonts.sh <tag> <fonttools-version> and commit the result" >&2
    exit 1
  fi
done
COMMIT="$(sed -n 's/^commit=//p' "$FONT_LOCK" | head -n1)"
[[ "$COMMIT" =~ ^[0-9a-f]{40}$ ]]
BASE="https://raw.githubusercontent.com/IBM/plex/${COMMIT}/packages/plex-sans-arabic/fonts/complete/ttf"
mkdir -p "$DIR" "$TMP"
if [ ! -x "$VENV/bin/pyftsubset" ]; then
  python3 -m venv "$VENV"
  "$VENV/bin/pip" install -q --require-hashes --no-deps -r "$TOOLS_LOCK"
fi
for spec in "Regular:400:regular" "Medium:500:medium" "SemiBold:600:semibold"; do
  weight="${spec%%:*}"
  rest="${spec#*:}"
  name="${rest#*:}"
  src="$TMP/IBMPlexSansArabic-${weight}.ttf"
  out="$DIR/ibm_plex_sans_arabic_${name}.ttf"
  expected="$(sed -n "s/^${weight}=//p" "$FONT_LOCK" | head -n1)"
  [[ "$expected" =~ ^[0-9a-f]{64}$ ]]
  curl -fsSL "$BASE/IBMPlexSansArabic-${weight}.ttf" -o "$src"
  actual="$(sha256sum "$src" | cut -d' ' -f1)"
  test "$actual" = "$expected"
  "$VENV/bin/pyftsubset" "$src" --output-file="$out" --unicodes='U+0000-024F,U+0600-06FF,U+0750-077F,U+08A0-08FF,U+FB50-FDFF,U+FE70-FEFF' --layout-features='*' --name-IDs='*' --glyph-names --symbol-cmap --legacy-cmap --notdef-glyph --notdef-outline
done
echo "Frame out fonts prepared in $DIR"
