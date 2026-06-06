# Dev Manager Active Plan

## Introduction

TextBlock is a fork of QUIK SMS that will become its own Android SMS/MMS app.
Its first product goal is to filter political campaign spam locally on-device,
before user-facing notification, while keeping messages reviewable instead of
deleted by default.

The current foundation is complete:

- GitHub fork: `https://github.com/cael-h/TextBlock`
- Local checkout: `/data/data/com.termux/files/home/projects/TextBlock/quik-textblock`
- `origin`: TextBlock fork
- `upstream`: `https://github.com/quik-sms/quik.git`
- Upstream push is disabled locally.
- Initial classifier boundary and rule-based political classifier exist.

## Completed Milestone Batch 1

- Establish build readiness for Android work in Termux.
- Separate TextBlock identity from QUIK so both can coexist if needed.
- Prepare the inbound SMS/MMS filter integration.
- Add classifier tests without committing raw user text samples.
- Keep documentation current and reviewable.

## Current Milestone Batch 2

Purpose:

Move TextBlock from a hard-wired rule filter toward a user-controllable local
spam system. This batch focuses on the remaining development-plan work:
settings and corrections, a future on-device LLM adapter seam, build
verification, and current documentation.

Main goals:

- [x] Add TextBlock settings for enabling/disabling filtering, quarantine/drop
  behavior, and contact allowlist behavior.
- [x] Add a local correction-data boundary for "Not spam" and "Block similar"
  decisions without storing raw personal samples.
- [x] Add an on-device LLM adapter seam behind `InboundMessageClassifier`, with a
  no-network/no-model fallback.
- [x] Improve Android build setup in this Termux environment enough to determine
  whether full compile/test verification is possible locally.
- [x] Update docs so milestone status reflects the code that now exists.

## Current Status

- Branch: `textblock-filter-foundation`
- Local integration head: batch 2 merged locally; pending final verification and push
- Latest pushed commit before batch 2: `645cd228 Mark dev manager milestone pushed`
- Worktree support: available
- Java status: OpenJDK 17.0.19 installed and `./gradlew --version` works.
- Android SDK status: local Termux shim at `/data/data/com.termux/files/home/android-sdk-termux` supports task discovery, targeted domain unit tests, and targeted data/presentation Kotlin compile when `ANDROID_HOME` is set and `-Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2` is supplied. Full APK assembly/lint remains unverified.
- Upstream status: `upstream/master` has no new commits relative to the current base; local branch is ahead only with TextBlock work.
- Automation status: no scheduler/automation tool is available in this session, so 20-minute checks will be manual.
- Multi-agent status: subagent tools are available.

## Parallel Slice Plan

### Slice A: Build Readiness

Owner: manager/local critical path

Status: completed with Android SDK blocker documented

Goal:

- Determine and document the minimum Termux build setup.
- Install/configure Java if possible.
- Run `./gradlew --version`, then a minimal Gradle task if available.

Likely files:

- `docs/DEV_MANAGER_ACTIVE_PLAN.md`
- `docs/DEVELOPMENT_PLAN.md`

Dependencies:

- None.

Risk:

- Android Gradle builds may require SDK packages that are not installed.

Result:

- Installed Java 17 and confirmed Gradle wrapper startup.
- `./gradlew tasks --all` failed before task execution because the Android SDK location is not configured.
- Full compile/test verification remains blocked until `ANDROID_HOME` or `local.properties` `sdk.dir` points at a valid Android SDK.

### Slice B: App Identity Separation

Owner: worker

Status: merged

Goal:

- Produce a concrete change plan, then implement TextBlock app identity changes
  if scope is clear.
- Ensure TextBlock does not install as QUIK.

Likely files:

- `presentation/build.gradle`
- `presentation/src/main/AndroidManifest.xml`
- app string resources under `presentation/src/main/res`
- metadata files if needed

Non-goals:

- Large UI redesign.
- Receive-path filtering.

Result:

- Merged `textblock-app-identity` into `textblock-filter-foundation`.
- Release application id is now `com.caelh.textblock`; debug and F-Droid variants use `.debug` and `.fdroid`.
- App label and focused default English identity copy now use TextBlock.
- Reviewer finding on localized French `app_name` was implemented by removing the stale localized override so locales inherit the non-translatable default label.

### Slice C: Inbound Filter Integration

Owner: worker

Status: merged

Goal:

- Inspect the SMS/MMS receive workers and dependency injection.
- Write a detailed slice plan for where the classifier should be wired.
- Implement only if the integration boundary is clear and does not conflict
  with app identity changes.

Likely files:

- `data/src/main/java/com/moez/QKSMS/worker/ReceiveSmsWorker.kt`
- `data/src/main/java/com/moez/QKSMS/worker/ReceiveMmsWorker.kt`
- injection modules

Non-goals:

- Dedicated quarantine UI.
- Settings UI.

Result:

- Merged `textblock-inbound-filter` into `textblock-filter-foundation`.
- `InboundMessageClassifier` is provided through Dagger and assigned by `InjectionWorkerFactory` to SMS/MMS receive workers.
- SMS and MMS receive paths classify after existing blocked-sender and user content-filter checks, and before normal notification work.
- TextBlock `QUARANTINE`, `BLOCK_CONVERSATION`, and `DROP` actions suppress notification by marking the thread read and marking the conversation blocked; no TextBlock path deletes messages by default.
- Reviewer finding on MMS acknowledgement was implemented: classified MMS no longer returns before `sendAcknowledgeInd(...)` / `sendNotifyRespInd(...)`.

### Slice D: Classifier Tests And Synthetic Samples

Owner: worker

Status: merged

Goal:

- Add focused tests for `RuleBasedPoliticalClassifier`.
- Use synthetic examples only.
- Cover false-positive guards.

Likely files:

- `domain/src/test/...`
- `domain/build.gradle` only if needed for test support
- docs updates if test conventions need documenting

Non-goals:

- Raw user messages.
- SMS/MMS receive integration.

Result:

- Merged `textblock-classifier-tests` into `textblock-filter-foundation`.
- Added synthetic JUnit 4 coverage for political donation spam, petition spam, Unicode normalization, MMS export noise, `/l/` link behavior, and benign allow cases.
- Reviewer found no implementation issues.

### Slice E: Review

Owner: reviewer after workers finish

Status: completed

Goal:

- Review each completed slice.
- Write findings to `docs/reviews/<slice>-review.md`.
- Original worker closes or disputes findings.

Result:

- Review docs were created under `docs/reviews/`.
- App identity review produced one medium finding; implemented by the identity worker.
- Inbound filter review produced one high finding; implemented by the inbound worker.
- Classifier tests review produced no implementation findings.

## Worker Assignments

Requested worker model profile:

- developer workers: `gpt-5.5`, high reasoning
- reviewer: `gpt-5.5`, xhigh reasoning

If a requested profile is unavailable, use the nearest available high-reasoning
agent and record the mismatch.

Batch 2 developer slices:

### Slice F: TextBlock Settings Controls

Owner: worker

Status: merged

Goal:

- Add user-facing TextBlock controls for enable/disable, quarantine/drop mode,
  and contact allowlist behavior.
- Make inbound SMS/MMS receive paths honor those preferences.

Likely files:

- `domain/src/main/java/com/moez/QKSMS/util/Preferences.kt`
- `presentation/src/main/java/com/moez/QKSMS/feature/settings/Settings*.kt`
- `presentation/src/main/res/layout/settings_controller.xml`
- `presentation/src/main/res/values/strings.xml`
- `data/src/main/java/com/moez/QKSMS/worker/ReceiveSmsWorker.kt`
- `data/src/main/java/com/moez/QKSMS/worker/ReceiveMmsWorker.kt`
- `docs/slices/textblock-settings-controls-plan.md`

Non-goals:

- Dedicated quarantine UI.
- Correction persistence.
- LLM runtime.

Result:

- Added TextBlock settings for filtering enabled, filtered-message action, and contact allowlist.
- Added `TextBlockFilterPolicy` and focused unit coverage.
- SMS/MMS receive paths honor filtering disabled, contact allowlist, quarantine mode, and drop mode.
- MMS persisted-message path still reaches carrier ACK/notify calls after TextBlock suppression handling.
- Reviewer found no implementation issues.

### Slice G: Local Correction Data Boundary

Owner: worker

Status: merged

Goal:

- Create a local, testable API for future "Not spam" and "Block similar"
  corrections.
- Keep raw message bodies out of persisted correction state.

Likely files:

- new files under `domain/src/main/java/com/moez/QKSMS/textblock/correction/`
- tests under `domain/src/test/java/dev/octoshrimpy/quik/textblock/correction/`
- `docs/slices/textblock-corrections-plan.md`

Non-goals:

- Blocking/messages UI changes.
- Receive-worker integration unless the API boundary is trivial and isolated.
- LLM runtime.

Result:

- Added typed correction actions, privacy-preserving correction keys, signal derivation, and an in-memory correction store.
- `NOT_SPAM` resolves as force-allow and has precedence over `BLOCK_SIMILAR`.
- Records persist only action, hashed key, and timestamp, not raw message bodies.
- Reviewer found no implementation issues.

### Slice H: On-Device LLM Adapter Boundary

Owner: worker

Status: merged

Goal:

- Add an adapter seam for optional on-device LLM classification while keeping
  receive workers coupled only to `InboundMessageClassifier`.
- Provide a deterministic no-model fallback and documentation for future Gemma
  integration.

Likely files:

- new files under `domain/src/main/java/com/moez/QKSMS/textblock/llm/`
- tests under `domain/src/test/java/dev/octoshrimpy/quik/textblock/llm/`
- `docs/slices/textblock-llm-adapter-plan.md`

Non-goals:

- Downloading or bundling a model.
- Network inference.
- Calling the LLM directly from receive workers.

Result:

- Added an optional on-device LLM runtime seam behind `InboundMessageClassifier`.
- Added no-model and deferred fallback behavior that returns allow-like results and cannot quarantine by surprise.
- Receive workers remain untouched by the LLM seam.
- Reviewer found no implementation issues.

### Slice I: Android Build Environment

Owner: manager/local plus worker documentation support

Status: merged

Goal:

- Determine whether this Termux/aarch64 environment can run Android Gradle
  compile/test tasks.
- Install/configure safe missing Termux build-tool packages where possible.
- Document a reproducible local or fallback desktop build path.

Likely files:

- `docs/slices/android-build-env-plan.md`
- `docs/DEVELOPMENT_PLAN.md`
- `docs/DEV_MANAGER_ACTIVE_PLAN.md`
- optional non-secret SDK setup helper under `scripts/`

Non-goals:

- Checking in `local.properties`.
- Committing SDK binaries.

Result:

- Installed Termux Android build tools and created an ignored local SDK shim with official Android 33/34 platform packages and Termux-native build tools.
- Added `scripts/check-android-build-env.sh`.
- Checker derives compile SDKs from Gradle files and validates both Android 33 and 34.
- `./gradlew tasks --all` passes with the SDK shim and Termux `aapt2` override.
- Full APK assembly and lint remain unverified.

Deferred until Slice G stabilizes:

- Dedicated quarantine/review UI and correction actions in the blocked messages
  screen. This will likely touch `presentation/src/main/java/.../feature/blocking/messages/`
  and should not run concurrently with correction API design.

## Manual Progress Check Cadence

Every 20 minutes while workers are active, collect:

```text
Slice:
Status:
Completed:
Current task:
Blockers:
Changed files:
Next 20 minutes:
Needs manager decision:
```

No automation tool is available in this session, so no scheduled check was set.

## Merge Strategy

- Keep each worker on a dedicated worktree/branch.
- Merge finished branches into `textblock-filter-foundation` only after review.
- Push each completed milestone to `origin`.
- Do not push to `upstream`.

Merge result:

- `textblock-app-identity` merged via `405b79ee`.
- `textblock-classifier-tests` merged via `8c7249eb`.
- `textblock-inbound-filter` merged via `92ad5f61`.
- `textblock-corrections` merged in batch 2.
- `textblock-llm-adapter` merged in batch 2.
- `textblock-settings-controls` merged in batch 2.
- `textblock-build-env` merged in batch 2.

## Risk Register

- Build tooling may be incomplete in Termux.
- App identity changes can touch broad resource/manifest surfaces.
- Inbound filtering can accidentally delete or hide wanted messages if wired too aggressively.
- Existing QUIK blocked-message semantics may not exactly match TextBlock quarantine needs.
- Without an Android emulator/device test, receive-path behavior is not fully verified.

## Batch 2 Verification

- `scripts/check-android-build-env.sh` passed with `ANDROID_HOME=/data/data/com.termux/files/home/android-sdk-termux`: 23 pass, 1 warning, 0 failures.
- `./gradlew tasks --all -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2` passed in the main worktree.
- `:domain:testDebugUnitTest --tests 'dev.octoshrimpy.quik.textblock.TextBlockFilterPolicyTest'` passed.
- `:domain:testDebugUnitTest --tests 'dev.octoshrimpy.quik.textblock.correction.*'` passed.
- `:domain:testDebugUnitTest --tests 'dev.octoshrimpy.quik.textblock.llm.*'` passed.
- `:data:compileDebugKotlin :presentation:compileDebugKotlin` passed on the settings branch.
- Full APK assembly, lint, and device/emulator receive-path testing remain pending.

## Next Slices

- Dedicated TextBlock quarantine/review UI and correction actions in the blocked messages flow.
- Wire correction decisions into classification policy.
- Add worker-level tests for SMS/MMS receive branch behavior and MMS ACK/notify preservation.
- Verify `:presentation:assembleDebug`, lint, and install on a connected Android device or emulator.

## Completion Criteria For This Milestone

- [x] Build readiness documented and improved where possible.
- [x] App identity implementation completed.
- [x] Inbound filter integration implementation completed.
- [x] Classifier tests added with Android SDK test blocker documented.
- [x] Review docs produced for completed implementation slices.
- [x] Branch pushed to `origin`.
