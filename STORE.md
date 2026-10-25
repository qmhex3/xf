# Distribution checklist

## GitHub Releases (primary)

1. Run workflow Generate Keystore once; store secrets:
   - KEYSTORE_BASE64
   - KEYSTORE_PASSWORD
   - KEY_ALIAS
   - KEY_PASSWORD
2. Tag: `git tag v1.0.5 && git push origin v1.0.5`
3. Workflow Release builds signed per-ABI APKs and attaches them to the GitHub Release.
4. Build the FFmpeg Codec Pack workflow and attach `frameout-codecpack.zip` as the verified codec-pack asset.

## Play Store (optional later)

- targetSdk 37 (current)
- 16 KB page size for native libs (codec pack build uses max-page-size=16384)
- Data safety form: the developer collects nothing; declare the optional, user-initiated image search, which sends the chosen frame over HTTPS to the engine the user picks. Purchases are handled by Google Play; the app collects no payment or account data, so no extra data type applies
- Content rating: utility / photo and video
- Developer verification may affect sideloading in some regions (2026+)

## Play Billing and Pro

Build the store bundle with `-PstoreBuild=true` (sets `PLAY_BILLING`). Without it the app has no paywall and every feature is unlocked.

Create these products in Play Console with exactly these IDs:

| Product | Type | ID | Base plan / notes |
|---------|------|----|-------------------|
| Pro subscription | Subscription | `frameout_pro` | Base plans `monthly` and `yearly`; add the 3-day free-trial offer to the one that should show it (an offer whose first phase is free is picked automatically) |
| Pro lifetime | One-time product | `frameout_pro_lifetime` | One-time purchase |

Checklist:

1. Activate both products and the base plans, then enable regional pricing so prices follow each country.
2. Add `PLAY_CERT_SHA256` as a repository secret (the app signing certificate SHA-256 shown in Play Console under App signing); the Play bundle workflow needs it.
3. Free: JPG, PNG and standard WebP export, three filters (mono, sepia, warm), collages 2x1, 1x2 and 2x2, the default accent. Everything else is Pro, with three free tries per feature before the paywall.
4. Existing installs keep every feature for free for life (grandfathering is on by default). Build with `-PgrandfatherExisting=false` to turn it off.
5. Test with a license-tester account and an internal testing track before production: buy, cancel, restore and the 14-day offline grace period.

## Ads (optional, off by default)

Ads are compiled in only with `-PadsEnabled=true`; the default build has no ad SDK and no ad code. Free users of the store build (`-PstoreBuild=true`) see them; Pro, grandfathered installs and non-store builds never do. Full beginner guide in Arabic: docs/ADS_SETUP_AR.md.

```bash
gradle :app:assembleDebug -PstoreBuild=true -PadsEnabled=true
gradle :app:bundleRelease -PstoreBuild=true -PadsEnabled=true \
  -PadmobAppId=ca-app-pub-XXXX~YYYY -PadmobBannerId=ca-app-pub-XXXX/AAAA \
  -PadmobInterstitialId=ca-app-pub-XXXX/BBBB -PadmobRewardedId=ca-app-pub-XXXX/CCCC
```

Debug builds always use Google test ad units. Release builds with ads refuse to build with blank or test IDs. Add `-PadsDebugEea=true -PadsTestDevice=<hashed id>` to test the EEA consent form.

## Policy alignment

- INTERNET only for optional image search (and Google Play Billing in the store build), contains ads (declare Ads = Yes in Play Console and complete Data safety for the ad SDK; add the ad provider's consent flow for EEA/UK)
- LGPL FFmpeg (and BSD-2 dav1d) only when codec pack is used; licenses and versions are listed in About and in NOTICE.md
- Privacy policy: PRIVACY.md
- Notices: NOTICE.md

## Tamper protection (release signing hash)

The app compares its own signing certificate with a SHA-256 baked in at build time. If the hash is empty the check is skipped, so release builds must set it.

1. Get the hash: `keytool -list -v -keystore app/release.keystore -alias frameout`, copy the SHA256 line.
2. Add it as the repository secret `CERT_SHA256` (colons are ignored). The Release workflow fails without it.
3. For a local release build: `CERT_SHA256=<hash> gradle :app:assembleRelease`.

A copy that is re-signed or marked debuggable shows an exit-only dialog. Debugger and hook detections (Frida, Xposed) are only written to the diagnostic log.
