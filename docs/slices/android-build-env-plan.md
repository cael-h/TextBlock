# Android Build Environment Plan

## Goal

Determine the most practical way to verify Android builds from the
Termux/aarch64 development environment, and document a reproducible fallback
path that uses a normal Android SDK on desktop or CI.

## Checklist

- [x] Confirm this worktree and branch are scoped to the build environment slice.
- [x] Inspect Gradle wrapper, Android Gradle Plugin, module SDK levels, and CI build commands.
- [x] Inspect local Java, Gradle, Android SDK environment variables, `local.properties`, and Termux Android tool packages.
- [x] Reproduce the current Gradle blocker without changing the machine.
- [x] Add a non-mutating helper script that reports SDK and tool readiness.
- [x] Update build-status documentation with the recommended verification path.
- [x] Run the helper script.
- [x] Run `git diff --check`.
- [x] Address review finding: validate every compile SDK used by the repo.
- [x] Address review finding: refresh docs for the provisioned Termux SDK shim.

## Current Findings

- Worktree: `/data/data/com.termux/files/home/projects/TextBlock/quik-textblock-build-env`.
- Branch: `textblock-build-env`.
- Base commit: `645cd228ee8e60e9e7213e1e15cf2da8d2bda555`.
- Java is available in Termux: OpenJDK 17.0.19.
- `./gradlew --version` works with Gradle 8.2 on Linux aarch64.
- The root build uses Android Gradle Plugin 8.2.2.
- The app, data, domain, and `android-smsmms` modules compile against SDK 34; `common` compiles against SDK 33.
- Original worker environment: `ANDROID_HOME` and `ANDROID_SDK_ROOT` were unset,
  and `local.properties` was absent. This is correct for a repo checkout because
  `local.properties` is gitignored and machine-specific.
- Original worker result: `./gradlew tasks --all` failed before task listing
  completed:

```text
SDK location not found. Define a valid SDK location with an ANDROID_HOME
environment variable or by setting the sdk.dir path in your project's local
properties file.
```

- Current manager-provisioned environment: a local Termux SDK shim exists at
  `/data/data/com.termux/files/home/android-sdk-termux`.
- The shim includes official platform jars for both compile SDKs used by the
  repo: `platforms/android-33/android.jar` and
  `platforms/android-34/android.jar`.
- The shim includes accepted SDK license hashes and a build-tools/platform-tools
  layout backed by Termux Android tool packages.
- Termux package search did not expose a complete `android-sdk` package in the
  configured repository.
- Termux packages can provide native Android build-related tools:
  `aapt`, `aapt2`, `aidl`, `d8`, `apksigner`, and `android-tools`.
- Those Termux packages do not by themselves create the Android SDK directory
  layout that AGP expects; the manager-provisioned shim supplies that layout.
- `scripts/check-android-build-env.sh` now derives compile SDK versions from the
  repo Gradle files and validates both `android-33` and `android-34`.
- With `ANDROID_HOME=/data/data/com.termux/files/home/android-sdk-termux`, the
  helper reports 23 pass, 1 warning, and 0 failures in this worktree. The
  warning is that `sdkmanager` is not on PATH.

## Practical Recommendation

Use the manager-provisioned Termux SDK shim for local task discovery and focused
JVM/domain unit-test verification. Keep desktop or CI with a complete standard
Android SDK as the fallback and final APK/lint verification path until full APK
assembly and lint pass in Termux.

Rationale:

- The repo is a standard Android Gradle project and AGP expects SDK package
  layout, not only standalone tools.
- The Termux shim now supplies that SDK layout well enough for helper checks,
  Gradle task discovery, and focused domain unit tests.
- Gradle tasks in this Termux setup need the local AAPT2 override:
  `-Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2`.
- The existing CI workflows already target Ubuntu with Java 17 and Gradle build
  commands, which remains the safer path for final build/lint confidence.
- SDK paths, licenses, and binaries remain machine-specific and must not be
  committed.

## Termux Local Preflight Path

Use Termux for environment checks and Gradle wrapper validation:

```sh
cd /data/data/com.termux/files/home/projects/TextBlock/quik-textblock-build-env
java -version
./gradlew --version
ANDROID_HOME=/data/data/com.termux/files/home/android-sdk-termux ./scripts/check-android-build-env.sh
```

If the manager wants the currently available Termux tool packages installed or
refreshed, run:

```sh
pkg update
pkg install openjdk-17 aapt aapt2 aidl d8 apksigner android-tools
```

These packages are useful for inspection and possibly manual APK tooling, but
they still need the SDK shim layout before Gradle Android tasks can run. After
package installation or SDK shim changes, rerun:

```sh
ANDROID_HOME=/data/data/com.termux/files/home/android-sdk-termux ./scripts/check-android-build-env.sh
./gradlew --version
```

Gradle task discovery is now expected to pass in Termux with the shim and AAPT2
override:

```sh
export ANDROID_HOME=/data/data/com.termux/files/home/android-sdk-termux
./gradlew tasks --all -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2
```

Focused domain unit tests for settings, corrections, and LLM work also pass in
the main worktree with this `ANDROID_HOME` value. Full APK assembly and lint are
still pending verification in Termux.

Do not commit `local.properties` or anything under the SDK directory.

## Desktop Or CI Android SDK Path

Use a host with the Android command-line tools or Android Studio installed, then
install the packages this repo needs:

```sh
export ANDROID_HOME="$HOME/android-sdk"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

sdkmanager "platforms;android-34" "build-tools;35.0.0" "platform-tools"
sdkmanager "platforms;android-33"
yes | sdkmanager --licenses

cd /path/to/TextBlock
printf 'sdk.dir=%s\n' "$ANDROID_HOME" > local.properties
./scripts/check-android-build-env.sh
./gradlew --no-daemon :presentation:assembleDebug
./gradlew --no-daemon test
./gradlew --no-daemon lint
```

For GitHub Actions, prefer making SDK setup explicit before Gradle commands if a
runner no longer provides the needed SDK by default:

```sh
sdkmanager "platforms;android-34" "build-tools;35.0.0" "platform-tools"
sdkmanager "platforms;android-33"
yes | sdkmanager --licenses
./gradlew --no-daemon assembleDebug
./gradlew --no-daemon test
./gradlew --no-daemon lint
```

## Risks

- Termux package versions may not match the build-tools version AGP selects.
- The Termux SDK shim passes task discovery and focused domain tests, but may
  still fail later in resource processing, APK assembly, signing, or lint.
- CI workflows currently contain artifact-name assumptions from upstream QUIK in
  some files; that is separate from SDK provisioning and should be handled by
  the identity/release slices.
- Release and F-Droid builds need signing material and are not the right first
  environment check. Use `:presentation:assembleDebug` first.
- `local.properties` is intentionally ignored; every local machine must create
  its own copy or use `ANDROID_HOME`.

## Verification Plan

- Run the helper script with the provisioned Termux SDK shim. Completed: it
  reports both required compile SDK platforms, build-tools, platform-tools,
  licenses, Java, Gradle, and Termux standalone tools as available.
- Run `git diff --check`. Completed: no whitespace errors were reported.
- Treat Gradle task discovery and focused domain unit tests as supported by the
  provisioned Termux shim.
- Leave full `assembleDebug`, `test`, and `lint` verification pending until
  those commands are run successfully in Termux or desktop/CI.
