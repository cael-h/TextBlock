# TextBlock Settings Controls Plan

## Goal

Add user-facing controls for local TextBlock political filtering and make SMS/MMS receive workers honor them.

Controls:

- Enable or disable TextBlock filtering.
- Choose quarantine or drop mode, with quarantine as the safe default.
- Allow messages from contacts, with allow contacts as the safe default.

Default behavior should stay equivalent to the current TextBlock behavior, except that contacts are allowed when the new allow-contacts preference is enabled.

## File Boundaries

- `domain/src/main/java/com/moez/QKSMS/util/Preferences.kt`
  - Add preference constants for TextBlock filtering mode.
  - Add Rx preference entries for enable, mode, and allow contacts.
- `domain/src/main/java/com/moez/QKSMS/textblock/*`
  - Add a small pure policy helper so preference-driven behavior can be tested without Android SDK.
- `domain/src/test/java/dev/octoshrimpy/quik/textblock/*`
  - Add focused tests for the preference policy.
- `presentation/src/main/java/com/moez/QKSMS/feature/settings/SettingsState.kt`
  - Add state for TextBlock enabled, filter-mode summary/id, and allow-contacts enabled.
- `presentation/src/main/java/com/moez/QKSMS/feature/settings/SettingsPresenter.kt`
  - Observe the new preferences.
  - Toggle boolean preferences from settings clicks.
  - Persist selected mode from the mode dialog.
- `presentation/src/main/java/com/moez/QKSMS/feature/settings/SettingsController.kt`
  - Add a `QkDialog` for TextBlock mode.
  - Bind and render the new rows.
- `presentation/src/main/java/com/moez/QKSMS/feature/settings/SettingsView.kt`
  - Add the mode selection stream and dialog method.
- `presentation/src/main/res/layout/settings_controller.xml`
  - Add a TextBlock settings category with three preference rows.
- `presentation/src/main/res/values/strings.xml`
  - Add strings and arrays for TextBlock settings.
- `data/src/main/java/com/moez/QKSMS/worker/ReceiveSmsWorker.kt`
  - Skip TextBlock classification when disabled.
  - Skip TextBlock classification for contacts when allow-contacts is true.
  - Delete instead of quarantine when drop mode is selected.
- `data/src/main/java/com/moez/QKSMS/worker/ReceiveMmsWorker.kt`
  - Same preference behavior as SMS.
  - Avoid early returns after persisted MMS before `sendAcknowledgeInd` and `sendNotifyRespInd`.

## Implementation Checklist

- [x] Add this plan before code changes.
- [x] Add TextBlock preference constants and stored preferences.
- [x] Add pure TextBlock preference policy and focused unit tests.
- [x] Add settings state, presenter, view, controller, layout, and strings.
- [x] Update SMS receive worker to honor TextBlock settings.
- [x] Update MMS receive worker to honor TextBlock settings without skipping carrier response PDUs.
- [x] Run static checks and available tests.
- [x] Update this checklist and document any build blockers.

## Behavior Details

- Filtering disabled:
  - Do not call `InboundMessageClassifier`.
  - Continue the existing non-TextBlock receive path.
- Allow contacts enabled:
  - If the sender is a contact, do not call `InboundMessageClassifier`.
  - This is the only intentional default behavior change.
- Allow contacts disabled:
  - Classify contacts as non-contact senders so the classifier does not apply its contact leniency.
- Quarantine mode:
  - For suppressing classifier results, mark the thread read and mark the conversation blocked with the existing TextBlock block reason.
  - This matches current TextBlock behavior.
- Drop mode:
  - For suppressing classifier results, delete the persisted message and do not notify.
- MMS:
  - For persisted MMS, all suppress/no-notify branches should complete normally inside the persisted-message block so the ACK and notify response are still sent.

## Tests And Static Verification

- Add domain JVM unit tests for the pure policy:
  - disabled filtering allows without classification.
  - allow-contacts enabled allows contacts without classification.
  - allow-contacts disabled does not exempt contacts.
  - quarantine mode chooses quarantine for suppressing results.
  - drop mode chooses drop for suppressing results.
- Run `./gradlew :domain:testDebugUnitTest` if the Android SDK is available.
- Run `git diff --check`.
- If Gradle cannot run because SDK or build tooling is unavailable, record the blocker in this file and final response.

Verification result:

- `git diff --check` passed.
- `./gradlew :domain:testDebugUnitTest --no-daemon` could not run because the Android SDK location is not configured. Gradle reported: `SDK location not found. Define a valid SDK location with an ANDROID_HOME environment variable or by setting the sdk.dir path in your project's local properties file`.
- Static MMS check: after initial input validation, `ReceiveMmsWorker` no longer returns from the persisted-message branch before `sendAcknowledgeInd` and `sendNotifyRespInd`.

## Risks

- Settings rows depend on generated view binding names, so XML ids must be stable and compile-safe.
- Existing classifier has contact leniency; workers must pass `isFromContact=false` when allow-contacts is disabled.
- MMS worker has existing early returns in notification suppression paths. New TextBlock suppression logic must avoid adding early returns before ACK/notify response calls.
- Drop mode deletes persisted records and should not store raw message samples in docs, logs, or tests.
