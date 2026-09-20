# Android Messaging Test Matrix

This matrix separates deterministic app tests from transport behavior that an
Android emulator cannot reproduce faithfully.

## Automated checks

| Area | Test | Expected result |
| --- | --- | --- |
| Build | Domain unit tests and debug APK assembly | Gradle reports `BUILD SUCCESSFUL` |
| SMS receipt | Inject three SMS messages from one emulator sender | Three unread SMS provider rows are created |
| SMS processing | Inspect TextBlock logs | `ReceiveSmsWorker` completes once per message with no fatal exception |
| Repeated alerts | Inspect the active conversation notification after every SMS | Notification updates to each body and never uses `ONLY_ALERT_ONCE` |
| Keyboard layout | Open the injected conversation and type with the software keyboard visible | Composer moves upward by at least 300 px and the draft remains visible |
| Status-bar layout | Compare the visible system status-bar inset with the inbox toolbar bounds | Toolbar begins at or below the status-bar inset |
| Drawer layout | Open the navigation drawer and inspect its first row | Drawer content begins below the status-bar inset |
| Media reactions | Run `MessageReactionTargetTest` | JPEG, GIF, WebP, video, audio, and generic attachments are reactable |
| Media captions | Run `MessageReactionTargetTest` | Caption text is used instead of a generic attachment label |
| MMS media persistence | Inject MMS fixtures through `PduPersister` | JPEG, PNG, GIF, WebP, and malformed image records contain the expected provider parts |
| MMS media rendering | Open the injected conversation | Media thumbnails and captions render; malformed media does not crash the app |

Run the emulator notification test from PowerShell:

```powershell
.\scripts\test-emulator-sms-notifications.ps1
```

Use `-SkipBuild` when the current debug APK is already built. The script uses a
headless `PixelPhone-API35` emulator by default, installs `TextBlock-Debug`,
makes it the emulator's default SMS app, and leaves real-phone data untouched.

Run the MMS media test separately:

```powershell
.\scripts\test-emulator-mms-media.ps1
```

The MMS harness is available only in the debug build. It creates genuine
`content://mms` inbox and part records through Android's MMS `PduPersister`,
then runs TextBlock's normal provider-to-Realm synchronization. Each run uses
unique message IDs so old emulator data cannot produce a false pass. It covers
post-download storage, parsing, GIF/WebP selection, rendering, captions, and
malformed-media resilience. It does not emulate a carrier MMSC download.

## Real-device transport checks

Use two real phones and keep the received message notification uncleared between
steps. Record whether the sender reports SMS, MMS, or RCS for each message.

| Send from second phone | Verify in TextBlock | Why this remains manual |
| --- | --- | --- |
| Three separate SMS messages | Every message appears and alerts | Confirms carrier delivery and device sound/vibration policy |
| JPEG with and without a caption | Image downloads, opens, and can be reacted to | Exercises carrier MMS download and APN behavior |
| Animated GIF | GIF downloads and animates in the conversation | Emulator SMS injection cannot deliver carrier MMS |
| WebP or app sticker | Payload appears if sent as MMS; note its MIME type | Stickers may instead use an app-specific or RCS transport |
| Reaction to newest and older images | Receiving phone identifies the intended image when supported | SMS/MMS reactions are text conventions, not stable message IDs |
| Same sequence while Zeta is foreground | Every TextBlock alert is presented | Exercises real foreground-app and OEM notification behavior |
| Same sequence with Galaxy Watch connected | Phone and watch behavior matches settings | Requires Wear OS notification bridging on actual hardware |

## RCS boundary

TextBlock is an SMS/MMS app. Android does not expose Google Messages' consumer
RCS transport to third-party default SMS apps. If the sender labels a failed
sticker, GIF, or picture as an RCS chat message, TextBlock will not receive that
payload. Repeat that case with RCS disabled or with the conversation explicitly
using SMS/MMS before treating it as a TextBlock defect.

For a failed real-device case, collect the approximate send time, sender-side
transport label, attachment type, and whether Android's `content://mms` provider
contains a new row. That distinguishes carrier delivery failures from TextBlock
parsing or rendering failures.
