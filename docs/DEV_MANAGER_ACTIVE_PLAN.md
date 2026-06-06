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

## Main Goals For This Session

- Establish build readiness for Android work in Termux.
- Separate TextBlock identity from QUIK so both can coexist if needed.
- Prepare the inbound SMS/MMS filter integration.
- Add classifier tests without committing raw user text samples.
- Keep documentation current and reviewable.

## Current Status

- Branch: `textblock-filter-foundation`
- Local integration head: `92ad5f61 Merge TextBlock inbound filter slice`
- Latest pushed commit before this integration batch: `fa5ac347 Add active dev manager plan`
- Worktree support: available
- Java status: OpenJDK 17.0.19 installed and `./gradlew --version` works.
- Android SDK status: not configured. Gradle Android tasks fail before compile/test because `ANDROID_HOME` is unset and `local.properties` has no `sdk.dir`.
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

## Risk Register

- Build tooling may be incomplete in Termux.
- App identity changes can touch broad resource/manifest surfaces.
- Inbound filtering can accidentally delete or hide wanted messages if wired too aggressively.
- Existing QUIK blocked-message semantics may not exactly match TextBlock quarantine needs.
- Without an Android emulator/device test, receive-path behavior is not fully verified.

## Completion Criteria For This Milestone

- [x] Build readiness documented and improved where possible.
- [x] App identity implementation completed.
- [x] Inbound filter integration implementation completed.
- [x] Classifier tests added with Android SDK test blocker documented.
- [x] Review docs produced for completed implementation slices.
- [ ] Branch pushed to `origin`.
