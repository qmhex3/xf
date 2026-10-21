> Historical audit retained for traceability; it describes the pre-repair state and is not the current implementation status.

# FrameX — second audit and repair log

## Changes applied

- Replaced destructive Room migration fallback with explicit version 1→2 and 2→3 migrations, including the video index table and project extraction fields.
- Corrected Android 14+ partial-media access checks and made the media scanner query only collections the app can read.
- Persisted SAF read permission after document selection.
- Reworked Motion Photo extraction to read XMP and copy the embedded video through a file channel instead of loading the entire source into memory.
- Restricted Live Photo filename pairing to exact base-name matches to reduce false matches.
- Made extraction checkpoints advance only after frame batches have been written to Room, and persist Paused on coroutine cancellation/system stop.
- Fixed Cancelled versus Paused state reporting for the system, GIF, and FFmpeg paths.
- Added GIF sampling choices for every frame, every N frames, interval, fixed count, keyframe-like sampling, and scene sampling; Auto-save now writes a full frame file instead of a thumbnail.
- Added FFmpeg sampling selection, real decoded PTS and keyframe metadata, content-URI file-descriptor access, duration discovery, and guarded frame-buffer allocation.
- Fixed FFmpeg build output paths, enabled swscale, and aligned Gradle versions with the project plan.
- Release workflow now refuses to publish an unsigned release when signing secrets are missing.
- Replaced JPEG round-trip YUV conversion with direct YUV-to-ARGB conversion and closes decoded Image objects.
- Increased the reassembly sample buffer based on track metadata, with a bounded maximum.
- Added film-strip, storyboard, mosaic, and contact-sheet collage layouts, corner-radius support, and JPG/PNG export selection.
- Added the 1-second extraction interval setting, removed WebP from the settings choices to match the plan, and made disk-space estimates depend on Auto-save.
- Gallery writes now delete incomplete MediaStore rows when encoding fails.
- Added pure Kotlin tests for FFmpeg sample selection.

## Checks completed in this environment

- `scripts/check-lines.sh`: passed.
- `scripts/check-comments.sh`: passed.
- Bash syntax checks for build/check scripts: passed.
- XML parsing: passed.
- Kotlin compilation and runtime checks for the pure extraction-selection logic: passed.
- ZIP integrity is checked after packaging.

## Still not safe to mark complete

- A full Android/Gradle build and Android instrumentation tests were not run: this environment has no Gradle executable or Android SDK. GitHub Actions must confirm compilation and packaging.
- FFmpeg sampling for variable-frame-rate files still uses a frame-count/duration estimate to choose candidate indices; decoded PTS is used for saved frames, but a persisted exact FFmpeg sample index is not yet implemented.
- GIF decoding still uses Android `Movie`, which does not expose every original GIF frame faithfully and reads the GIF into memory. A dedicated streaming GIF/APNG/animated-WebP decoder is still needed.
- Video reassembly still does not implement the required decode/re-encode fallback for arbitrary GOP boundaries. The larger sample buffer reduces common failures but does not make all cuts valid or lossless.
- HDR/color-space preservation and tiled 8K decoding are not implemented.
- Live Photo pairing uses exact filename stems, not Apple content identifiers.
- Save-folder selection and the original-frame-date versus extraction-time naming policy are not wired through the UI and storage layer.
- Tablet/foldable sidebar behavior and the full context-sensitive Morph Dock remain incomplete.
- Device/OEM, 16KB final-APK, ABI, large-file, lifecycle-timeout, and end-to-end media tests still require CI/device execution.
- The separately mentioned `workfile` was not present among the files available in this workspace, so it could not be merged without guessing its contents.
