# Google Play Release Build Verification

## Summary

TextBlock now builds Play-ready release artifacts at target SDK 35:

- Play upload bundle:
  `presentation/build/outputs/bundle/release/TextBlock-v4.3.7-release.aab`
- Sideload APK:
  `presentation/build/outputs/apk/release/TextBlock-v4.3.7-release.apk`
- Shared copies:
  `/data/data/com.termux/files/home/termux_share/TextBlock-v4.3.7-release.aab`
  `/data/data/com.termux/files/home/termux_share/TextBlock-v4.3.7-release.apk`

## Build Environment Changes

- Added Android SDK Platform 35 to the local Termux SDK shim at
  `/data/data/com.termux/files/home/android-sdk-termux/platforms/android-35`.
- Verified Google SDK package SHA-1 for `platform-35_r02.zip`:
  `0bb560a90a7a2cbd0dd8348224d518b638fe7949`.
- Upgraded Termux Android build tools needed for API 35 resource linking:
  `aapt`, `aapt2`, `aidl`, `android-tools`, `abseil-cpp`, and `libprotobuf`.

## Code / Config Changes

- Raised all Android modules to `compileSdk 35` and `targetSdkVersion 35`.
- Bumped the release version to `4.3.7` / `2239`.
- Added `android.suppressUnsupportedCompileSdk=35` because the project still
  uses Android Gradle Plugin 8.2.2.
- Added missing default-locale fallback resources for:
  `compose_send_group_summary` and `scheduled_options`.

## Verification

Environment check:

```sh
export ANDROID_HOME=/data/data/com.termux/files/home/android-sdk-termux
./scripts/check-android-build-env.sh
```

Result:

```text
Summary: 22 pass, 1 warn, 0 fail
```

Build and tests:

```sh
export ANDROID_HOME=/data/data/com.termux/files/home/android-sdk-termux
./gradlew --no-daemon :domain:testDebugUnitTest :presentation:bundleRelease :presentation:assembleRelease \
  -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2
```

Result:

```text
BUILD SUCCESSFUL in 8m 20s
```

APK metadata:

```text
package: name='com.caelh.textblock' versionCode='2239' versionName='4.3.7'
compileSdkVersion='35'
sdkVersion:'23'
targetSdkVersion:'35'
```

APK signing:

```text
Verified using v1 scheme (JAR signing): true
Verified using v2 scheme (APK Signature Scheme v2): true
Number of signers: 1
Signer #1 certificate SHA-256 digest:
156b479321cf09e00c97514cf2134d1e33e9a887341a503d83ae074ffc951391
```

AAB signing:

```text
jar verified.
```

Artifact hashes:

```text
455903dfd326a7d8bb468ef1e6a5b884fdb0a2b8b48b14881d3e082adfb565cb  TextBlock-v4.3.7-release.apk
d45d06c1d08651ba7e7f1b4a712ed486c7000ddcc13f3f01c11e52a037fba986  TextBlock-v4.3.7-release.aab
```

## Install / Smoke Test Status

Automated install and UI smoke tests are blocked in this Termux session because
ADB has no attached device:

```text
List of devices attached
```

Localhost ADB is also unavailable:

```text
failed to connect to '127.0.0.1:5555': Connection refused
```

## Manual Smoke Checklist

Run this after installing `TextBlock-v4.3.7-release.apk` on the phone:

- Confirm the app launches and reports version `4.3.7` if version is visible.
- Set TextBlock as the default SMS app.
- Send a normal outgoing text and confirm it appears in the thread.
- Receive a normal allowed text and confirm notification appears.
- Receive or simulate a political spam pattern and confirm it is filtered before
  notification.
- Open quarantine / blocked review and confirm the filtered message is present.
- Use "Block similar text" from both inbox and conversation overflow menus.
- Use "Not spam" / restore behavior if available on a quarantined message.
- Long press message whitespace and confirm reaction options appear.
- Long press actual message text and confirm text selection still works.
- Restart the app and confirm inbox/conversation state persists.

## Residual Risk

- Device install and runtime smoke testing still need to be completed on a phone
  or emulator with ADB access.
- The manifest still declares many sensitive permissions. These are plausible
  for a full SMS/MMS client, but they should be audited before broad release and
  justified carefully in the Play Console SMS permission declaration.
- The build emits many legacy Kotlin/deprecation warnings inherited from QUIK.
  They are not blocking the Play upload artifact, but they remain maintenance
  debt.
