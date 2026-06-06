# Android Build Environment Review

Review target: `/data/data/com.termux/files/home/projects/TextBlock/quik-textblock-build-env` on branch `textblock-build-env`, reviewed against base commit `645cd228`.

## Findings

1. Medium - The helper can report a false-ready SDK when `android-33` is missing. `scripts/check-android-build-env.sh:5` hard-codes only `PROJECT_COMPILE_SDK=34`, and `scripts/check-android-build-env.sh:109-116` checks only `platforms/android-34/android.jar`. The slice plan itself notes that `common` compiles against SDK 33 at `docs/slices/android-build-env-plan.md:28`. If a Termux or CI SDK has android-34 but not android-33, the helper can pass while Gradle still fails on the `common` module. The script should check every compile SDK used by the repo, or derive the set from Gradle files.

2. Medium - The build-status docs are stale relative to the current main Termux shim. `docs/slices/android-build-env-plan.md:31-48` still says `./gradlew tasks --all` fails because no SDK path exists, and `docs/slices/android-build-env-plan.md:52-65` recommends desktop/CI as the primary verification path until a manager provisions a complete SDK. `docs/DEVELOPMENT_PLAN.md:144-150` repeats that Android Gradle tasks are blocked until a complete SDK is configured. Current main-worktree evidence supersedes this: `local.properties` points at `/data/data/com.termux/files/home/android-sdk-termux`, the helper passes with `ANDROID_HOME` set to that path, and `./gradlew tasks --all -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2` completes successfully. The docs should distinguish the worker's original environment from the now-provisioned main shim.

## Open Questions / Assumptions

- I treated the worker's original "SDK location not found" result as stale for the main worktree, per the manager context and current local verification.
- I assumed the script is meant to be a readiness check for all modules, not only a generic SDK-34 preflight.
- It is still an open product/process decision whether the Termux SDK shim should be documented as supported local verification or as an experimental manager-only setup.

## Test Gaps / Residual Risk

- I did not run `assembleDebug`, `test`, or `lint`; current evidence is limited to helper-script checks and Gradle task discovery.
- The helper has not been tested against mixed good/bad `ANDROID_HOME`, `ANDROID_SDK_ROOT`, and `local.properties` combinations.
- The script does not verify the Gradle property needed in this Termux setup: `android.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2`.

## Verification

- `git diff --check 645cd228` passed for the slice.
- `sh -n scripts/check-android-build-env.sh` passed.
- `ANDROID_HOME=/data/data/com.termux/files/home/android-sdk-termux scripts/check-android-build-env.sh` passed with 20 pass, 1 warning, 0 failures in the build-env worktree.
- In the main worktree, `ANDROID_HOME=/data/data/com.termux/files/home/android-sdk-termux ./gradlew tasks --all -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2` passed.

## Behavior Review Notes

- The script is non-mutating by inspection: it reads environment variables, `local.properties`, SDK files, and command availability; it does not write files or install packages.
- Variable expansion is generally quoted, and SDK paths are not committed by the slice.
- The docs correctly warn not to commit `local.properties` or SDK directories in `docs/slices/android-build-env-plan.md:104`.

## Summary

The helper is useful and non-mutating, but it needs to validate all compile SDK platforms. The documentation needs a status refresh now that the main Termux SDK shim can pass helper checks and Gradle task discovery.
