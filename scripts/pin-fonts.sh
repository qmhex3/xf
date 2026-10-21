#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TAG="${1:-v5.2.0}"
FONTTOOLS_VERSION="${2:?fonttools version required}"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
COMMIT="$(git ls-remote https://github.com/IBM/plex "refs/tags/${TAG}^{}" | cut -f1)"
if [ -z "$COMMIT" ]; then COMMIT="$(git ls-remote https://github.com/IBM/plex "refs/tags/${TAG}" | cut -f1)"; fi
[[ "$COMMIT" =~ ^[0-9a-f]{40}$ ]]
BASE="https://raw.githubusercontent.com/IBM/plex/${COMMIT}/packages/plex-sans-arabic/fonts/complete/ttf"
{
  echo "commit=$COMMIT"
  for weight in Regular Medium SemiBold; do
    curl -fsSL "$BASE/IBMPlexSansArabic-${weight}.ttf" -o "$TMP/${weight}.ttf"
    echo "${weight}=$(sha256sum "$TMP/${weight}.ttf" | cut -d' ' -f1)"
  done
} > "$ROOT/scripts/fonts.lock"
python3 -m venv "$TMP/venv"
"$TMP/venv/bin/pip" download -q --no-deps --dest "$TMP/wheels" "fonttools==${FONTTOOLS_VERSION}"
WHEEL="$(ls "$TMP"/wheels/*)"
echo "fonttools==${FONTTOOLS_VERSION} --hash=sha256:$(sha256sum "$WHEEL" | cut -d' ' -f1)" > "$ROOT/scripts/fonttools-requirements.txt"
