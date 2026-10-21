#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WORKFLOW="$ROOT_DIR/.github/workflows/ffmpeg-build.yml"
NDK_CURRENT="$(grep -oE 'ndk;[0-9.]+' "$WORKFLOW" | head -n1 | cut -d';' -f2)"
FFMPEG_CURRENT="$(grep -oE 'default: "[0-9.]+' "$WORKFLOW" | head -n1 | tr -cd '0-9.\n')"
FFMPEG_EXPECTED="${FFMPEG_VERSION_OVERRIDE:-$FFMPEG_CURRENT}"
DAV1D_CURRENT="$(grep -oE 'DAV1D_VERSION:-[0-9.]+' "$ROOT_DIR/scripts/build-ffmpeg.sh" | head -n1 | cut -d- -f2)"
export NDK_CURRENT FFMPEG_EXPECTED DAV1D_CURRENT
python3 - <<'PY'
import os
import re
import sys
import urllib.request
import xml.etree.ElementTree as ET


def fetch(url):
    req = urllib.request.Request(url, headers={'User-Agent': 'FrameOut-version-verifier'})
    with urllib.request.urlopen(req, timeout=20) as response:
        return response.read()


def version_key(value):
    return tuple(int(part) for part in re.findall(r'\d+', value))

try:
    xml = ET.fromstring(fetch('https://dl.google.com/android/repository/repository2-3.xml'))
    ndk = []
    for package in xml.findall('.//remotePackage'):
        path = package.attrib.get('path', '')
        if not path.startswith('ndk;'):
            continue
        channel = package.find('./channelRef')
        if channel is not None and channel.attrib.get('ref') not in (None, 'channel-0'):
            continue
        version = path.split(';', 1)[1]
        if re.fullmatch(r'\d+\.\d+\.\d+', version):
            ndk.append(version)
    current = os.environ['NDK_CURRENT']
    latest = max(ndk, key=version_key) if ndk else ''
    print(f'NDK-stable: current={current} latest={latest}')
    if current not in ndk:
        raise SystemExit('NDK_CURRENT is not an available official stable SDK package')
    if latest != current:
        raise SystemExit('NDK version is not the latest stable package')

    html = fetch('https://ffmpeg.org/releases/').decode('utf-8', 'replace')
    versions = sorted(set(re.findall(r'ffmpeg-([0-9]+\.[0-9]+\.[0-9]+)(?:[._-]|\.tar)', html)), key=version_key)
    latest_ffmpeg = versions[-1] if versions else ''
    print(f'FFmpeg-stable: current={os.environ["FFMPEG_EXPECTED"]} latest={latest_ffmpeg}')
    if latest_ffmpeg != os.environ['FFMPEG_EXPECTED']:
        raise SystemExit('FFmpeg version is not the latest published stable release')
    listing = fetch('https://downloads.videolan.org/pub/videolan/dav1d/').decode('utf-8', 'replace')
    dav1d = sorted(set(re.findall(r'href="([0-9]+\.[0-9]+\.[0-9]+)/"', listing)), key=version_key)
    latest_dav1d = dav1d[-1] if dav1d else ''
    print(f'dav1d-stable: current={os.environ["DAV1D_CURRENT"]} latest={latest_dav1d}')
    if latest_dav1d != os.environ['DAV1D_CURRENT']:
        raise SystemExit('dav1d version is not the latest published stable release')
except Exception as exc:
    print(f'OFFICIAL_VERSION_VERIFY_FAILED: {exc}', file=sys.stderr)
    raise SystemExit(1)
PY
