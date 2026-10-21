# Third-party notices

## AndroidX / Jetpack

Copyright The Android Open Source Project. Licensed under the Apache License, Version 2.0.

## Kotlin

Copyright JetBrains s.r.o. and contributors. Apache License 2.0.

## FFmpeg (optional codec pack)

When the optional native codec pack is built and shipped, FFmpeg is used under the GNU LGPL version 2.1 or later only:

- Built without --enable-gpl, without --enable-nonfree and without --enable-version3 (the three are pinned off in scripts/build-ffmpeg.sh)
- Dynamic linking via shared libraries
- CI fails if a GPL, nonfree or version3 flag appears (scripts/check-ffmpeg-license.sh --source), if the configure result is not "LGPL version 2.1 or later" (--config), or if a built library reports another license (--binaries)
- The license text ships in the codec pack as COPYING.LGPLv2.1 and is shown in About
- Source corresponding to the exact build is obtainable from the project release assets or build scripts under scripts/build-ffmpeg.sh, which downloads the signed upstream tarball for the version printed in the codec pack MANIFEST.txt. Codec-pack ZIP assets include SHA-256 manifests

FFmpeg is a trademark of Fabrice Bellard. See https://ffmpeg.org/legal.html

## dav1d (optional codec pack)

The AV1 decoder dav1d, Copyright VideoLAN and dav1d authors, is linked statically into libavcodec.so under the BSD 2-Clause license. The license text ships in the codec pack as COPYING.dav1d and is shown in About. Source: https://code.videolan.org/videolan/dav1d

## Codec pack contents

Each codec-pack ZIP carries COPYING.LGPLv2.1 (FFmpeg), COPYING.dav1d (dav1d) and MANIFEST.txt with both versions. The SHA-256 of both COPYING files is part of the signed payload, and the app refuses to install a pack where either file is missing or altered.

## IBM Plex (fonts, if bundled)

IBM Plex fonts are licensed under the SIL Open Font License 1.1 when included under designsystem/src/main/res/font/.
