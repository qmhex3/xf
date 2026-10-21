#!/usr/bin/env bash
set -euo pipefail

REQUIRED=(
  "mkv|demuxer|matroska" "mkv|decoder|h264" "mkv|decoder|hevc" "mkv|decoder|vp9" "mkv|decoder|libdav1d"
  "mkv|parser|h264" "mkv|parser|hevc" "mkv|parser|vp9" "mkv|parser|av1"
  "webm|demuxer|matroska" "webm|decoder|vp8" "webm|decoder|vp9" "webm|parser|vp8" "webm|parser|vp9"
  "avi|demuxer|avi" "avi|decoder|mpeg4" "avi|decoder|msmpeg4v3" "avi|decoder|mjpeg"
  "avi|parser|mpeg4video" "avi|parser|mpegaudio" "avi|parser|ac3"
  "wmv|demuxer|asf" "wmv|decoder|wmv1" "wmv|decoder|wmv2" "wmv|decoder|wmv3" "wmv|decoder|vc1" "wmv|parser|vc1"
  "flv|demuxer|flv" "flv|decoder|flv" "flv|decoder|vp6f" "flv|decoder|h264" "flv|parser|h264" "flv|parser|mpegaudio"
  "ts|demuxer|mpegts" "ts|decoder|h264" "ts|decoder|hevc" "ts|decoder|mpeg2video"
  "ts|parser|h264" "ts|parser|hevc" "ts|parser|mpegvideo" "ts|parser|aac" "ts|parser|ac3" "ts|parser|mpegaudio"
  "m2ts|demuxer|mpegts" "m2ts|parser|ac3" "mts|demuxer|mpegts" "mts|parser|ac3"
  "vob|demuxer|mpegps" "vob|decoder|mpeg2video" "vob|decoder|mpeg1video" "vob|parser|mpegvideo" "vob|parser|ac3"
  "3gp|demuxer|mov" "3gp|decoder|h263" "3gp|decoder|mpeg4" "3gp|decoder|h264" "3gp|parser|h263" "3gp|parser|mpeg4video"
  "rmvb|demuxer|rm" "rmvb|decoder|rv10" "rmvb|decoder|rv20" "rmvb|decoder|rv30" "rmvb|decoder|rv40"
  "ogv|demuxer|ogg" "ogv|decoder|theora" "ogv|decoder|vp3" "ogv|parser|vp3"
  "prores|demuxer|mov" "prores|decoder|prores" "prores|parser|prores"
  "ffv1|demuxer|avi" "ffv1|decoder|ffv1"
  "dnxhd|demuxer|dnxhd" "dnxhd|demuxer|mov" "dnxhd|decoder|dnxhd" "dnxhd|parser|dnxhd"
  "j2k|demuxer|mjpeg_2000" "j2k|demuxer|image_j2k_pipe" "j2k|decoder|jpeg2000" "j2k|parser|jpeg2000"
  "dv|demuxer|dv" "dv|decoder|dvvideo"
  "mxf|demuxer|mxf" "mxf|decoder|mpeg2video" "mxf|decoder|h264" "mxf|decoder|prores" "mxf|decoder|dnxhd"
  "mxf|decoder|jpeg2000" "mxf|decoder|dvvideo" "mxf|decoder|v210" "mxf|parser|mpegvideo" "mxf|parser|h264"
  "all|protocol|file"
)

macro_for() {
  local kind="$1" name="$2"
  printf 'CONFIG_%s_%s' "$(printf '%s' "$name" | tr '[:lower:]' '[:upper:]')" "$(printf '%s' "$kind" | tr '[:lower:]' '[:upper:]')"
}

if [ "${1:-}" = "--list" ]; then
  for entry in "${REQUIRED[@]}"; do
    IFS='|' read -r _ kind name <<< "$entry"
    macro_for "$kind" "$name"; printf '\n'
  done | sort -u
  exit 0
fi

SRC_DIR="${1:-.}"
HEADER="$SRC_DIR/config_components.h"
if [ ! -f "$HEADER" ]; then
  echo "config_components.h not found in $SRC_DIR (run after ./configure)" >&2
  exit 2
fi

MISSING=0
for entry in "${REQUIRED[@]}"; do
  IFS='|' read -r format kind name <<< "$entry"
  macro="$(macro_for "$kind" "$name")"
  if ! grep -q "^#define $macro 1" "$HEADER"; then
    echo "MISSING $kind $name (needed by $format): $macro is not 1 in config_components.h" >&2
    MISSING=1
  fi
done

if [ "$MISSING" -ne 0 ]; then
  echo "Add the missing components to --enable-demuxer/--enable-parser/--enable-decoder in scripts/build-ffmpeg.sh" >&2
  exit 1
fi
echo "OK: ${#REQUIRED[@]} required FFmpeg components enabled for mkv webm avi wmv flv ts m2ts mts vob 3gp rmvb ogv prores dnxhd j2k dv mxf"
