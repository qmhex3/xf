#!/usr/bin/env bash
set -euo pipefail
FAILED=0
LIST="$(mktemp)"
trap 'rm -f "$LIST"' EXIT
find . -type f \( -name "*.kt" -o -name "*.kts" -o -name "*.yml" -o -name "*.yaml" -o -name "*.xml" \) \
  ! -path "./.git/*" ! -path "./build/*" ! -path "*/build/*" ! -path "./.gradle/*" > "$LIST"
while IFS= read -r file; do
  if grep -nE '^\s*//|^\s*/\*|^\s*<!--' "$file" >/dev/null 2>&1; then
    echo "FAIL: comments found in $file"
    grep -nE '^\s*//|^\s*/\*|^\s*<!--' "$file" || true
    FAILED=1
  fi
done < "$LIST"
if [ "$FAILED" -ne 0 ]; then
  exit 1
fi
echo "OK: no comments in source files"
