#!/usr/bin/env bash
set -euo pipefail
python3 - <<'PY2'
from pathlib import Path
import re

root = Path('.')
failed = False
for path in root.rglob('*.kt'):
    if any(part in {'build', '.gradle', '.git'} for part in path.parts):
        continue
    lines = path.read_text(encoding='utf-8').splitlines()
    depth = 0
    active = []
    for i, raw in enumerate(lines, 1):
        clean = re.sub(r'"(?:\\.|[^"\\])*"', '""', raw)
        if re.search(r'\bfun\s+[A-Za-z_][A-Za-z0-9_]*\s*\(', clean) and '{' in clean:
            start_depth = depth + clean.count('{')
            active.append((i, start_depth))
        depth += clean.count('{') - clean.count('}')
        remaining = []
        for start, target in active:
            if depth < target:
                size = i - start + 1
                if size > 40:
                    print(f'FAIL: {path}:{start} function spans {size} lines')
                    failed = True
            else:
                remaining.append((start, target))
        active = remaining
if failed:
    raise SystemExit(1)
print('OK: functions within 40 lines')
PY2
