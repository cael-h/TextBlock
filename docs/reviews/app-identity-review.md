# App Identity Slice Review

Worktree: `/data/data/com.termux/files/home/projects/TextBlock/quik-textblock-identity`
Branch: `textblock-app-identity`
Base commit: `fa5ac347`

## Findings

### Medium: French app label can still resolve to QUIK

`docs/slices/app-identity-plan.md:27-28` assumes changing the default
`translatable="false"` `app_name` is enough to update the app label without
editing translations. Static inspection found a locale-specific override at
`presentation/src/main/res/values-fr/strings.xml:20` that still defines
`app_name` as `QUIK`. Because the manifest label uses `@string/app_name`, French
locale devices can still show the old app name even though
`presentation/src/main/res/values/strings.xml:21` now says `TextBlock`.

Suggested fix: remove or update the localized `app_name` override, and scan for
any other localized app-label overrides before merging the identity slice.

## Open Questions / Assumptions

- Assumed `com.caelh.textblock` is the intended permanent Android
  `applicationId` for release, with `.debug` and `.fdroid` suffixes for those
  variants.
- Assumed retaining `namespace 'dev.octoshrimpy.quik'` and source package names
  is intentional for upstream mergeability.
- Localized non-label copy that still says QUIK appears intentionally deferred,
  but the app label override above is not just stale copy.

## Test Gaps

- `git diff --check` passed for the reviewed identity worktree.
- Android resource merge/build verification was not completed in this Termux
  environment because the Android SDK is not configured. Treat this as a
  verification gap rather than an implementation finding.
- Launcher shortcut behavior was reviewed statically only; no device/emulator
  install was available to confirm variant-specific shortcut target packages.

## Summary

The slice correctly separates the install package from upstream QUIK while
keeping the source namespace stable. Default English identity surfaces and store
titles were updated. The main merge blocker is the remaining French `app_name`
resource override, which can keep the launcher-visible brand as QUIK on French
locale devices.
