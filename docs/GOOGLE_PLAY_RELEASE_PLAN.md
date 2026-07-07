# Google Play Release Plan

## Purpose

TextBlock needs a Google Play presence before the developer account risks
inactivity closure. The first Play release does not need to be public. The
near-term goal is to get a compliant build uploaded to an internal or closed
testing track, then tighten policy docs, QA, and optional monetization before
any broader launch.

TextBlock will be listed as a free, local-first SMS/MMS app with political spam
filtering. Optional developer support should be added only through Google Play
Billing or left out of the first submission.

## Current Repo State

- Branch: `textblock-filter-foundation`
- Package/application id: `com.caelh.textblock`
- Version: `4.3.7`
- Version code: `2239`
- Current target SDK: `35`
- Current compile SDK: `35`
- Release APK and Android App Bundle build locally.
- Device install remains blocked until an ADB target is attached or wireless ADB
  is enabled.

## Official Requirements Checked

- Inactive developer account prevention:
  https://support.google.com/googleplay/android-developer/answer/11605267
- Target API requirements:
  https://support.google.com/googleplay/android-developer/answer/11926878
- Android App Bundle requirement:
  https://support.google.com/googleplay/android-developer/answer/9844279
- Internal, closed, and open testing tracks:
  https://support.google.com/googleplay/android-developer/answer/9845334
- New personal account testing requirements:
  https://support.google.com/googleplay/android-developer/answer/14151465
- SMS and Call Log permission policy:
  https://support.google.com/googleplay/android-developer/answer/10208820
- App review / App content requirements:
  https://support.google.com/googleplay/android-developer/answer/9859455
- User data policy:
  https://support.google.com/googleplay/android-developer/answer/10144311
- Data safety section:
  https://support.google.com/googleplay/android-developer/answer/10787469
- Store listing preview assets:
  https://support.google.com/googleplay/android-developer/answer/9866151
- Google Play payments policy:
  https://support.google.com/googleplay/android-developer/answer/10281818

## Hard Blockers Before Upload

### 1. Target SDK

Google Play currently requires new phone apps and updates to target Android 15,
API level 35, or higher. TextBlock currently targets API 33, so this must be
updated before Play accepts a new submission.

Implementation tasks:

- Install Android platform 35 and compatible build tools into the Termux SDK
  shim.
- Update `presentation/build.gradle` from `compileSdk 34` /
  `targetSdkVersion 33` to API 35.
- Build and fix any compile/runtime issues caused by the target SDK bump.
- Smoke-test SMS receive, send, notification suppression, default SMS setup,
  quarantine, "Block Similar," and reaction UI on device.

### 2. Android App Bundle

New Play apps must be published with an Android App Bundle, not only an APK.

Implementation tasks:

- Build a signed release bundle with `:presentation:bundleRelease`.
- Verify the bundle is signed.
- Keep producing side-loadable release APKs for our own phone, but use the AAB
  for Play.

Expected local command shape:

```sh
export ANDROID_HOME=/data/data/com.termux/files/home/android-sdk-termux
./gradlew --no-daemon :presentation:bundleRelease \
  -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2
```

### 3. SMS Permission Review

TextBlock requests high-risk SMS/MMS permissions. This is legitimate only if
the app is clearly a default SMS handler and SMS/MMS handling is core
functionality.

Implementation tasks:

- Audit every manifest permission and remove anything unnecessary.
- Confirm default SMS handler declarations remain correct.
- Prepare the Play Console Permissions Declaration Form.
- In the declaration, describe TextBlock as a full SMS/MMS client whose core
  function is receiving, sending, displaying, and locally filtering SMS/MMS.
- Avoid wording that makes it sound like a background-only scanner, because that
  is much harder to defend under the SMS policy.

High-risk permissions to audit:

- `READ_SMS`
- `RECEIVE_SMS`
- `RECEIVE_MMS`
- `SEND_SMS`
- `READ_CONTACTS`
- `READ_PHONE_STATE`
- `CALL_PHONE`
- `POST_NOTIFICATIONS`
- `SCHEDULE_EXACT_ALARM`
- legacy media/storage permissions

## Console Setup Checklist

### Developer Account

- Verify Play Console account email and phone number.
- Confirm account type: personal or organization.
- If this is a personal developer account created after November 13, 2023,
  expect the production gate: closed testing with at least 12 opted-in testers
  for 14 continuous days before production access.

### App Creation

- Create app in Play Console.
- App name: `TextBlock`.
- Default language: English (United States), unless the account default differs.
- App type: app.
- Pricing: free.
- Category: Communication.
- Distribution: start with internal testing or closed testing.

### Release Track Strategy

Recommended path:

1. Internal testing first, because it is fastest and can be used before full
   app setup is finished.
2. Closed testing next, especially if the account is subject to the 12 tester /
   14 day production requirement.
3. Production only after SMS permission approval, privacy policy, screenshots,
   and real-world testing are stable.

If the account closure deadline is very close, upload to internal testing as
soon as the API 35 AAB builds and the minimum app content forms are accepted.

## App Content Requirements

### Privacy Policy

Required because TextBlock handles SMS, MMS, contacts, phone state, and other
sensitive user data.

Implementation tasks:

- Create a public privacy policy URL.
- Add an in-app privacy policy link, probably in Settings or About.
- State clearly that message filtering is local on device.
- State whether SMS/MMS/contact data is collected, transmitted, shared, sold, or
  retained.
- State that no raw message contents are sent to TextBlock servers if we keep
  the current no-network design.
- Mention optional future on-device model behavior only after it exists.

### Data Safety Form

Prepare answers for:

- SMS and MMS message content.
- Contacts and phone numbers.
- App activity/settings/preferences.
- Crash diagnostics, if any SDK collects them.
- Whether data is encrypted in transit. Current app has no internet permission,
  so the answer should reflect that no app data is transmitted unless this
  changes.
- Whether users can request data deletion. If no server-side data exists, say no
  server data is retained and local data can be deleted in-app or by uninstall.

### Other App Content Forms

- Ads: no.
- Content rating: complete questionnaire.
- Target audience: adults/general users, not directed to children.
- News app: no.
- Government app: no.
- Health/COVID: no.
- Financial features: no.
- Restricted access instructions: explain that reviewers must set TextBlock as
  the default SMS app to test SMS/MMS behavior.

## Store Listing Assets

Required or strongly recommended:

- 512x512 app icon, 32-bit PNG with alpha, under 1024 KB.
- Feature graphic, 1024x500 JPEG or 24-bit PNG without alpha.
- At least 2 phone screenshots; prefer 4 or more.
- Short description, max 80 characters.
- Full description.
- Support email.
- Privacy policy URL.

Screenshot guidance:

- Use synthetic conversations only.
- Show inbox, conversation, TextBlock filtering controls, and quarantine/review.
- Avoid real phone numbers, real names, real political messages, and raw user
  screenshots.

Draft short description:

```text
Private SMS with local political spam filtering.
```

Draft positioning:

```text
TextBlock is a free SMS/MMS app with local-first spam filtering. It helps route
political campaign texts and similar unwanted messages out of your main inbox
without uploading your conversations to a server.
```

## Optional Developer Donation

Do not add an external PayPal, Venmo, Patreon, Ko-fi, or web donation link in
the first Play version. Google Play's payments policy can require Google Play
Billing for in-app digital payments and donation-like support flows.

Recommended options:

- Initial release: skip donations entirely.
- Later release: add Google Play Billing with a one-time "Support TextBlock
  development" product.
- Keep all filtering features free; donation should not unlock core SMS or spam
  filtering behavior.

Implementation tasks for later donation support:

- Add current Google Play Billing Library.
- Create Play Console in-app product IDs.
- Add a small support screen.
- Add tests for purchase state handling where practical.
- Update privacy policy and Data safety answers if any billing-related SDK data
  handling changes.

## Implementation Plan

### Phase 0: Account Preservation

Owner: user plus manager support

- Verify Play Console email and phone.
- Create the TextBlock app record.
- Create internal tester list.
- Start privacy policy hosting decision.
- Confirm whether the account is personal and when it was created.

Exit criteria:

- App exists in Play Console.
- Internal testing track is available.
- We know whether the 12 tester / 14 day production gate applies.

### Phase 1: Build Compliance

Owner: engineering

- Install API 35 SDK platform/build tools.
- Update Gradle SDK targets.
- Build release AAB.
- Run focused unit tests.
- Run lint.
- Install a generated APK on the phone and smoke-test core flows.

Exit criteria:

- Signed release AAB exists.
- Side-loadable release APK still works.
- No target SDK blocker remains.

### Phase 2: Permission And Privacy Compliance

Owner: engineering plus user review

- Audit manifest permissions.
- Remove or justify each sensitive permission.
- Add in-app privacy policy link if missing.
- Draft privacy policy.
- Draft SMS permission declaration text.
- Draft Data safety answers.

Exit criteria:

- App content forms can be completed without guessing.
- SMS permission justification matches actual app behavior.

### Phase 3: Store Listing Package

Owner: engineering/design

- Generate Play icon asset from existing TextBlock icon.
- Produce feature graphic.
- Capture synthetic screenshots.
- Write short and full descriptions.
- Prepare release notes.

Exit criteria:

- Play listing can pass required asset fields.
- Listing accurately describes local SMS/MMS filtering.

### Phase 4: Internal Test Upload

Owner: user in Play Console, engineering supplying artifacts/text

- Upload signed AAB.
- Complete required app content forms.
- Submit internal testing release for review.
- Share opt-in link with internal testers after available.

Exit criteria:

- TextBlock has a Play Console release in internal testing.
- Account inactivity risk is reduced according to Google's account guidance.

### Phase 5: Closed Test And Production Readiness

Owner: user plus engineering support

- Move to closed testing if production access is gated.
- Keep at least 12 testers opted in for 14 continuous days if required.
- Monitor pre-launch report, crashes, ANRs, policy warnings, and tester feedback.
- Fix critical issues and increment version code for each Play upload.

Exit criteria:

- Closed testing requirement is complete if applicable.
- App is eligible to request production access.

### Phase 6: Donation Support

Owner: engineering

- Decide whether donation belongs in the first public release.
- If yes, implement it through Play Billing.
- If no, keep first public release free with no monetization.

Exit criteria:

- Play listing and app behavior match the declared monetization model.

## Parallel Slices

These can mostly run in parallel after Phase 0:

- Build compliance: SDK 35, Gradle target bump, AAB generation.
- Privacy and policy docs: privacy policy, Data safety, SMS declaration draft.
- Store assets: icon, feature graphic, screenshots, descriptions.
- QA checklist: default SMS setup, receive/send, filter suppression, quarantine,
  block similar, reactions, restore/uninstall behavior.

Avoid parallel edits to:

- `presentation/build.gradle`, because target SDK, version code, and release
  signing changes share the same file.
- `AndroidManifest.xml`, because permission cleanup and policy-sensitive default
  SMS handler declarations need one coordinated owner.

## Risk Register

- Target SDK upgrade may reveal Android 14/15 behavior changes in SMS,
  notification, pending intent, background worker, or broadcast handling.
- SMS permission approval is the largest policy risk.
- Incomplete privacy policy or Data safety answers can block review.
- External donation links can create payment policy risk.
- Real user screenshots or real message text in listing assets would be a
  privacy problem.
- If the Play app signing upload key differs from our current release key, we
  must document and back up the chosen key before upload.

## Immediate Next Step

Phase 1 build compliance is complete except device install smoke testing. The
next engineering task is to audit sensitive manifest permissions and prepare the
privacy / SMS permission declaration material while the user creates the Play
Console app entry and verifies account contact details.
