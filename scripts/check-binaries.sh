#!/usr/bin/env bash
set -euo pipefail
FAILED=0
LIST="$(mktemp)"
trap 'rm -f "$LIST"' EXIT
if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  git ls-files -z > "$LIST"
else
  find . -type f ! -path './.git/*' ! -path './build/*' ! -path '*/build/*' ! -path './.gradle/*' -print0 > "$LIST"
fi
HAS_FILE=0
if command -v file >/dev/null 2>&1; then HAS_FILE=1; fi
while IFS= read -r -d '' file; do
  if [ "$HAS_FILE" -eq 1 ]; then
    type=$(file -b "$file" 2>/dev/null || true)
    case "$type" in
      *ELF*|*Mach-O*|*PE32*|*shared*object*|*archive*)
        echo "FAIL: binary tracked in repository: $file"
        FAILED=1
        ;;
    esac
  else
    case "$file" in
      *.so|*.a|*.o|*.dll|*.dylib|*.exe|*.bin)
        echo "FAIL: binary tracked in repository: $file"
        FAILED=1
        ;;
    esac
  fi
done < "$LIST"
if [ "$FAILED" -ne 0 ]; then exit 1; fi
echo "OK: no executable or ELF binaries are tracked"
