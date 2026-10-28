# Frame out Privacy Policy

Last updated: 2026-10-28

## Summary

Frame out is a local video frame extraction app. Your videos and frames are processed on your device and are never uploaded. The app has no accounts and does not sell personal data. The app shows ads; see Advertising below.

## Network

The app declares the INTERNET permission for three purposes: (1) optional image search, (2) advertising and purchase verification (below), and (3) downloading the signed announcements file described under "Announcements and promotions". For image search: when you pick a search engine, the chosen frame is uploaded to that site inside a private in-app browser (cookies and cache are cleared on close). Apart from that and the advertising described below, nothing else is sent.

## Announcements and promotions

The app periodically downloads one small public file (text, links and optionally images or video) from static file hosting to show occasional notifications (for example an occasion greeting, an update note or an important notice) and an in-app card introducing our other apps. These are not AdMob ads and nothing opens unless you tap the card.

- The request is an ordinary download of a public file. No account, advertising ID or device identifier is attached, and content is not personalised. As with any website, the hosting provider may log the IP address and time of the request under its own policy; we do not use those logs to identify you.
- The file is digitally signed (ECDSA P-256). The app ignores any file whose signature, app ID or expiry does not verify, and checks the SHA-256 of every image or video before showing it. Media is cached temporarily on the device.
- You can turn off the "Announcements" notification channel or the notification permission in Android settings at any time.

## Advertising

The app shows ads served by a third-party ad network. The ad SDK may collect and use device identifiers (such as the advertising ID) and approximate usage data to deliver and measure ads, according to that provider's privacy policy and the ad settings on your device. Where required (for example in the EEA and UK), the app asks for your consent before personalised ads. The ads are served by Google AdMob (Google Ireland Limited in the EEA and UK, Google LLC elsewhere); see https://policies.google.com/technologies/ads. Consent and privacy choices are collected through Google's User Messaging Platform, and a privacy-choices entry appears in Settings wherever the law requires one. Ads are never shown to Pro subscribers, and watching an optional rewarded ad grants a few minutes of a Pro feature on the device only. To detect blocked ads the app resolves a few ad-network host names through your normal DNS and checks whether the device is online; it stores only counters and timestamps on the device.

## Purchases (Google Play build only)

The Google Play build offers an optional paid upgrade, Frame out Pro, as a monthly or yearly subscription or a one-time lifetime purchase. Purchases are processed entirely by Google Play. Frame out has no account system, does not see or store your payment details, and does not send purchase data to any server of its own. The app asks Google Play on the device whether you own Pro and stores only the result on the device: whether Pro is active, the plan type (subscription or lifetime) and the time of the last check. Uninstalling the app removes it, and Restore purchases rebuilds it from your Google account. Builds distributed outside Google Play contain no billing code and make no purchase requests.

## Data stored on device

- Project metadata and frame indexes in an on-device Room database
- Optional frame images and thumbnails under the app private storage
- User preferences in SharedPreferences
- Pro status flag and the counters of free tries per feature (SharedPreferences)
- Optional images saved to the system gallery (MediaStore) when the user chooses Save

Uninstalling the app removes private app storage. Gallery exports remain until the user deletes them.

## Permissions

| Permission | Purpose |
|------------|---------|
| READ_MEDIA_VIDEO / READ_MEDIA_IMAGES / READ_EXTERNAL_STORAGE (legacy) | Read videos and images the user selects |
| READ_MEDIA_VISUAL_USER_SELECTED | Partial media access on Android 14+ |
| POST_NOTIFICATIONS | Progress for background extraction and occasional announcements |
| FOREGROUND_SERVICE / DATA_SYNC / MEDIA_PROCESSING | Continue extraction while the app is backgrounded |
| WAKE_LOCK | Keep CPU available during long extraction |
| com.android.vending.BILLING (Google Play build) | Added by the Google Play Billing Library so the app can show prices and check whether you own Pro |
| ACCESS_NETWORK_STATE | Required by the bundled on-device ML Kit analyzers; no data leaves the device through it |
| ACCESS_MEDIA_LOCATION | Read the GPS tag stored in a selected video or image so it can be copied into frames you export or capture when the keep-location setting is on. The app never requests live device location |

No contacts, microphone, camera, or accounts access. The app does not use GPS or any live device location, and uses the network only for the optional image search, advertising, the signed announcements file described above and, in the Google Play build, for purchases handled by Google Play. It only reads the location metadata already embedded in media you choose, and only when the keep-location setting is on, to write it into the images you export or capture from that media. This metadata stays on the device and is never transmitted. Gallery exports and shares can carry that metadata, so check the result before sharing it.

## Sharing

Share uses the system share sheet with content URIs via FileProvider. Frame out does not upload shared files.

## Diagnostics

Optional local diagnostic text can be exported by the user through the system share sheet. It is not sent automatically.

## Children

The app is a general utility and does not target children or collect age-related data.

## Contact

For privacy questions about a self-hosted or forked build, contact the distributor of that build.
