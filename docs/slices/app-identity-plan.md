# App Identity Separation Plan

## Goal

Make this TextBlock worktree install and present as its own Android app while
keeping upstream QUIK source/package structure intact enough for future merges.

## Checklist

- [x] Inspect git status and the identity-related Gradle, manifest, string, shortcut, and metadata files.
- [x] Change the Android install identity from QUIK to TextBlock by updating the app module `applicationId` and generated artifact name.
- [x] Change user-facing app display names from QUIK to TextBlock in the default app label resources and build-type overrides.
- [x] Update app identity surfaces that must follow the install package, including launcher shortcuts.
- [x] Update minimal store metadata title entries where they still present the app as QUIK.
- [x] Re-scan for remaining QUIK and package identity references and document any intentionally retained upstream surfaces.
- [x] Run lightweight verification and Gradle if Java is available; document any blockers.

## Inspection Notes

- Worktree branch: `textblock-app-identity`.
- Initial git status: clean.
- `presentation/build.gradle` currently sets `applicationId 'dev.octoshrimpy.quik'`.
- `presentation/build.gradle` also sets `archivesBaseName` to `QUIK-v...` and debug `app_name` to `QUIK-Debug`.
- `presentation/build.gradle` namespace remains `dev.octoshrimpy.quik`; this should stay unchanged for now so Kotlin packages and generated `R`/`BuildConfig` imports do not require a broad refactor.
- `presentation/src/main/AndroidManifest.xml` uses relative component class names and `${applicationId}` for widget/startup authorities, so changing `applicationId` should update those runtime identities automatically.
- `presentation/src/main/res/xml/shortcuts.xml` hardcodes `dev.octoshrimpy.quik` as the shortcut target package. This must move to TextBlock's application id so launcher shortcuts target the installed package.
- `presentation/src/main/res/values/strings.xml` defines `app_name` as `QUIK` and includes several other user-facing QUIK strings.
- Localized `strings.xml` files contain many translated QUIK references. Because `app_name` is `translatable="false"` in the default resource file, changing the default value should update the app label without editing every translation.
- `metadata/en-US/title.txt` and `metadata/fr-FR/title.txt` still say `QUIK SMS`.

## Proposed Scope

In scope:

- Set the app module `applicationId` to a TextBlock package id.
- Keep `namespace 'dev.octoshrimpy.quik'` and source package declarations unchanged.
- Rename the default app label to `TextBlock`.
- Rename debug label to `TextBlock-Debug`.
- Rename generated archive base from `QUIK-v...` to `TextBlock-v...`.
- Update launcher shortcut target package/action to the TextBlock application id while keeping the existing target class package.
- Update minimal metadata titles from `QUIK SMS` to `TextBlock`.
- Document remaining QUIK references that are upstream source structure, localized stale copy, premium feature copy, about links/contact, copyright comments, or package names retained for mergeability.

Out of scope:

- Kotlin/Java package refactors.
- Android namespace refactor.
- App icon or visual redesign.
- SMS/MMS filtering behavior.
- Classifier tests or filter logic.
- Full localization rewrite.

## Package Decision

Use `com.caelh.textblock` as the base Android `applicationId`. This gives
Android a separate package from upstream `dev.octoshrimpy.quik` while avoiding a
source package refactor.

Build variants after the change:

- Release: `com.caelh.textblock`
- Debug: `com.caelh.textblock.debug`
- F-Droid: `com.caelh.textblock.fdroid`

## Implementation Changes

- `presentation/build.gradle`
  - Changed release `applicationId` from `dev.octoshrimpy.quik` to `com.caelh.textblock`.
  - Changed archive base name from `QUIK-v...` to `TextBlock-v...`.
  - Changed debug app label override from `QUIK-Debug` to `TextBlock-Debug`.
  - Changed debug `application_id` generated string to `com.caelh.textblock.debug`.
  - Added F-Droid `application_id` generated string for `com.caelh.textblock.fdroid`.
  - Left `namespace 'dev.octoshrimpy.quik'` unchanged.
- `presentation/src/main/res/values/strings.xml`
  - Changed `app_name` to `TextBlock`.
  - Added default `application_id` string as `com.caelh.textblock` for release resources and shortcut targeting.
  - Replaced default English user-facing QUIK app-name copy with TextBlock, including default-SMS, permission, rate, about, blocking manager, backup path, changelog, and plus-gated copy.
- `presentation/src/main/res/xml/shortcuts.xml`
  - Changed shortcut action from `dev.octoshrimpy.quik.START` to `com.caelh.textblock.START`.
  - Changed shortcut target package from hardcoded `dev.octoshrimpy.quik` to `@string/application_id`.
  - Left shortcut target class as `dev.octoshrimpy.quik.feature.compose.ComposeActivity` because the runtime class package was intentionally retained.
- `metadata/en-US/title.txt` and `metadata/fr-FR/title.txt`
  - Changed title from `QUIK SMS` to `TextBlock`.
- `presentation/src/main/res/values-fr/strings.xml`
  - Removed the localized `app_name` override so French devices inherit the default non-translatable `TextBlock` app label.

## Verification

- `git status --short` shows only this slice's scoped changes:
  - `metadata/en-US/title.txt`
  - `metadata/fr-FR/title.txt`
  - `presentation/build.gradle`
  - `presentation/src/main/res/values/strings.xml`
  - `presentation/src/main/res/xml/shortcuts.xml`
  - `docs/slices/app-identity-plan.md`
- Focused grep found no remaining old release identity in scoped identity files:
  - `applicationId 'dev.octoshrimpy.quik'`
  - `targetPackage="dev.octoshrimpy.quik"`
  - `android:action="dev.octoshrimpy.quik.START"`
  - exact `>QUIK<`, `QUIK SMS`, `QUIK-Debug`, or `QUIK-v`
- Java is available: OpenJDK 17.0.19.
- Tried `./gradlew :presentation:processDebugResources`. The task started a Gradle daemon but produced no additional output under parallel Gradle load from other worktrees. To avoid leaving this slice's verification process running indefinitely, only this worktree's wrapper/daemon was stopped. Full Gradle resource verification remains pending.

## Review Response

- Review doc: `/data/data/com.termux/files/home/projects/TextBlock/quik-textblock/docs/reviews/app-identity-review.md`.
- Finding implemented: Medium, French localized app label could still resolve to QUIK.
- Response: removed `presentation/src/main/res/values-fr/strings.xml`'s localized `app_name` override. The only remaining `app_name` resource definition is the default `presentation/src/main/res/values/strings.xml` value, `TextBlock`, so localized launcher labels inherit the intended app identity.
- Verification added:
  - Scanned all `presentation/src/main/res/**/strings.xml` files for `<string name="app_name"` overrides; only `presentation/src/main/res/values/strings.xml` remains, with `TextBlock`.
  - Focused old-label grep found no `app_name` value of `QUIK`, `QUIK SMS`, `QUIK-Debug`, `QUIK-v`, old shortcut package/action, or old release `applicationId` in scoped identity files.
  - `git diff --check` passed.

## Remaining Identity Surfaces To Revisit Later

- `namespace 'dev.octoshrimpy.quik'` and Kotlin/Java package declarations.
- ProGuard keep rule for `dev.octoshrimpy.quik`.
- About/source/contact strings that still point to upstream QUIK.
- Localized QUIK references in non-default resources.
- `tools:text` preview-only strings that still mention QUIK.
- Internal resource names such as `qksms_plus`.
- Copyright/license comments inherited from upstream QKSMS/QUIK.
- Release signing alias and release keystore filename.
