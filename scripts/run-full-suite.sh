#!/usr/bin/env bash
set -uo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LABEL="${1:?label}"
FILTER="${2:-all}"
OUT="$ROOT_DIR/build/suite-results/$LABEL"
APP_ID="com.frameout.app"
RUNNER="$APP_ID.test/androidx.test.runner.AndroidJUnitRunner"
mkdir -p "$OUT"
cd "$ROOT_DIR"
gradle :app:installDebug :app:installDebugAndroidTest --no-daemon || exit 1
if [ -s "$ROOT_DIR/build/fixtures/manifest.txt" ]; then
  adb shell rm -rf /data/local/tmp/frameout-fixtures
  adb push "$ROOT_DIR/build/fixtures" /data/local/tmp/frameout-fixtures >/dev/null
  adb shell "run-as $APP_ID sh -c 'rm -rf files/fixtures && mkdir -p files/fixtures && cp /data/local/tmp/frameout-fixtures/* files/fixtures/'"
fi
ARGS=(-w -r -e listener com.frameout.app.full.ReportListener -e longFrames 2000 -e maxJankPercent 60)
if [ "$FILTER" = "basic" ]; then ARGS+=(-e notAnnotation com.frameout.app.full.FullSuite); fi
adb logcat -c
adb shell am instrument "${ARGS[@]}" "$RUNNER" | tee "$OUT/instrument.txt"
adb logcat -d > "$OUT/logcat.txt"
adb exec-out run-as "$APP_ID" cat files/fullsuite/results.jsonl > "$OUT/results.jsonl" 2>/dev/null || true
adb exec-out run-as "$APP_ID" cat files/fullsuite/capabilities.json > "$OUT/capabilities.json" 2>/dev/null || true
test -s "$OUT/results.jsonl" || exit 1
if grep -qE "^INSTRUMENTATION_STATUS_CODE: (-1|-2)$|INSTRUMENTATION_RESULT: shortMsg|Process crashed" "$OUT/instrument.txt"; then exit 1; fi
exit 0
