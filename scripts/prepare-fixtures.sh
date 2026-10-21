#!/usr/bin/env bash
set -uo pipefail
OUT="${1:-build/fixtures}"
mkdir -p "$OUT"
rm -f "$OUT"/*
MANIFEST="$OUT/manifest.txt"
: > "$MANIFEST"
FAILED_FIXTURES=()
SRC_SD="testsrc2=size=320x240:rate=30"
SRC_PAL="testsrc2=size=720x576:rate=25"
SRC_HD="testsrc2=size=1920x1080:rate=25"
make_fixture() {
  local name="$1" source="$2"
  shift 2
  local inputs=(-f lavfi -i "$source")
  if [ -n "${AUDIO_SRC:-}" ]; then inputs+=(-f lavfi -i "$AUDIO_SRC" -shortest); fi
  if ffmpeg -v error -y "${inputs[@]}" -frames:v 30 "$@" "$OUT/$name" && [ -s "$OUT/$name" ]; then
    local frames
    frames="$(ffprobe -v error -count_frames -select_streams v:0 -show_entries stream=nb_read_frames -of default=nw=1:nk=1 "$OUT/$name" 2>/dev/null | head -n1)"
    if [[ "$frames" =~ ^[0-9]+$ ]] && [ "$frames" -gt 0 ]; then
      echo "$name $frames" >> "$MANIFEST"
      return 0
    fi
  fi
  echo "failed fixture $name" >&2
  FAILED_FIXTURES+=("$name")
  rm -f "$OUT/$name"
}
make_fixture_av() {
  AUDIO_SRC="sine=frequency=440:sample_rate=48000:duration=2" make_fixture "$@"
}
check_packet_size() {
  local name="$1" size="$2" file="$OUT/$1" bytes
  [ -s "$file" ] || return 0
  bytes="$(stat -c %s "$file" 2>/dev/null || stat -f %z "$file")"
  if [ $((bytes % size)) -ne 0 ]; then
    echo "failed fixture $name: size $bytes is not a multiple of $size" >&2
    FAILED_FIXTURES+=("$name")
    grep -v "^$name " "$MANIFEST" > "$MANIFEST.tmp" || true
    mv "$MANIFEST.tmp" "$MANIFEST"
    rm -f "$file"
  fi
}
make_fixture mpeg4.avi "$SRC_SD" -c:v mpeg4
make_fixture msmpeg4.avi "$SRC_SD" -c:v msmpeg4
make_fixture wmv1.asf "$SRC_SD" -c:v wmv1
make_fixture wmv2.wmv "$SRC_SD" -c:v wmv2
make_fixture flv1.flv "$SRC_SD" -c:v flv
make_fixture h264_bframes.ts "$SRC_SD" -c:v libx264 -bf 2 -pix_fmt yuv420p
make_fixture h264_raw.h264 "$SRC_SD" -c:v libx264 -pix_fmt yuv420p -f h264
make_fixture hevc_raw.hevc "$SRC_SD" -c:v libx265 -pix_fmt yuv420p -f hevc
make_fixture hevc.mkv "$SRC_SD" -c:v libx265 -pix_fmt yuv420p
make_fixture mpeg1.mpg "$SRC_SD" -c:v mpeg1video
make_fixture mpeg2.mpg "$SRC_PAL" -c:v mpeg2video
make_fixture mpeg2_raw.m2v "$SRC_PAL" -c:v mpeg2video -f mpeg2video
make_fixture mpeg2.vob "$SRC_PAL" -c:v mpeg2video -f vob
make_fixture theora.ogv "$SRC_SD" -c:v libtheora
make_fixture vp8.webm "$SRC_SD" -c:v libvpx
make_fixture vp9.mkv "$SRC_SD" -c:v libvpx-vp9
make_fixture av1.mkv "$SRC_SD" -c:v libaom-av1 -cpu-used 8 -pix_fmt yuv420p
make_fixture still.avif "$SRC_SD" -frames:v 1 -c:v libaom-av1 -still-picture 1 -pix_fmt yuv420p -f avif
make_fixture animated.avif "$SRC_SD" -c:v libaom-av1 -cpu-used 8 -pix_fmt yuv420p -f avif
make_fixture prores.mov "$SRC_SD" -c:v prores_ks
make_fixture dv.dv "$SRC_PAL" -c:v dvvideo -pix_fmt yuv420p
make_fixture mxf_mpeg2.mxf "$SRC_PAL" -c:v mpeg2video -pix_fmt yuv422p -b:v 30M -f mxf
make_fixture nut.nut "$SRC_SD" -c:v mpeg4 -f nut
make_fixture vp8.ivf "$SRC_SD" -c:v libvpx -f ivf
make_fixture rawvideo.y4m "$SRC_SD" -c:v rawvideo -pix_fmt yuv420p -f yuv4mpegpipe
make_fixture mjpeg_raw.mjpeg "$SRC_SD" -c:v mjpeg -pix_fmt yuvj420p -f mjpeg
make_fixture rv10.rm "$SRC_SD" -c:v rv10
make_fixture rv20.rm "$SRC_SD" -c:v rv20
make_fixture h263.3gp "$SRC_SD" -c:v h263 -s 176x144
make_fixture svq1.mov "$SRC_SD" -c:v svq1 -s 320x240
make_fixture gxf_mpeg2.gxf "$SRC_PAL" -c:v mpeg2video -pix_fmt yuv422p -f gxf
make_fixture_av fmt_mkv.mkv "$SRC_SD" -c:v libx264 -pix_fmt yuv420p -bf 2 -c:a ac3
make_fixture_av fmt_webm.webm "$SRC_SD" -c:v libvpx-vp9 -pix_fmt yuv420p -c:a libopus
make_fixture_av fmt_avi.avi "$SRC_SD" -c:v mpeg4 -bf 2 -c:a libmp3lame
make_fixture_av fmt_wmv.wmv "$SRC_SD" -c:v wmv2 -c:a wmav2
make_fixture_av fmt_flv.flv "$SRC_SD" -c:v flv -c:a libmp3lame -ar 44100
make_fixture_av fmt_ts.ts "$SRC_SD" -c:v libx264 -bf 2 -pix_fmt yuv420p -c:a ac3 -f mpegts
make_fixture_av fmt_m2ts.m2ts "$SRC_SD" -c:v libx264 -bf 2 -pix_fmt yuv420p -c:a ac3 -f mpegts -mpegts_m2ts_mode 1
make_fixture_av fmt_mts.mts "$SRC_SD" -c:v libx264 -bf 2 -pix_fmt yuv420p -c:a ac3 -f mpegts -mpegts_m2ts_mode 1
make_fixture_av fmt_vob.vob "$SRC_PAL" -c:v mpeg2video -c:a ac3 -f vob
make_fixture_av fmt_3gp.3gp "$SRC_SD" -c:v h263 -s 176x144 -c:a aac
make_fixture fmt_rmvb.rmvb "$SRC_SD" -c:v rv20 -an -f rm
make_fixture_av fmt_ogv.ogv "$SRC_SD" -c:v libtheora -c:a libvorbis
make_fixture fmt_prores.mov "$SRC_SD" -c:v prores_ks -profile:v 3 -pix_fmt yuv422p10le
make_fixture fmt_dnxhr.mov "$SRC_SD" -c:v dnxhd -profile:v dnxhr_lb -pix_fmt yuv422p
make_fixture fmt_j2k.mov "$SRC_SD" -c:v jpeg2000 -pix_fmt rgb24
make_fixture fmt_j2k_gray.mov "$SRC_SD" -c:v jpeg2000 -pix_fmt gray
make_fixture fmt_dv.dv "$SRC_PAL" -c:v dvvideo -pix_fmt yuv420p -aspect 4:3
make_fixture fmt_mxf_dnxhd.mxf "$SRC_HD" -c:v dnxhd -b:v 120M -pix_fmt yuv422p -f mxf
check_packet_size fmt_dv.dv 144000
check_packet_size fmt_ts.ts 188
check_packet_size fmt_m2ts.m2ts 192
check_packet_size fmt_mts.mts 192
fetch_rv40() {
  local url="${RV40_SAMPLE_URL:-}" target="$OUT/fmt_rv40.rmvb"
  [ -n "$url" ] || return 0
  if ! curl -fsSL --max-time 120 "$url" -o "$target" || [ ! -s "$target" ]; then
    echo "failed fixture fmt_rv40.rmvb (download from $url)" >&2
    FAILED_FIXTURES+=("fmt_rv40.rmvb")
    rm -f "$target"
    return 0
  fi
  if [ -n "${RV40_SHA256:-}" ] && [ "$(sha256sum "$target" | awk '{print $1}')" != "$RV40_SHA256" ]; then
    echo "failed fixture fmt_rv40.rmvb (sha256 mismatch)" >&2
    FAILED_FIXTURES+=("fmt_rv40.rmvb")
    rm -f "$target"
    return 0
  fi
  echo "fmt_rv40.rmvb $(sha256sum "$target" | awk '{print $1}')" >> "$OUT/native-only.txt"
}
fetch_rv40
echo "fixtures: $(wc -l < "$MANIFEST")"
if [ "${#FAILED_FIXTURES[@]}" -ne 0 ]; then
  printf 'FAIL: required fixtures failed: %s\n' "${FAILED_FIXTURES[*]}" >&2
  exit 1
fi
test -s "$MANIFEST"
