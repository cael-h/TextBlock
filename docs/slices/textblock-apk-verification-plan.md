# TextBlock APK Verification Plan

## Goal

Verify whether the current `textblock-apk-verification` branch can assemble a
debug APK in the Termux/aarch64 environment, determine whether lint can run, and
attempt APK installation only if `adb` reports a connected device or emulator in
the `device` state.

## Scope

- Worktree:
  `/data/data/com.termux/files/home/projects/TextBlock/quik-textblock-apk-verification`
- Branch: `textblock-apk-verification`
- Base branch: `textblock-filter-foundation`
- Base commit: `32d06eaff695168689c348378e0dcf3797d9553d`
- SDK shim: `/data/data/com.termux/files/home/android-sdk-termux`
- Termux AAPT2 override: `/data/data/com.termux/files/usr/bin/aapt2`

This slice is verification and documentation only. Production Kotlin, Java,
resources, Gradle plugin versions, SDK binaries, `local.properties`, and build
outputs are out of scope.

## Checklist

- [x] Confirm branch and clean starting state.
- [x] Write this plan before running verification commands.
- [x] Run the Android build environment checker with the Termux SDK shim.
- [x] Run `:presentation:assembleDebug` with the Termux SDK shim and AAPT2
  override.
- [x] If assembly reaches a meaningful APK/lint-ready state, run `lint` with
  the same SDK shim and AAPT2 override.
- [x] Run `adb devices`.
- [x] Install the generated debug APK only if at least one target is listed in
  the `device` state.
- [x] Record exact command output summaries, artifact paths, blockers, and
  install result.
- [x] Update `docs/DEVELOPMENT_PLAN.md` with the verification status.
- [x] Run `git diff --check`.
- [x] Commit documentation-only changes on `textblock-apk-verification`.

## Commands

Environment check:

```sh
ANDROID_HOME=/data/data/com.termux/files/home/android-sdk-termux ./scripts/check-android-build-env.sh
```

Assemble debug APK:

```sh
ANDROID_HOME=/data/data/com.termux/files/home/android-sdk-termux ./gradlew --no-daemon :presentation:assembleDebug -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2
```

Lint, if assembly gets far enough:

```sh
ANDROID_HOME=/data/data/com.termux/files/home/android-sdk-termux ./gradlew --no-daemon lint -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2
```

ADB target check:

```sh
adb devices
```

Install command, only if a safe target is present. This command was not run in
this verification because no target was listed as `device`:

```sh
adb install -r presentation/build/outputs/apk/debug/TextBlock-v4.3.6-debug.apk
```

## Expected Artifact Paths

The expected APK output path was:

```text
presentation/build/outputs/apk/debug/TextBlock-v4.3.6-debug.apk
```

The APK was generated successfully and was 54 MB on disk.

If the project emits different debug APK names in future branches, inspect:

```text
presentation/build/outputs/apk/
```

## Status Log

### 2026-06-07 Initial State

- Git branch: `textblock-apk-verification`.
- Starting HEAD: `32d06eaff695168689c348378e0dcf3797d9553d`.
- Starting worktree status: clean.
- Verification had not yet run in this slice when this plan was created.

### 2026-06-07 Environment Check

Command:

```sh
ANDROID_HOME=/data/data/com.termux/files/home/android-sdk-termux ./scripts/check-android-build-env.sh
```

Result: pass.

Evidence:

- Summary: 23 pass, 1 warn, 0 fail.
- Java 17.0.19 was found.
- The SDK shim was found through `ANDROID_HOME`.
- Android platform jars for compile SDK 33 and 34 were found.
- Build-tools and platform-tools entries were found.
- Termux standalone `aapt`, `aapt2`, `aidl`, `d8`, `apksigner`, `adb`, and
  `zipalign` were found.
- Warning: `sdkmanager` was not found on PATH.

### 2026-06-07 Debug APK Assembly

Command:

```sh
ANDROID_HOME=/data/data/com.termux/files/home/android-sdk-termux ./gradlew --no-daemon :presentation:assembleDebug -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2
```

Result: pass.

Evidence:

- `BUILD SUCCESSFUL in 4m 44s`.
- `115 actionable tasks: 47 executed, 68 from cache`.
- APK artifact:
  `presentation/build/outputs/apk/debug/TextBlock-v4.3.6-debug.apk`.
- APK size: 54 MB.
- Packaging note: `stripDebugDebugSymbols` could not strip
  `librealm-jni.so`, so the library was packaged as-is.
- Gradle note: deprecated Gradle features were reported as incompatible with
  Gradle 9.0.

### 2026-06-07 Lint

Command:

```sh
ANDROID_HOME=/data/data/com.termux/files/home/android-sdk-termux ./gradlew --no-daemon lint -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2
```

Result: pass.

Evidence:

- `BUILD SUCCESSFUL in 23m 46s`.
- `174 actionable tasks: 75 executed, 8 from cache, 91 up-to-date`.
- HTML lint reports were written to:
  - `android-smsmms/build/reports/lint-results-debug.html`
  - `common/build/reports/lint-results-debug.html`
  - `data/build/reports/lint-results-debug.html`
  - `domain/build/reports/lint-results-debug.html`
  - `presentation/build/reports/lint-results-debug.html`
- Lint analysis printed third-party lint registry vendor warnings for
  AutoDispose and Conductor. These did not fail the lint build.
- Gradle note: deprecated Gradle features were reported as incompatible with
  Gradle 9.0.

### 2026-06-07 ADB And Install

Command:

```sh
adb devices
```

Result: no install target.

Evidence:

- ADB daemon started successfully.
- `adb devices` printed `List of devices attached` with no device rows.
- No `device`-state target was present.
- Install was intentionally skipped.

### 2026-06-07 Diff Check

Command:

```sh
git diff --check
```

Result: pass. No whitespace errors were reported.

### 2026-06-07 Review Follow-Up

Review finding: medium documentation consistency issue in
`docs/DEVELOPMENT_PLAN.md`.

Result: implemented. The stale later `Build Status` section now points to the
verified Java 17, Termux SDK shim, `assembleDebug`, lint, and no-device install
status instead of the old Java-missing blocker.

## Results

- Termux environment check passed with the SDK shim.
- Debug APK assembly passed in Termux.
- Lint passed in Termux.
- Debug APK artifact:
  `presentation/build/outputs/apk/debug/TextBlock-v4.3.6-debug.apk`.
- Install was not attempted because no connected Android device or emulator was
  listed as `device` by `adb devices`.

## Blockers

- Device install remains unverified. Precise blocker: `adb devices` returned no
  attached device or emulator rows, so there was no safe install target.
- `sdkmanager` is not on PATH in this Termux environment, but this did not block
  environment check, debug APK assembly, or lint with the provisioned SDK shim.
