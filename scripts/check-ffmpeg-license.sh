#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ABIS=(arm64-v8a armeabi-v7a x86_64)
LIBS=(avutil swresample swscale avcodec avformat)
FORBIDDEN='--enable-(gpl|nonfree|version3|postproc|libx264|libx265|libxvid|libfdk-aac|libvidstab|librubberband|libxavs2?|libdavs2|libcdio|frei0r|libopencore-amr(nb|wb)|libvo-amrwbenc|libaribb24|libsmbclient|decklink|openssl)([^A-Za-z0-9_-]|$)'
REQUIRED_FLAGS=(--disable-gpl --disable-nonfree --disable-version3)

fail() { echo "FAIL: $*" >&2; exit 1; }

check_source() {
  local files=("$@") file flag
  if [ "${#files[@]}" -eq 0 ]; then
    files=("$ROOT_DIR/scripts/build-ffmpeg.sh" "$ROOT_DIR"/.github/workflows/*.yml)
  fi
  for file in "${files[@]}"; do
    [ -f "$file" ] || fail "missing $file"
    if grep -nE -- "$FORBIDDEN" "$file" >&2; then fail "GPL, nonfree or version3 configure flag in $file"; fi
  done
  for flag in "${REQUIRED_FLAGS[@]}"; do
    grep -qF -- "$flag" "${files[0]}" || fail "$flag is not pinned in ${files[0]}"
  done
  echo "OK: no GPL or nonfree configure flags"
}

check_config() {
  local header="${1:-.}/config.h"
  [ -f "$header" ] || fail "config.h not found in ${1:-.} (run after ./configure)"
  grep -qE '^#define FFMPEG_LICENSE "LGPL version 2\.1 or later"$' "$header" || fail "FFMPEG_LICENSE is not LGPL version 2.1 or later"
  if grep -E '^#define CONFIG_(GPL|NONFREE|VERSION3) 1' "$header" >&2; then fail "configure enabled GPL, nonfree or version3"; fi
  echo "OK: configure result is LGPL version 2.1 or later"
}

check_binaries() {
  local root="${1:?directory with <abi>/libav*.so}" abi lib so
  for abi in "${ABIS[@]}"; do
    for lib in "${LIBS[@]}"; do
      so="$root/$abi/lib$lib.so"
      [ -f "$so" ] || fail "missing $so"
      if grep -aEq "lib$lib license: (GPL|nonfree|LGPL version 3)" "$so"; then fail "$so is not LGPL 2.1 only"; fi
      grep -aqF "lib$lib license: LGPL version 2.1 or later" "$so" || fail "$so does not report LGPL version 2.1 or later"
    done
  done
  echo "OK: all FFmpeg libraries report LGPL version 2.1 or later"
}

check_pack_text() {
  local zip="$1" name="$2" needle="$3" text
  text="$(unzip -p "$zip" "$name" 2>/dev/null || true)"
  [ -n "$text" ] || fail "$name is missing from $zip"
  grep -qF -- "$needle" <<< "$text" || fail "$name does not look like the expected license text"
}

check_pack_hash() {
  local zip="$1" name="$2" payload actual
  payload="$(unzip -p "$zip" SIGNING_PAYLOAD.txt)"
  actual="$(unzip -p "$zip" "$name" | sha256sum | awk '{print $1}')"
  grep -qE "^$actual  $name\$" <<< "$payload" || fail "$name is not covered by the signed payload"
}

check_pack() {
  local zip="${1:?codec pack zip}"
  [ -f "$zip" ] || fail "missing $zip"
  check_pack_text "$zip" COPYING.LGPLv2.1 "GNU LESSER GENERAL PUBLIC LICENSE"
  check_pack_text "$zip" COPYING.LGPLv2.1 "Version 2.1"
  check_pack_text "$zip" COPYING.dav1d "Redistribution and use in source and binary forms"
  check_pack_hash "$zip" COPYING.LGPLv2.1
  check_pack_hash "$zip" COPYING.dav1d
  echo "OK: codec pack carries a signed COPYING file for FFmpeg and dav1d"
}

case "${1:-}" in
  --source) shift; check_source "$@" ;;
  --config) shift; check_config "$@" ;;
  --binaries) shift; check_binaries "$@" ;;
  --pack) shift; check_pack "$@" ;;
  *) echo "usage: $0 --source [files] | --config <ffmpeg-src> | --binaries <root> | --pack <zip>" >&2; exit 2 ;;
esac
