# Frame out

Video frame extractor for Android — internal gallery, trim, sequential decode, collage, batch, save and share.

## Requirements

- JDK 17+
- Android SDK 37 / minSdk 24
- Gradle 9.7.1 (wrapper not committed; CI uses gradle/actions)

## Modules

| Module | Role |
|--------|------|
| app | UI, navigation, WorkManager entry |
| core | Room, prefs, storage, models, device capability |
| engine | Index, decode, extract, player, workers, reassembly |
| designsystem | Theme, glass, dock, icons |
| codecpack | Optional native codecs (JNI + FFmpeg CI build) |

## Build

```bash
gradle :app:assembleDebug
gradle :app:assembleRelease
bash scripts/check-lines.sh
bash scripts/check-comments.sh
bash scripts/size-report.sh
```

Size budget: under 6MiB per ABI APK without full FFmpeg pack.

## Release

1. Run **Generate Keystore** workflow once; save the four secrets (see STORE.md).
2. Tag and push: `git tag v1.0.5 && git push origin v1.0.5`
3. **Release** workflow signs per-ABI APKs and publishes a GitHub Release.
4. Optional: run **FFmpeg Codec Pack** and attach the verified codec-pack ZIP asset.
5. On a device that needs an exotic format, import that ZIP locally from Settings; the app never downloads codecs over the network.

Details: STORE.md · Privacy: PRIVACY.md · Notices: NOTICE.md

## CI

- Build on main/PR: checks, tests, debug+release APKs, size report
- Release on tag `v*`: signed APKs + GitHub Release assets
- FFmpeg: manual workflow_dispatch, signed source verification, ABI build, 16KB alignment and checksummed ZIP
- License gate: every build runs scripts/check-ffmpeg-license.sh (no GPL, nonfree or version3 flags); the FFmpeg workflow also checks config.h, the built libraries and the COPYING files in the ZIP

## Rules

- Max 300 lines per file, 40 lines per function
- No source comments
- No binary assets in repo
- Manual DI (AppContainer)
- Compose foundation only (no Material3)
- INTERNET for optional image search (nothing is sent unless you pick an engine) and a signed announcements file (see PRIVACY.md); ads are an opt-in build flag (-PadsEnabled=true) and absent from default builds, see docs/ADS_SETUP_AR.md

## Version

1.0.5 (versionCode 24) — repair build after plan audit


The IBM Plex Sans Arabic fonts (Regular, Medium, SemiBold) are committed under `designsystem/src/main/res/font/` as `ibm_plex_sans_arabic_regular.ttf`, `ibm_plex_sans_arabic_medium.ttf` and `ibm_plex_sans_arabic_semibold.ttf`; `bash scripts/check-fonts.sh` verifies they exist.
