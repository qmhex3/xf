# Isolated decoder process (open item)

Goal: run FFmpeg demux and decode in a service declared with `android:isolatedProcess="true"` so a decoder crash or exploit cannot reach app data.

Constraints found in the code:
- An isolated process cannot read `filesDir`, so the codec pack libraries cannot be loaded from there. The pack must be loaded from a file descriptor or copied into a location the isolated process may read.
- Media input must arrive as a `ParcelFileDescriptor`; the native open calls take a path today and would use `/proc/self/fd/N`.
- Frames must come back through `SharedMemory` or `HardwareBuffer`, not Parcel copies.
- The JNI surface to move: `FfmpegDecoderSession`, `FfmpegIndexer`, `FfmpegProbe`, `FfmpegTileGrid`.

Suggested order: probe and indexer first (no pixels returned), then single-frame decode, then the session API. Needs device tests (`device-test.yml`) before release.
