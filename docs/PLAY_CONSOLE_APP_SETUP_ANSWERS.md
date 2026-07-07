# Play Console App Setup Answers

Last researched: 2026-07-07

## Purpose

This document gives recommended Play Console answers for TextBlock's app setup
forms. It is based on Google's current Play Console help and policy pages plus
the current TextBlock release build.

TextBlock should be presented consistently as:

- A free SMS/MMS app.
- A default SMS handler.
- A local-first message filtering app.
- Not a government, health, financial, child-directed, or ad-supported app.
- Not a background-only SMS scanner.

The current Play listing should not mention donations until a real Google Play
Billing implementation is added.

## Current Build Facts Used

- Package id: `com.caelh.textblock`
- Version: `4.3.7`
- Version code: `2239`
- Target SDK: `35`
- Release artifact for Play: `TextBlock-v4.3.7-release.aab`
- The current release APK does not declare `android.permission.INTERNET`.
- The release APK does declare sensitive permissions for SMS/MMS, contacts,
  phone state, media, audio recording, notifications, and local storage.
- Billing is currently stubbed in `BillingManagerImpl`: no products are exposed
  and purchase flow is a no-op. Treat the first Play release as free with no
  in-app purchases.
- Legacy external blocking integrations still exist for Call Control, Call
  Blocker, and Should I Answer. If these remain visible in the Play build, the
  privacy policy and Data Safety answers need to acknowledge optional transfer
  of phone-number data to those user-selected apps.
- The repo's current root `PRIVACY` file is not acceptable for TextBlock's Play
  submission. It names the old Quik package and does not disclose the sensitive
  data TextBlock accesses.

## Recommended Answers By Screenshot Item

### 1. Set Privacy Policy

Recommended answer:

- Provide a public, non-PDF privacy policy URL.
- Add the same privacy policy link or text inside the app before wider release.
- Do not use the current root `PRIVACY` file as-is.

Privacy policy must cover:

- TextBlock is a default SMS/MMS app.
- TextBlock accesses SMS/MMS message contents, senders, recipients, timestamps,
  and conversation metadata to provide messaging, quarantine, blocking, and
  local spam filtering.
- TextBlock accesses contacts to show names, avatars, and recipient choices.
- TextBlock accesses phone/SIM state to support SMS/MMS sending and receiving.
- TextBlock accesses media/files when the user attaches or views MMS content,
  imports/exports backup files, or records/sends audio messages.
- TextBlock uses microphone access only for user-initiated voice input or audio
  message features.
- TextBlock's political/spam filtering runs locally on the device.
- The current Play build does not upload conversations, contacts, filter
  decisions, analytics, or crash reports to TextBlock servers.
- SMS/MMS messages are transmitted through the user's mobile carrier and
  recipients when the user sends messages.
- If optional external blocking managers are enabled, phone-number data may be
  passed to the installed third-party blocking app chosen by the user.
- Local data can be removed by deleting conversations, deleting backups, clearing
  app data, or uninstalling the app.
- Include the developer name shown on the Play listing and a privacy contact
  email.

Suggested Console value:

```text
Use the final public TextBlock privacy policy URL.
```

Blocker:

- We still need to write and host the final privacy policy. A GitHub Pages page,
  project website page, or other public HTML page is fine.

### 2. Sign In Details

Recommended answer:

```text
TextBlock does not require sign-in or an account.

Review instructions:
1. Install TextBlock on an Android device or emulator with SMS capability.
2. Open TextBlock.
3. When prompted, set TextBlock as the default SMS app.
4. Grant the requested SMS, contacts, notification, media, and microphone
   permissions only as needed for the tested feature.
5. Send and receive test SMS/MMS messages to review inbox, conversation,
   sending, receiving, quarantine, blocking, and local filtering behavior.

No username, password, one-time code, QR code, or test account is required.
```

Notes:

- The restricted access is Android's default SMS role, not account login.
- If Play asks whether all functionality is accessible, answer yes after the app
  is made default SMS handler.

### 3. Ads

Recommended answer:

```text
No, this app does not contain ads.
```

Rationale:

- The current build has no ad SDK and no ad placements.
- Google says the ads declaration covers display, native, banner, and third-party
  ad SDK ads. Optional future donations or in-app purchases are not an ad label
  issue, but they do trigger payments/monetization considerations.

Cleanup before public release:

- Remove or rewrite any stale Quik "remove ads" / Plus copy if it is visible in
  the UI. Current billing code is stubbed, but stale text can confuse reviewers.

### 4. Content Rating

Recommended questionnaire stance:

- App type: app, not game.
- Category, if asked: communication, utility, or other non-game category.
- Violence, blood, sexual content, nudity, gambling, drugs, alcohol, tobacco,
  horror, or regulated goods: no app-provided content.
- Profanity: no app-provided profanity. Private user messages may contain
  arbitrary content; answer accurately if the questionnaire asks about user
  generated/private messaging.
- User-generated content / messaging: if asked whether users can exchange
  messages, answer yes. Clarify that it is private SMS/MMS, not a public social
  feed.
- Purchases: no for the first release.
- Location sharing: no core location-sharing feature.
- Web browser or unrestricted internet: no.

Expected outcome:

- Likely a low/general rating, but let IARC assign the rating from accurate
  answers. Do not tune answers to force a specific rating.

### 5. Target Audience

Recommended answer:

```text
18 and over only.
```

Supporting answers:

- Not designed for children.
- Store listing is not intended to appeal to children.
- No child-directed content.
- No ads.
- Do not select ages under 18 unless you are prepared to handle Families policy
  requirements for an SMS app with sensitive permissions.

Rationale:

- TextBlock handles private SMS/MMS, contacts, phone numbers, and media.
- Targeting adults only keeps the first launch simpler and avoids unnecessary
  child-directed policy obligations.

### 6. Data Safety

Important distinction:

- In Google's Data Safety form, "collect" means transmitting user data off the
  device. Purely local processing does not need to be declared as collected.
- "Sharing" can include on-device transfer to another app, unless it falls under
  a user-initiated or properly disclosed/consented transfer.

Recommended first-release strategy:

1. If possible, disable or hide legacy external blocking managers before the
   first Play upload.
2. Then answer that TextBlock does not collect or share user data with the
   developer or other companies, while the privacy policy still discloses local
   access to sensitive data.

If legacy external blocking managers remain visible:

- Do not blindly answer "no sharing."
- Declare optional sharing for phone-number data if the form requires it, with
  purpose `App functionality`, because a user-selected external blocking manager
  can receive a sender/number to check or block it.
- Mention in the privacy policy that this only happens when the user selects an
  installed third-party blocking manager.

Recommended answers for the current no-network build if external blocking
managers are disabled or treated as user-initiated/disclosed:

```text
Does your app collect or share any required user data types?
No.

Privacy policy:
Provide the TextBlock privacy policy URL.
```

If Play asks for data handling despite the "no collection/share" answer, use
this mapping for local access disclosures and future policy text:

| Data type | Current use | Collected by TextBlock server? | Shared? |
| --- | --- | --- | --- |
| SMS or MMS | Send, receive, display, quarantine, filter, block | No | Carrier/recipient only when user sends; external blocker only if enabled |
| Contacts | Names, avatars, recipient picker | No | No by default |
| Phone number / phone state | SMS/MMS routing, SIM/subscription handling | No | External blocker only if enabled |
| Photos/videos/audio/files | User-selected MMS attachments, backups, recorded audio messages | No | Carrier/recipient only when user sends |
| Voice/audio | User-initiated speech input or audio message recording | No | System speech service or recipient only when user initiates |
| App activity/settings/filter rules | Local preferences, quarantine, corrections | No | No |
| Crash logs/diagnostics | No active crash-reporting SDK in current release | No | No |
| Device or other IDs | No analytics/ad ID collection in current release | No | No |

Encryption in transit:

- If the form says no collection/share, this should not apply.
- Do not claim TextBlock end-to-end encrypts carrier SMS/MMS. SMS/MMS transport
  is controlled by the carrier path, not TextBlock.

Deletion:

- No TextBlock server account exists.
- Users can delete conversations, filtered/quarantined texts, backups, and local
  app data. Uninstalling or clearing app data removes local TextBlock data.

### 7. Government Apps

Recommended answer:

```text
No.
```

Rationale:

- TextBlock is not developed by or for a government.
- It does not claim to provide official government services or official
  government information.
- Filtering political campaign texts does not make it a government app.

### 8. Financial Features

Recommended answer:

```text
My app doesn't provide any financial features.
```

Rationale:

- TextBlock does not offer banking, loans, payments, money transfer, crypto,
  trading, insurance, rewards, or financial advice.
- Optional developer donations should be omitted from the first release.
- If donations are added later, use Google Play Billing and update this plan if
  Play's financial or payments questions change.

### 9. Health

Recommended answer:

```text
My app doesn't provide any health features.
```

Rationale:

- TextBlock does not provide medical, health, fitness, clinical, research, or
  health-data features.

### 10. Select App Category And Provide Contact Details

Recommended values:

- App or game: `App`
- Category: `Communication` / `Communications`
- Tags: choose up to five only if Play offers accurate tags. Prefer tags related
  to SMS, messaging, privacy, spam blocking, or communication. Do not use
  politics, news, or finance tags.
- Support email: use a public support email you are comfortable showing on the
  Play listing.
- Website: recommended, especially if it hosts the privacy policy.
- Phone: optional; leave blank unless you want it public.

Rationale:

- Google's category examples place messaging, chat, dialers, address books,
  browsers, and call management under Communications.

### 11. Set Up Your Store Listing

Recommended app name:

```text
TextBlock
```

Recommended short description:

```text
Private SMS with local political spam filtering.
```

Recommended full description:

```text
TextBlock is a free SMS/MMS app with local-first spam filtering.

Use it as your default texting app to send, receive, and organize SMS/MMS
conversations. TextBlock can route likely political campaign spam and similar
unwanted messages out of your main inbox before they notify you, while keeping
filtered messages reviewable.

Message filtering runs on your device. TextBlock does not upload your
conversations, contacts, or filter decisions to a TextBlock server.

Key features:
- Full SMS/MMS inbox and conversation view
- Local political spam filtering
- Quarantine/review flow for filtered texts
- Block similar texts from inbox or conversation menus
- Message reactions and normal text selection
- Customizable notification and contact color behavior

TextBlock must be set as your default SMS app for SMS/MMS receiving and sending.
```

Store listing asset requirements:

- App icon: 512 x 512, 32-bit PNG with alpha, max 1024 KB.
- Feature graphic: required for the store listing.
- Screenshots: at least two across supported device types; recommended at least
  four phone screenshots at 1080 px or higher.
- Screenshots should show actual in-app UI using synthetic conversations only.
- Do not use real phone numbers, real contact names, real political texts, or
  notification-bar details from your personal phone.

Suggested screenshot set:

1. Inbox with synthetic threads.
2. Conversation with synthetic messages and reaction affordance.
3. Quarantine/filtered messages screen.
4. Filter settings or "Block similar texts" flow.

## SMS Permissions Declaration

This may appear separately because the manifest requests restricted SMS/MMS
permissions.

Recommended use case:

```text
Default SMS handler
```

Recommended explanation:

```text
TextBlock is a full SMS/MMS client. Users set TextBlock as their default SMS app
to receive, send, display, organize, and manage SMS/MMS conversations. SMS/MMS
permissions are required for TextBlock's core functionality: receiving incoming
texts, sending outgoing texts, reading existing conversation history, displaying
message threads, supporting MMS attachments, blocking unwanted senders, and
locally filtering likely political spam into a reviewable quarantine.

TextBlock does not use SMS/MMS data for advertising or marketing. Filtering runs
locally on the device and the current release does not upload message contents
or contacts to TextBlock servers.
```

Reviewer caveat:

- Do not describe TextBlock as only a background SMS scanner. Google's SMS
  policy is much easier to satisfy when the app is clearly a default SMS handler
  and can send/receive SMS as core functionality.

## Implementation Follow-Ups

Before closed/open/production review:

- Replace the stale root `PRIVACY` file or create a hosted privacy policy page.
- Add an in-app privacy policy link.
- Decide whether to remove/hide legacy external blocking managers for the first
  Play release or disclose optional sharing with those third-party apps.
- Remove or rewrite any visible Quik Plus/remove-ads text.
- Prepare synthetic screenshots and feature graphic.
- Keep the app free and skip donations in the first submission.

## Official Sources

- Prepare app for review: https://support.google.com/googleplay/android-developer/answer/9859455
- Sign-in details: https://support.google.com/googleplay/android-developer/answer/15748846
- User Data and privacy policy requirements: https://support.google.com/googleplay/android-developer/answer/10144311
- Data Safety form: https://support.google.com/googleplay/android-developer/answer/10787469
- SMS/Call Log permissions: https://support.google.com/googleplay/android-developer/answer/10208820
- Android default SMS handler requirements: https://developer.android.com/guide/topics/permissions/default-handlers
- Content ratings: https://support.google.com/googleplay/android-developer/answer/9898843
- Target audience: https://support.google.com/googleplay/android-developer/answer/9867159
- Government apps: https://support.google.com/googleplay/android-developer/answer/9514050
- Financial features declaration: https://support.google.com/googleplay/android-developer/answer/13849271
- Health apps declaration: https://support.google.com/googleplay/android-developer/answer/14738291
- Create and set up your app: https://support.google.com/googleplay/android-developer/answer/9859152
- Category and tags: https://support.google.com/googleplay/android-developer/answer/9859673
- Preview assets: https://support.google.com/googleplay/android-developer/answer/9866151
