# Classifier Tests Slice Plan

## Scope

Add focused local unit coverage for `RuleBasedPoliticalClassifier` using synthetic
message examples only. The tests should cover political donation and petition
spam, Unicode compatibility normalization, MMS export placeholder stripping, and
benign allow cases that should not be quarantined.

## Constraints

- Do not use raw user message bodies.
- Keep changes scoped to `domain/src/test/**`, this plan, and
  `domain/build.gradle` only if local unit test dependencies are required.
- Do not change app identity, SMS/MMS receive integration, quarantine UI, or
  settings behavior.

## Findings

- `domain/src/test` currently has no tests.
- `domain/build.gradle` does not currently declare `testImplementation`
  dependencies.
- Neighboring modules use JUnit 4 via the root `junit_version` property.
- `RuleBasedPoliticalClassifier` is pure Kotlin/JVM-compatible except for
  Android library module build plumbing, so focused local unit tests should be
  feasible.

## Checklist

- [x] Inspect git status, `domain/build.gradle`, existing tests,
  and classifier/model APIs.
- [x] Add focused synthetic unit tests for quarantine cases.
- [x] Add focused synthetic unit tests for allow cases.
- [x] Add minimal domain test dependency support if required.
- [x] Attempt the relevant Gradle test task; Android SDK configuration blocked
  execution before test compilation.
- [x] Perform static review of test compile assumptions and expected classifier
  outcomes.
- [x] Update this checklist with final status.

## Proposed Test Cases

- Known political donation spam quarantines as fundraising.
- Political petition spam quarantines.
- Stylized Unicode political spam normalizes and quarantines.
- MMS export placeholders such as `[image/jpeg]` and standalone `null` lines do
  not prevent detection.
- Benign political conversation from a contact is allowed when score remains
  low.
- Normal messages with money amounts are allowed.
- Normal messages with `/l/` links are allowed unless combined with political or
  campaign signals.

## Verification

- Attempted `./gradlew :domain:testDebugUnitTest`.
- Result: blocked before test compilation because the Android SDK location is
  not configured for this worktree. Gradle reported that `ANDROID_HOME` is not
  set and `local.properties` has no `sdk.dir`.
- Static review completed:
  - Test source package matches the classifier package
    `dev.octoshrimpy.quik.textblock`.
  - Kotlin test source is under `domain/src/test/java`, matching the project's
    existing Kotlin-under-`java` source layout.
  - `domain/build.gradle` now declares the minimal JUnit 4 dependency used by
    sibling modules.
  - Synthetic test inputs exercise only public classifier APIs and assert public
    `ClassificationResult` fields.
