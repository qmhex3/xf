#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CHECK="$ROOT_DIR/scripts/check-ffmpeg-components.sh"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

bash "$CHECK" --list | sed 's/^/#define /; s/$/ 1/' > "$WORK/config_components.h"
bash "$CHECK" "$WORK" >/dev/null

sed -i 's/^#define CONFIG_RM_DEMUXER 1/#define CONFIG_RM_DEMUXER 0/' "$WORK/config_components.h"
if OUTPUT="$(bash "$CHECK" "$WORK" 2>&1)"; then echo "FAIL: missing rm demuxer was not detected"; exit 1; fi
echo "$OUTPUT" | grep -q "MISSING demuxer rm (needed by rmvb)" || { echo "FAIL: wrong message: $OUTPUT"; exit 1; }

sed -i 's/^#define CONFIG_RM_DEMUXER 0/#define CONFIG_RM_DEMUXER 1/; /CONFIG_AC3_PARSER/d' "$WORK/config_components.h"
if OUTPUT="$(bash "$CHECK" "$WORK" 2>&1)"; then echo "FAIL: missing ac3 parser was not detected"; exit 1; fi
echo "$OUTPUT" | grep -q "MISSING parser ac3" || { echo "FAIL: wrong message: $OUTPUT"; exit 1; }

rm "$WORK/config_components.h"
if bash "$CHECK" "$WORK" 2>/dev/null; then echo "FAIL: absent header was accepted"; exit 1; fi
echo "OK: check-ffmpeg-components self-test"
