# Agent Work Log

## 2026-09-16 - TextBlock 4.3.14 carrier MMS download repair

- Outcome: repaired the production carrier MMS entry point for Android 13+ by
  registering its telephony callback with an explicit exported flag and removing
  an invalid manual URI grant. Carrier failure callbacks now stop before PDU
  parsing and clear both download-deduplication guards, allowing later attempts
  for the same MMS URL instead of suppressing them until process restart.
- Verification: the API 35 emulator's real `MmsServiceBroker` accepted the
  production download request. Two consecutive requests for the same test URL
  both entered the system MMS service and returned the expected no-carrier
  configuration error, proving failure cleanup permits retry. The persisted-MMS
  suite passed JPEG, PNG, GIF, WebP, malformed image, caption, and UI checks with
  zero fatal exceptions. `:domain:testDebugUnitTest`, debug assembly, and signed
  release assembly passed. The release manifest excludes both debug receivers.
  Version 4.3.14 was copied to Google Drive with SHA-256
  `D5BEF3FB33B5E3455EA82CDDDA47B5C0E386AC688B5557607C5F05703CBAFF91`.
- Scope boundary: the emulator cannot complete a carrier MMSC transfer. Final
  transport confirmation requires the signed build on a cellular phone, with
  the recipient number deregistered from RCS so senders use SMS/MMS.
- Agent role: main integration.
- Main model/reasoning: current Codex model; exact runtime identifier not exposed.
- Changed files: `android-smsmms/src/main/java/com/android/mms/transaction/DownloadManager.java`,
  `android-smsmms/src/main/java/com/android/mms/transaction/PushReceiver.java`,
  `data/src/main/java/com/moez/QKSMS/receiver/MmsReceivedReceiver.kt`,
  `data/src/main/java/com/moez/QKSMS/worker/ReceiveMmsWorker.kt`, debug MMS probe
  files, version metadata, changelog, and this work log. This batch is not yet
  committed.

## 2026-09-16 - TextBlock emulator MMS media harness

- Outcome: added a debug-build-only MMS fixture receiver and API 35 emulator
  harness. It persists real Android MMS rows and binary parts for JPEG, PNG,
  GIF, WebP sticker-like media, captions, and malformed image data, then uses
  TextBlock's production `SyncMessage` path to update Realm and the conversation.
- Verification: `:domain:testDebugUnitTest`, `:presentation:assembleDebug`, and
  `:presentation:assembleRelease` passed. The MMS harness passed with five
  unique MMS messages, all expected MIME parts, visible conversation thumbnails
  and captions, and zero fatal exceptions. A screenshot was also inspected to
  confirm that the media conversation rendered correctly. The fixture receiver
  is absent from the merged release manifest.
- Scope boundary: this validates the post-download provider, mapping, Realm,
  and UI paths. Google Voice or a second physical phone is still required to
  exercise carrier MMSC retrieval and SMS/MMS fallback from RCS clients.
- Agent role: main integration.
- Main model/reasoning: unknown.
- Subagent contribution: `gpt-5.6-luna` | medium | MMS persistence and sync-path
  repository survey | no direct file edits.
- Changed files: `presentation/src/debug/AndroidManifest.xml`,
  `presentation/src/debug/java/com/moez/QKSMS/debug/MmsFixtureReceiver.kt`,
  `presentation/src/main/java/com/moez/QKSMS/injection/AppComponent.kt`,
  `scripts/test-emulator-mms-media.ps1`,
  `docs/ANDROID_MESSAGING_TEST_MATRIX.md`, and this work log. This batch is not
  yet committed.

## 2026-09-14 - TextBlock 4.3.13 Android messaging test harness

- Outcome: added a reusable API 35 emulator harness that builds and installs
  TextBlock-Debug, injects repeated SMS messages, verifies provider and worker
  processing, rejects `ONLY_ALERT_ONCE`, checks keyboard resizing, and measures
  inbox and drawer placement against the visible status bar. Added GIF, WebP,
  and caption reaction-target unit coverage and a real-device MMS/RCS matrix.
- Runtime finding: the prior `DrawerLayout` padding approach did not move its
  children on API 35. Insets now apply directly to the toolbar margin and drawer
  content, with a system status-bar dimension fallback for legacy inset dispatch.
- Verification: `:domain:testDebugUnitTest`, `:presentation:assembleDebug`, and
  `:presentation:assembleRelease` passed. The emulator harness passed with three
  provider rows, three worker completions, three notification updates, an 820 px
  composer lift, a 63 px toolbar top for a 63 px status bar, and an 84 px first
  drawer-row top. The PowerShell harness parses without errors. Signed APK
  SHA-256: `E38EEB341F8F252D8D6029D96D74797ED530846F7E44650D86404034FC3C7252`.
- Agent role: main integration.
- Main model/reasoning: unknown.
- Changed files: see the working-tree diff; this batch is not yet committed.

## 2026-09-14 - TextBlock 4.3.12 repeated message alerts

- Outcome: removed the one-alert-per-notification behavior for inbound messages,
  so each new SMS or MMS can sound and vibrate even when that conversation
  already has a notification in the tray. Non-message refreshes after deletion
  remain silent.
- Device evidence: three SMS messages from Mariah reached Android at 10:05 AM,
  10:20 AM, and 11:01 AM. Notification permission was allowed, DND was off, the
  message channel was high importance, and the existing conversation
  notification carried `ONLY_ALERT_ONCE`, matching the missed-alert behavior.
- Verification: `:domain:testDebugUnitTest` and
  `:presentation:assembleRelease` passed; signed version 4.3.12 installed on the
  Pixel 10 Pro and retained the default SMS role. APK SHA-256:
  `29B663D9774E46CB932CB70752EE43CB997075B6B107B1DB8B982C1E964D6166`.
- Agent role: main integration.
- Main model/reasoning: unknown.
- Changed files: see the working-tree diff; this batch is not yet committed.

## 2026-09-13 - TextBlock 4.3.8 device test build

- Outcome: fixed Android 15 keyboard/IME overlap, enabled reactions on media-only
  MMS messages, added a full AndroidX emoji picker behind the reaction-bar `+`,
  and installed signed version 4.3.8 on the Pixel 10 Pro without changing the
  default SMS role or deleting app data.
- Verification: `:domain:testDebugUnitTest` and
  `:presentation:compileDebugKotlin` passed; `:presentation:assembleRelease`
  passed; ADB install, package version, process launch, and SMS role checks
  passed.
- Device evidence: the Android MMS provider contains valid received GIFs and no
  stranded failed/outbox MMS records. GIF transport therefore works at least
  intermittently; inline GIF rendering was made explicit for the next build.
- Agent role: main integration.
- Main model/reasoning: unknown.
- Subagent contribution: `gpt-5.6-luna` | medium | media/reaction, Galaxy Watch,
  and link-preview architecture audits | no direct file edits.
- Changed files: see the working-tree diff; this batch is not yet committed.

## 2026-09-13 - TextBlock 4.3.9 changelog correction

- Outcome: redirected the What's New dialog's More action from Quik's releases
  to TextBlock's releases, added TextBlock-specific 4.3.9 release notes, and
  included the previously verified explicit GIF-rendering update.
- Agent role: main integration.
- Main model/reasoning: unknown.
- Changed files: see the working-tree diff; this batch is not yet committed.

## 2026-09-13 - TextBlock 4.3.11 emoji picker and system-bar insets

- Outcome: deferred picker creation until its dialog container is measured and
  explicitly sized its grid rows, and now waits for EmojiCompat metadata before
  rendering, preventing blank emoji cells and startup-time conversation crashes.
  The inbox and drawer also respond to Android 15+ status-bar and display-cutout
  insets instead of drawing navigation controls behind system icons.
- Verification: `:domain:testDebugUnitTest` and release Kotlin compilation
  passed; `:presentation:assembleRelease` passed; signed version 4.3.11 was
  installed on the Pixel 10 Pro, launched without a new crash, and retained the
  default SMS role.
- Device evidence: the prior 4.3.9 build crashed after calling EmojiCompat
  before metadata initialization completed. Android's MMS provider contains
  successfully downloaded image-only messages in Mariah's thread through
  September 12, but no newer failed media attempt reached the MMS provider,
  suggesting the newest media was sent over a different transport such as RCS.
- Agent role: main integration.
- Main model/reasoning: unknown.
- Changed files: see the working-tree diff; this batch is not yet committed.
## 2026-09-16 - On-device political classifier and quarantine retention

- Outcome: added a reproducible 128 KB hashed text neural network behind the
  rule classifier, with a conservative 0.93 quarantine threshold and fail-open
  behavior. Android Contacts still bypass all classification. Added per-message
  quarantine records and daily deletion after 90 days; marking a message as
  not spam removes its retention record.
- Training evidence: the deterministic synthetic held-out set contained 192
  examples with zero thresholded false positives and zero misses. This is a
  smoke test, not a claim about real-world accuracy; corrections remain the
  mechanism for improving later training sets.
- Verification: `:domain:testDebugUnitTest` and `:data:testDebugUnitTest`
  passed, including the two political messages observed on the Pixel and
  counterexamples for contacts, banking, pharmacy, appointments, and ordinary
  polls. `:presentation:assembleRelease` passed; signed version 4.3.14 was
  installed on the Pixel 10 Pro, launched successfully, migrated Realm from
  schema 16 to 17, and retained the default SMS role. APK SHA-256:
  `2C44B62FDC6711B276FB70F790EAE9A449C94FD793BB21752B313FD9563BD20F`.
- Agent role: main integration.
- Main model/reasoning: gpt-6-astra | extra high.
- Commit: this entry is included in the `Prepare TextBlock 4.3.14 Play release`
  commit.

## 2026-09-17 - Media confirmation and system-bar cleanup

- Outcome: confirmed on the Pixel that TextBlock now receives and renders a
  regular MMS image, a sticker, and an animated GIF. Consolidated the duplicate
  conversation three-dot controls by moving Conversation info into the standard
  overflow menu. Added shared Android 15+ status- and navigation-bar insets for
  all activities, while retaining screen-specific IME handling in conversations
  and drawer handling in the inbox.
- Verification: `:presentation:compileDebugKotlin`,
  `:domain:testDebugUnitTest`, `:data:testDebugUnitTest`, and
  `:presentation:assembleRelease` passed. Signed version 4.3.14 was installed on
  the Pixel 10 Pro and retained the default SMS role. The device locked before
  final live screenshots of Settings could be captured. APK SHA-256:
  `998E210870C20AA8CA0C621B1BA2C9F8198287F028F47CB03FFC5EE1CADD4F0C`.
- Agent role: main integration.
- Main model/reasoning: gpt-6-astra | extra high.
- Changed files: see the working-tree diff; this batch is not yet committed.

## 2026-09-18 - Three-button navigation audit and Play Store preparation

- Outcome: audited the inbox, conversation composer, navigation drawer, and
  Settings screens on an Android 15 emulator using classic three-button
  navigation. Fixed the inbox content container so its compose button clears
  the navigation bar. Added TextBlock-owned source, release, support, and
  privacy links; refreshed the Play listing copy; added a public privacy-policy
  page; and prepared a 1024 x 500 feature graphic plus four 1080 x 2160 phone
  screenshots made with synthetic messages.
- Verification: `:domain:testDebugUnitTest`, `:data:testDebugUnitTest`,
  `:presentation:bundleRelease`, and `:presentation:assembleRelease` passed.
  The signed release build was installed on a wiped API 35 emulator and the
  four store screenshots were visually checked. AAB SHA-256:
  `F19EFD9B4740163EFE8C9C1D28A59D228C67F45A657E86FDE2E50A29AF8AB82C`.
- Play Console note: the original draft is permanently associated with the
  incorrect package `com.textBlock`; the release bundle uses the correct
  package `com.caelh.textblock`, so a fresh Play app entry is required.
- Agent role: main integration.
- Main model/reasoning: gpt-6-astra | extra high.
- Commit: this entry is included in the `Prepare TextBlock 4.3.14 Play release`
  commit.

## 2026-09-20 - GitHub support links

- Outcome: corrected the existing drawer star request so it opens the TextBlock
  repository instead of the upstream QUIK repository. Added a persistent,
  low-key Settings row reading "Support the TextBlock developer" with a
  "Star TextBlock on GitHub" action. The drawer request remains dismissible.
- Verification: `:presentation:bundleRelease` and
  `:presentation:assembleRelease` passed. AAB SHA-256:
  `84D143AD5B205B3851609282B2443E04FD130BFFC3147E0A83642AF067E21C68`.
- Agent role: main integration.
- Main model/reasoning: gpt-6-astra | extra high.
- Commit: this entry is included in the `Fix TextBlock GitHub support links`
  commit.
