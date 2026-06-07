# Correction-Aware Classifier Policy Plan

## Scope

Add a correction-aware `InboundMessageClassifier` wrapper in the TextBlock
domain layer. The wrapper should resolve local correction signals before calling
the base classifier, so existing classification behavior remains unchanged when
no correction matches.

This slice stays within domain classifier and correction code, with optional DI
wiring only if needed to expose the wrapper as the app classifier.

## Current Baseline

- `RuleBasedPoliticalClassifier` is the current app-provided
  `InboundMessageClassifier`.
- `CorrectionSignals` derives privacy-preserving keys from inbound message
  address and body.
- `InMemoryCorrectionStore` resolves `NOT_SPAM` before `BLOCK_SIMILAR`.
- Correction records contain `CorrectionAction`, `CorrectionKey`, and timestamp
  only; no raw body field exists.

## Implementation Plan

1. Add a domain classifier wrapper that implements `InboundMessageClassifier`.
2. On each classification request, derive `CorrectionSignals` from the inbound
   message and resolve them with `CorrectionStore`.
3. For `CorrectionDecisionOutcome.FORCE_ALLOW`, return an allow
   `ClassificationResult` without consulting the base classifier.
4. For `CorrectionDecisionOutcome.BLOCK_SIMILAR`, return a blocking or
   quarantine `ClassificationResult` without consulting the base classifier.
5. For `CorrectionDecisionOutcome.NO_MATCH`, delegate exactly to the base
   classifier and return its result.
6. Add focused tests with a fake base classifier to prove delegation and
   short-circuit behavior.
7. Add a privacy regression test that saved correction records do not contain
   raw message text.
8. Wire the app provider only if a suitable correction store can be provided
   without creating durable schema or touching receive-worker code.

## Classification Mapping

- `NOT_SPAM`: returns an allow `ClassificationResult`, forcing allow before
  base classifier output.
- `BLOCK_SIMILAR`: returns a `ClassificationResult` with
  `FilterAction.QUARANTINE`, `FilterCategory.UNKNOWN`, and full confidence.
  This produces a blocking/quarantine classification while leaving final drop
  behavior to `TextBlockFilterPolicy`.
- `NO_MATCH`: returns the base classifier output unchanged.

## Privacy Boundary

- Do not add raw message body fields to `CorrectionRecord` or any new correction
  storage type.
- Use `CorrectionSignals.from(message)` for matching so body-derived data stays
  hashed.
- Tests should use synthetic message text and assert correction records expose
  only typed SHA-256 keys.

## Risks

- The in-memory store is not durable, so app-level correction behavior will not
  survive process death until a persistence slice exists.
- Token-signature matching for `BLOCK_SIMILAR` can over-match by design; the
  wrapper should keep the result reason explicit for auditability.
- If the app provider is wrapped with a fresh in-memory store, there is no UI or
  worker integration yet to populate it. That is acceptable for this slice if
  future correction entry points share the same injected store.

## Tests

- No-match correction decision delegates to the base classifier result.
- `NOT_SPAM` match returns allow and does not call the base classifier.
- `BLOCK_SIMILAR` match returns quarantine/blocking classification and does not
  call the base classifier.
- Saved correction records do not include raw message body text.

## Verification Commands

- `ANDROID_HOME=/data/data/com.termux/files/home/android-sdk-termux ./gradlew :domain:testDebugUnitTest --tests 'dev.octoshrimpy.quik.textblock.*' -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2`
- `git diff --check`
- `git diff --cached --check`

## Checklist

- [x] Inspect existing TextBlock classifier, correction, tests, and DI patterns.
- [x] Write this slice plan before implementation.
- [x] Add correction-aware classifier wrapper.
- [x] Add focused classifier policy unit tests.
- [x] Confirm correction records remain privacy-preserving.
- [x] Wire classifier/provider if needed without touching receive workers.
- [x] Run focused unit tests or document blocker.
- [x] Run `git diff --check`.
- [x] Review final diff for scope and unrelated changes.
- [x] Commit slice changes on `textblock-correction-policy`.

## Status

Complete. Slice changes are ready in the local commit.

## Verification Results

- Passed:
  `ANDROID_HOME=/data/data/com.termux/files/home/android-sdk-termux ./gradlew :domain:testDebugUnitTest --tests 'dev.octoshrimpy.quik.textblock.*' -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2`
- Passed:
  `ANDROID_HOME=/data/data/com.termux/files/home/android-sdk-termux ./gradlew :presentation:compileDebugKotlin -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2`
- Passed: `git diff --check`
- Passed: `git diff --cached --check`
