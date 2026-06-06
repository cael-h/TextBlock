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
- Latest pushed commit: `c67a3ba5 Document TextBlock development plan`
- Worktree support: available
- Java status: `java` not found
- Android SDK env vars: `ANDROID_HOME`, `ANDROID_SDK_ROOT`, and `JAVA_HOME` are unset
- Automation status: no scheduler/automation tool is available in this session, so 20-minute checks will be manual.
- Multi-agent status: subagent tools are available.

## Parallel Slice Plan

### Slice A: Build Readiness

Owner: manager/local critical path

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

### Slice B: App Identity Separation

Owner: worker

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

### Slice C: Inbound Filter Integration

Owner: worker

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

### Slice D: Classifier Tests And Synthetic Samples

Owner: worker

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

### Slice E: Review

Owner: reviewer after workers finish

Goal:

- Review each completed slice.
- Write findings to `docs/reviews/<slice>-review.md`.
- Original worker closes or disputes findings.

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

## Risk Register

- Build tooling may be incomplete in Termux.
- App identity changes can touch broad resource/manifest surfaces.
- Inbound filtering can accidentally delete or hide wanted messages if wired too aggressively.
- Existing QUIK blocked-message semantics may not exactly match TextBlock quarantine needs.
- Without an Android emulator/device test, receive-path behavior is not fully verified.

## Completion Criteria For This Milestone

- Build readiness documented and improved where possible.
- App identity plan or implementation completed.
- Inbound filter integration plan or implementation completed.
- Classifier tests added or test blocker documented.
- Review docs produced for completed implementation slices.
- Branch pushed to `origin`.
