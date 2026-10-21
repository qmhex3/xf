#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
python3 - "$ROOT_DIR" <<'PY'
import json
import os
import pathlib
import re
import sys
import urllib.error
import urllib.request

root = pathlib.Path(sys.argv[1])
failed = False
pattern = re.compile(r"uses:\s*([A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+)(?:/[^\s@]+)?@(?:[0-9a-f]{40}\s*#\s*)?(v[^\s#]+)")


def latest_release(owner_repo):
    url = f"https://api.github.com/repos/{owner_repo}/releases/latest"
    headers = {"User-Agent": "FrameOut-version-verifier", "Accept": "application/vnd.github+json"}
    token = os.environ.get("GITHUB_TOKEN", "")
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(req, timeout=20) as response:
        return json.load(response).get("tag_name", "")


def accepts_pin(current, latest):
    if re.fullmatch(r"v\d+", current):
        return latest == current or latest.startswith(current + ".")
    if re.fullmatch(r"v\d+\.\d+", current):
        return latest.startswith(current + ".")
    return current == latest

for path in sorted(root.joinpath(".github/workflows").glob("*.yml")):
    for owner_repo, tag in pattern.findall(path.read_text(encoding="utf-8")):
        try:
            latest = latest_release(owner_repo)
            if not latest:
                raise ValueError("missing release tag")
            if accepts_pin(tag, latest):
                print(f"ACTION {owner_repo}: current={tag} latest={latest} OK")
            else:
                print(f"ACTION_STALE {owner_repo}: {tag} != {latest}", file=sys.stderr)
                failed = True
        except (OSError, ValueError, urllib.error.URLError) as exc:
            print(f"ACTION_VERIFY_FAILED {owner_repo}: {exc}", file=sys.stderr)
            failed = True
sys.exit(1 if failed else 0)
PY
