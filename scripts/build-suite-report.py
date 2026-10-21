import html
import json
import os
import sys
from pathlib import Path

root = Path(sys.argv[1])
out = Path(sys.argv[2])
expected_arg = sys.argv[3] if len(sys.argv) > 3 else os.environ.get("EXPECTED_DEVICES", "")
expected = [item for item in expected_arg.split(",") if item]
devices = {}
for directory in sorted(p for p in root.glob("*") if p.is_dir()):
    results = []
    file = directory / "results.jsonl"
    if file.is_file():
        for line in file.read_text(encoding="utf-8").splitlines():
            if line.strip():
                results.append(json.loads(line))
    capabilities = {}
    cap = directory / "capabilities.json"
    if cap.is_file():
        capabilities = json.loads(cap.read_text(encoding="utf-8"))
    devices[directory.name] = {"results": results, "capabilities": capabilities}

if not devices:
    print("FAIL: no device result directories found", file=sys.stderr)
    raise SystemExit(2)
if expected:
    missing = [name for name in expected if name not in devices]
    if missing:
        missing_text = ", ".join(missing)
        print(f"FAIL: missing expected device results: {missing_text}", file=sys.stderr)
        raise SystemExit(2)
empty = [name for name, data in devices.items() if not data["results"]]
if empty:
    empty_text = ", ".join(empty)
    print(f"FAIL: device result files are empty or missing: {empty_text}", file=sys.stderr)
    raise SystemExit(2)

tests = sorted({f"{r['class']}.{r['test']}" for d in devices.values() for r in d["results"]})
matrix = {}
for name, data in devices.items():
    for r in data["results"]:
        matrix.setdefault(f"{r['class']}.{r['test']}", {})[name] = r
missing_matrix = []
for test in tests:
    for name in expected:
        if name not in matrix.get(test, {}):
            missing_matrix.append(f"{test} on {name}")
if missing_matrix:
    print(f"FAIL: missing test results: {', '.join(missing_matrix[:20])}", file=sys.stderr)
    raise SystemExit(2)

counts = {name: {"passed": 0, "failed": 0, "skipped": 0} for name in devices}
for test in tests:
    for name in devices:
        entry = matrix.get(test, {}).get(name)
        if entry:
            counts[name][entry["status"]] += 1

summary = {"devices": {n: {"counts": counts[n], "capabilities": d["capabilities"]} for n, d in devices.items()},
           "tests": {t: {n: {"status": e["status"], "reason": e["reason"]} for n, e in matrix.get(t, {}).items()} for t in tests}}
out.mkdir(parents=True, exist_ok=True)
(out / "report.json").write_text(json.dumps(summary, indent=2), encoding="utf-8")

colors = {"passed": "#1b7f3b", "failed": "#b3261e", "skipped": "#8a6d00", "none": "#777"}
head = "".join(f"<th>{html.escape(n)}</th>" for n in devices)
rows = []
for test in tests:
    cells = []
    for name in devices:
        entry = matrix.get(test, {}).get(name)
        status = entry["status"] if entry else "none"
        reason = html.escape(entry["reason"]) if entry else ""
        cells.append(f'<td style="color:{colors[status]}" title="{reason}">{status}<br><small>{reason[:90]}</small></td>')
    rows.append(f"<tr><td>{html.escape(test)}</td>{''.join(cells)}</tr>")
totals = "".join(f"<td>{c['passed']} / {c['failed']} / {c['skipped']}</td>" for c in counts.values())
page = f"<!doctype html><meta charset=utf-8><title>Frame out suite</title><style>body{{font:14px sans-serif}}table{{border-collapse:collapse}}td,th{{border:1px solid #ccc;padding:4px 8px;vertical-align:top}}</style><table><tr><th>test</th>{head}</tr><tr><td>passed / failed / skipped</td>{totals}</tr>{''.join(rows)}</table>"
(out / "report.html").write_text(page, encoding="utf-8")

failed = sum(c["failed"] for c in counts.values())
lines = ["| device | passed | failed | skipped |", "|---|---|---|---|"]
lines += [f"| {n} | {c['passed']} | {c['failed']} | {c['skipped']} |" for n, c in counts.items()]
summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
if summary_path:
    with open(summary_path, "a", encoding="utf-8") as handle:
        handle.write("\n".join(lines) + "\n")
if failed:
    print(f"::error::{failed} test failures in the full suite", file=sys.stderr)
    raise SystemExit(1)
print("\n".join(lines))
