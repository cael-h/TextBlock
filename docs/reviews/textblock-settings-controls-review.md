# TextBlock Settings Controls Review

Review target: `/data/data/com.termux/files/home/projects/TextBlock/quik-textblock-settings-controls` on branch `textblock-settings-controls`, reviewed against base commit `645cd228`.

## Findings

No findings.

## Open Questions / Assumptions

- I treated the worker's recorded SDK failure in `docs/slices/textblock-settings-controls-plan.md:90-91` as stale. With the current Termux SDK shim, the focused domain test and targeted `data`/`presentation` Kotlin compile both pass.
- I assumed the intended quarantine implementation is still QUIK's existing blocked-conversation path, because the slice marks TextBlock suppressions as read and blocked in `ReceiveSmsWorker.kt:126-134` and `ReceiveMmsWorker.kt:229-238`.

## Test Gaps / Residual Risk

- There are no worker-level tests that mock `Preferences`, `InboundMessageClassifier`, and the repositories to prove disabled filtering skips classification, contact allowlisting skips classification, quarantine blocks, and drop deletes on both SMS and MMS.
- The MMS ACK/notify response behavior was verified by code inspection and compile, not by an executable test around `sendAcknowledgeInd` / `sendNotifyRespInd`.
- Settings UI behavior was compile-checked, but not exercised in an instrumentation or presenter test.

## Verification

- `git diff --check 645cd228` passed for the slice.
- `ANDROID_HOME=/data/data/com.termux/files/home/android-sdk-termux ./gradlew :domain:testDebugUnitTest --tests 'dev.octoshrimpy.quik.textblock.TextBlockFilterPolicyTest' -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2` passed.
- `ANDROID_HOME=/data/data/com.termux/files/home/android-sdk-termux ./gradlew :data:compileDebugKotlin :presentation:compileDebugKotlin -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2` passed. Gradle emitted existing resource/Kotlin warnings; no new compile failure was found.

## Behavior Review Notes

- Defaults match the requirement: filtering on, quarantine mode, and allow contacts are set in `Preferences.kt:144-146`.
- The pure policy skips classification when filtering is disabled and when contacts are allowed in `TextBlockFilterPolicy.kt:15-28`; drop mode maps suppressing classifier actions to `DROP` in `TextBlockFilterPolicy.kt:30-42`.
- SMS honors the policy before notification creation in `ReceiveSmsWorker.kt:109-143`; drop deletes the message at `ReceiveSmsWorker.kt:137-140`.
- MMS keeps the persisted-message branch alive through ACK and notify response at `ReceiveMmsWorker.kt:280-284`; TextBlock quarantine/drop only flips `shouldNotify` or deletes inside the branch at `ReceiveMmsWorker.kt:212-245`.

## Summary

The slice implements the requested defaults and receive-path behavior, and the touched data/presentation code compiles under the current local SDK shim. The main remaining risk is missing executable coverage for worker branching and MMS carrier response behavior.
