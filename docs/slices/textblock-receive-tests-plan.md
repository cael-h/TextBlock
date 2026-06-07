# Slice L: Receive-Path Test Seams

## Goal

Add focused unit-test coverage for TextBlock receive-path decisions without constructing Android `Worker` instances. Keep production behavior unchanged while making SMS/MMS branch outcomes testable through small pure helpers.

## Scope

- Extract a domain-level helper for receive TextBlock decisions used by SMS and MMS workers.
- Cover filtering disabled, contact allowlist, quarantine mode, and drop mode.
- Cover MMS post-persistence protocol preservation at helper level so TextBlock suppression affects user notification only and does not imply skipping MMS ACK/notify responses.
- Wire workers to the helper only where needed for the tested seam.

## Out Of Scope

- UI or blocking-message presentation changes.
- Classifier implementation, classifier semantics, or correction policy changes.
- Durable correction storage.
- Gradle/plugin upgrades.
- Device or emulator testing.

## Test Strategy

- Add domain unit tests under `domain/src/test/java/dev/octoshrimpy/quik/textblock/`.
- Test that disabled filtering and contact allowlist return an allow decision without invoking classification.
- Test quarantine mode maps suppressing classifier results to a quarantine receive effect.
- Test drop mode maps suppressing classifier results to a delete/drop receive effect.
- Test allowed classifier results remain allowed in drop mode.
- Test MMS post-persistence planning keeps `sendAcknowledgeInd` and `sendNotifyRespInd` enabled even when TextBlock quarantine/drop suppresses user notification.

## Implementation Checklist

- [x] Add pure receive decision helper in the TextBlock domain package.
- [x] Add focused helper tests for SMS/MMS receive decisions.
- [x] Rewire `ReceiveSmsWorker` to use the helper for TextBlock decision handling.
- [x] Rewire `ReceiveMmsWorker` to use the helper and MMS post-persistence plan without adding early returns.
- [x] Run focused domain tests.
- [x] Run `:data:compileDebugKotlin` after touching worker files.
- [x] Run `git diff --check`.
- [x] Commit the slice on `textblock-receive-tests`.

## Risks

- Worker dependencies are Android-heavy, so direct worker unit tests would be brittle in this slice.
- The helper must stay policy-only and avoid changing filtering or blocking behavior.
- MMS ACK/notify behavior is sensitive to control flow; the worker should continue to send responses after successful persistence even when TextBlock suppresses notification.

## Status

- Plan created.
- Added `TextBlockReceivePolicy` helper and focused domain tests for disabled filtering, contact allowlist, quarantine/drop effects, allowed results, and MMS ACK/notify preservation planning.
- Wired SMS and MMS receive workers to the helper while preserving existing repository side effects and MMS response sending after successful persistence.
- Focused domain tests passed:
  `ANDROID_HOME=/data/data/com.termux/files/home/android-sdk-termux ./gradlew :domain:testDebugUnitTest --tests 'dev.octoshrimpy.quik.textblock.*' -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2`
- Data Kotlin compile passed:
  `ANDROID_HOME=/data/data/com.termux/files/home/android-sdk-termux ./gradlew :data:compileDebugKotlin -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2`
- `git diff --check` passed.
- Commit created for this slice.
