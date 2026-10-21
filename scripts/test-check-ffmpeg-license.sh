#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CHECK="$ROOT_DIR/scripts/check-ffmpeg-license.sh"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

expect_fail() {
  local label="$1"; shift
  if bash "$CHECK" "$@" >/dev/null 2>&1; then echo "FAIL: $label was accepted"; exit 1; fi
}

printf '%s\n' '#define FFMPEG_LICENSE "LGPL version 2.1 or later"' '#define CONFIG_GPL 0' '#define CONFIG_NONFREE 0' '#define CONFIG_VERSION3 0' > "$WORK/config.h"
bash "$CHECK" --config "$WORK" >/dev/null
sed -i 's/^#define CONFIG_GPL 0/#define CONFIG_GPL 1/' "$WORK/config.h"
expect_fail "CONFIG_GPL 1" --config "$WORK"
sed -i 's/^#define CONFIG_GPL 1/#define CONFIG_GPL 0/; s/^#define CONFIG_NONFREE 0/#define CONFIG_NONFREE 1/' "$WORK/config.h"
expect_fail "CONFIG_NONFREE 1" --config "$WORK"
sed -i 's/^#define CONFIG_NONFREE 1/#define CONFIG_NONFREE 0/; s/LGPL version 2.1 or later/GPL version 2 or later/' "$WORK/config.h"
expect_fail "GPL license string" --config "$WORK"
rm "$WORK/config.h"
expect_fail "absent config.h" --config "$WORK"

printf '%s\n' '--disable-gpl' '--disable-nonfree' '--disable-version3' > "$WORK/build.sh"
bash "$CHECK" --source "$WORK/build.sh" >/dev/null
printf '%s\n' '  --enable-gpl' >> "$WORK/build.sh"
expect_fail "--enable-gpl" --source "$WORK/build.sh"
printf '%s\n' '--disable-gpl' '--disable-nonfree' '--disable-version3' '  --enable-nonfree' > "$WORK/build.sh"
expect_fail "--enable-nonfree" --source "$WORK/build.sh"
printf '%s\n' '--disable-gpl' '--disable-nonfree' '--disable-version3' '  --enable-libx264' > "$WORK/build.sh"
expect_fail "--enable-libx264" --source "$WORK/build.sh"
printf '%s\n' '--disable-gpl' '--disable-nonfree' > "$WORK/build.sh"
expect_fail "unpinned --disable-version3" --source "$WORK/build.sh"

for abi in arm64-v8a armeabi-v7a x86_64; do
  mkdir -p "$WORK/libs/$abi"
  for lib in avutil swresample swscale avcodec avformat; do
    printf 'x\0lib%s license: LGPL version 2.1 or later\0y' "$lib" > "$WORK/libs/$abi/lib$lib.so"
  done
done
bash "$CHECK" --binaries "$WORK/libs" >/dev/null
printf 'x\0libavcodec license: GPL version 2 or later\0y' > "$WORK/libs/x86_64/libavcodec.so"
expect_fail "GPL binary" --binaries "$WORK/libs"
printf 'x\0libavcodec license: nonfree and unredistributable\0y' > "$WORK/libs/x86_64/libavcodec.so"
expect_fail "nonfree binary" --binaries "$WORK/libs"
rm "$WORK/libs/x86_64/libavcodec.so"
expect_fail "missing binary" --binaries "$WORK/libs"

mkdir "$WORK/pack"
printf 'GNU LESSER GENERAL PUBLIC LICENSE\n Version 2.1, February 1999\n' > "$WORK/pack/COPYING.LGPLv2.1"
printf 'Redistribution and use in source and binary forms, with or without\n' > "$WORK/pack/COPYING.dav1d"
(cd "$WORK/pack" && sha256sum COPYING.LGPLv2.1 COPYING.dav1d > SIGNING_PAYLOAD.txt && zip -qr ../good.zip .)
bash "$CHECK" --pack "$WORK/good.zip" >/dev/null
(cd "$WORK/pack" && rm COPYING.dav1d && zip -qr ../no-dav1d.zip .)
expect_fail "pack without COPYING.dav1d" --pack "$WORK/no-dav1d.zip"
printf 'Redistribution and use in source and binary forms, with or without\n' > "$WORK/pack/COPYING.dav1d"
(cd "$WORK/pack" && sha256sum COPYING.LGPLv2.1 > SIGNING_PAYLOAD.txt && zip -qr ../unsigned.zip .)
expect_fail "license outside the signed payload" --pack "$WORK/unsigned.zip"
echo "OK: check-ffmpeg-license self-test"
