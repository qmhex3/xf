#!/usr/bin/env bash
set -euo pipefail
MAX_FILE=300
# on  = يفشل البناء عند تجاوز الحد
# off = يطبع تحذيرات فقط ولا يفشل البناء
ENFORCE_LINE_LIMIT="${ENFORCE_LINE_LIMIT:-off}"
FAILED=0
LIST="$(mktemp)"
trap 'rm -f "$LIST"' EXIT
find . -type f \( -name "*.kt" -o -name "*.kts" -o -name "*.yml" -o -name "*.yaml" -o -name "*.toml" -o -name "*.sh" -o -name "*.properties" -o -name "*.cpp" -o -name "*.h" -o -name "*.hpp" \) \
  ! -path "./.git/*" ! -path "./build/*" ! -path "*/build/*" ! -path "./.gradle/*" > "$LIST"
while IFS= read -r file; do
  lines=$(wc -l < "$file")
  if [ "$lines" -gt "$MAX_FILE" ]; then
    if [ "$ENFORCE_LINE_LIMIT" = "on" ]; then
      echo "FAIL: $file has $lines lines (max $MAX_FILE)"
      FAILED=1
    else
      echo "WARN: $file has $lines lines (max $MAX_FILE)"
    fi
  fi
done < "$LIST"
if [ "$FAILED" -ne 0 ]; then
  exit 1
fi
if [ "$ENFORCE_LINE_LIMIT" = "on" ]; then
  echo "OK: all files within $MAX_FILE lines"
else
  echo "SKIPPED: line limit enforcement is off (set ENFORCE_LINE_LIMIT=on)"
fi
