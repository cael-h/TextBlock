# Classifier Tests Slice Review

Worktree: `/data/data/com.termux/files/home/projects/TextBlock/quik-textblock-classifier-tests`
Branch: `textblock-classifier-tests`
Base commit: `fa5ac347`

## Findings

No implementation findings were found in the reviewed classifier-tests slice.

## Open Questions / Assumptions

- Assumed exact diagnostic reason strings are acceptable test expectations for
  this early rule-based classifier. If those reasons are not intended as stable
  behavior, future tests may be less brittle if they assert action/category and
  use fewer reason-string checks.
- The classifier currently returns only `ALLOW` and `QUARANTINE`; future tests
  should be added if `BLOCK_CONVERSATION`, `DROP`, or score configurability are
  introduced.
- Test bodies appear synthetic and do not copy raw junk-message samples.

## Test Gaps

- `git diff --check` passed for the reviewed classifier-tests worktree.
- I ran `./gradlew :domain:testDebugUnitTest --no-daemon`; it failed before test
  compilation because the Android SDK location is not configured:
  `ANDROID_HOME` is unset and `local.properties` has no `sdk.dir`.
- Because of that environment gap, the new tests were statically reviewed but
  not executed.
- These are classifier unit tests only; receive-worker integration behavior is
  still uncovered.

## Summary

The slice adds minimal JUnit 4 support to `domain/build.gradle` and focused,
synthetic unit coverage for political donation spam, petition spam, Unicode
normalization, MMS export noise, and benign allow cases. The coverage is scoped
appropriately for the classifier layer.
