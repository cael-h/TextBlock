# TextBlock Development Plan

## Purpose

TextBlock is a personal fork of QUIK SMS focused on local SMS/MMS filtering,
starting with political campaign spam. The app should become the default Android
SMS/MMS app, inspect inbound messages before notification, and quarantine likely
spam without sending message contents to a cloud service.

The long-term direction is a local-first messaging app with optional on-device
LLM support for nuanced filtering and later message Q&A. The initial product
must work without an LLM.

## Repository Setup

Local checkout:

```sh
/data/data/com.termux/files/home/projects/TextBlock/quik-textblock
```

GitHub fork:

```text
https://github.com/cael-h/TextBlock
```

Upstream project:

```text
https://github.com/quik-sms/quik
```

Current working branch:

```sh
textblock-filter-foundation
```

Current foundation commit:

```sh
1deb31ac Add TextBlock filter foundation
```

Remote layout:

```sh
origin   https://github.com/cael-h/TextBlock.git
upstream https://github.com/quik-sms/quik.git
```

`origin` is the fork we push TextBlock work to. `upstream` is the source QUIK
repo we pull updates from. Upstream push is intentionally disabled locally:

```sh
git remote set-url --push upstream DISABLED
```

This prevents accidental pushes to the source project.

## Working Rules

- Keep QUIK as the upstream base.
- Put TextBlock-specific work on feature branches in the fork.
- Pull/merge upstream QUIK updates regularly.
- Do not rewrite upstream history.
- Prefer small milestone commits that are easy to review and revert.
- Keep message-filter logic local-first and privacy-preserving.
- Do not store raw user message exports in the repo.
- Store extracted identifiers and rule notes instead of raw personal messages.

## Pulling QUIK Updates

Use this flow when we want upstream changes from QUIK:

```sh
cd /data/data/com.termux/files/home/projects/TextBlock/quik-textblock
git status --short --branch
git fetch upstream
git merge upstream/master
git push origin HEAD
```

If the working tree has uncommitted TextBlock changes, commit or stash them
before merging upstream:

```sh
git status --short
git add <files>
git commit -m "<message>"
```

For a more linear history, rebase can be used instead of merge:

```sh
git fetch upstream
git rebase upstream/master
git push --force-with-lease origin HEAD
```

Default preference: use merge unless there is a clear reason to rebase.

## Branch Strategy

Suggested branch pattern:

```text
textblock-filter-foundation
textblock-wire-inbound-filter
textblock-quarantine-ui
textblock-settings
textblock-llm-adapter
```

Each branch should target one milestone. Push every notable milestone to
`origin`:

```sh
git push -u origin <branch-name>
```

## Current Foundation

Added docs:

- `docs/textblock-filter-architecture.md`
- `docs/drive-sample-identifiers.md`
- `docs/DEVELOPMENT_PLAN.md`

Added classifier boundary:

- `domain/src/main/java/com/moez/QKSMS/textblock/InboundMessageClassifier.kt`
- `domain/src/main/java/com/moez/QKSMS/textblock/InboundMessageForClassification.kt`
- `domain/src/main/java/com/moez/QKSMS/textblock/ClassificationResult.kt`
- `domain/src/main/java/com/moez/QKSMS/textblock/RuleBasedPoliticalClassifier.kt`

The current classifier is not wired into live SMS/MMS receive handling yet. It is
the stable boundary for rule-based filtering now and on-device LLM filtering
later.

## Build Environment Status

The Termux/aarch64 checkout can run Java 17 and `./gradlew --version`. A
manager-provisioned SDK shim now exists at
`/data/data/com.termux/files/home/android-sdk-termux` with Android platforms 33
and 34, accepted licenses, platform-tools, and build-tools backed by Termux
Android tool packages.

Use `scripts/check-android-build-env.sh` for non-mutating local checks. With the
Termux shim, Gradle task discovery, focused domain unit tests, debug APK
assembly, and lint can be verified locally:

```sh
export ANDROID_HOME=/data/data/com.termux/files/home/android-sdk-termux
./scripts/check-android-build-env.sh
./gradlew tasks --all -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2
./gradlew --no-daemon :presentation:assembleDebug -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2
./gradlew --no-daemon lint -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2
```

Slice M verified APK assembly and lint in Termux on June 7, 2026:

- Environment check: 23 pass, 1 warning, 0 failures.
- `:presentation:assembleDebug`: passed in 4m44s.
- APK artifact:
  `presentation/build/outputs/apk/debug/TextBlock-v4.3.6-debug.apk`.
- `lint`: passed in 23m46s.
- Install remains unverified because `adb devices` reported no attached
  device or emulator rows.

See `docs/slices/android-build-env-plan.md` for Termux package notes and
fallback SDK setup commands. See
`docs/slices/textblock-apk-verification-plan.md` for exact APK, lint, ADB, and
install verification evidence.

## Sample-Derived Rules

The first rule set came from user-provided Google Drive text exports. The raw
message bodies were inspected through the Drive connector, but not stored in the
repo. The extracted patterns are documented in:

```text
docs/drive-sample-identifiers.md
```

Current high-signal identifiers:

- known campaign/PAC domains
- `/actblue_secure_`
- campaign `/l/` short-link paths
- `End2End` footer
- small-dollar donation asks
- district/race codes like `PA-07` and `SC-01`
- petition language
- election urgency language
- PAC/candidate/campaign vocabulary
- Unicode compatibility normalization for stylized text
- stripping MMS export placeholders like `[image/jpeg]` and `null`

## Milestone 1: Wire Rule Filter Into Receive Path

Goal: classify inbound messages before notification.

Target files:

- `data/src/main/java/com/moez/QKSMS/worker/ReceiveSmsWorker.kt`
- `data/src/main/java/com/moez/QKSMS/worker/ReceiveMmsWorker.kt`
- Dagger/provider wiring in the data/presentation injection modules

Expected behavior:

- Run existing blocked-sender checks.
- Run existing user content filters.
- Run TextBlock classifier.
- If classifier returns `ALLOW`, continue normal QUIK behavior.
- If classifier returns `QUARANTINE`, suppress normal notification and route to
  a reviewable blocked/quarantine state.
- Do not delete by default.

Open implementation question:

- Reuse QUIK's existing blocked conversation UI first, or build a dedicated
  TextBlock quarantine view.

Initial preference:

- Reuse existing blocked/quarantine mechanics for MVP speed, then add a better
  dedicated view after behavior is proven.

## Milestone 2: Settings And Corrections

Goal: make filtering controllable and correctable.

Expected controls:

- enable/disable TextBlock political filtering
- quarantine vs drop mode
- contacts allowlist behavior
- "Not spam" correction
- "Block similar" correction

Correction data should stay local and exportable.

## Milestone 3: Test Coverage

Goal: verify the classifier with realistic examples and false-positive guards.

Test categories:

- known political donation spam
- known political petition spam
- stylized Unicode text
- MMS export noise
- benign political conversation from a contact
- normal messages with money amounts
- normal messages with `/l/` links

Do not commit raw personal texts. Use synthetic or heavily reduced examples that
preserve only the relevant identifiers.

## Milestone 4: On-Device LLM Adapter

Goal: support more nuanced filtering without coupling model runtime to SMS
receive code.

Design rule:

- Keep all model runtime code behind `InboundMessageClassifier`.
- Do not call an LLM directly from `ReceiveSmsWorker` or `ReceiveMmsWorker`.

Expected flow:

1. Fast local rules run first.
2. Obvious allow/quarantine decisions return immediately.
3. Ambiguous messages can be queued for model classification.
4. Model results update local message state and future heuristics.

Potential Android runtime targets:

- Google AI Edge / LiteRT-LM for Gemma-family local inference.
- MediaPipe LLM Inference API only if it remains the simpler supported option.
- No-LLM fallback on lower-end devices.

## Build Status

As of this setup, Gradle was not run successfully in Termux because Java is not
installed/configured:

```text
ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
```

Before Android build verification, install/configure a JDK and Android SDK tools
for this Termux environment.

## Useful Commands

Check repo state:

```sh
cd /data/data/com.termux/files/home/projects/TextBlock/quik-textblock
git status --short --branch
git remote -v
```

Commit TextBlock work:

```sh
git add <files>
git commit -m "<message>"
git push origin HEAD
```

Create a new milestone branch:

```sh
git switch -c textblock-wire-inbound-filter
git push -u origin textblock-wire-inbound-filter
```

Fetch upstream QUIK without merging:

```sh
git fetch upstream
git log --oneline --decorate --graph --max-count=20 --all
```

Merge upstream QUIK:

```sh
git fetch upstream
git merge upstream/master
git push origin HEAD
```

Verify the fork:

```sh
gh repo view cael-h/TextBlock --json nameWithOwner,url,isFork
```
