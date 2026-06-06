# Local Correction Data Boundary Review

Review target: `/data/data/com.termux/files/home/projects/TextBlock/quik-textblock-corrections` on branch `textblock-corrections`, reviewed against base commit `645cd228`.

## Findings

No findings.

## Open Questions / Assumptions

- I treated the worker's recorded Gradle SDK failure in `docs/slices/textblock-corrections-plan.md:78-83` as stale. The focused correction tests pass with the current local SDK shim.
- I assumed this slice is intentionally domain-only and not expected to persist corrections or affect live receive/classification behavior yet, consistent with `docs/slices/textblock-corrections-plan.md:5-8` and `docs/slices/textblock-corrections-plan.md:26-29`.

## Test Gaps / Residual Risk

- There is no persistence implementation yet, so storage migration, file/database serialization, and concurrent update behavior are untested.
- Hashes remain sensitive for low-entropy text and predictable sender addresses; this is documented in `docs/slices/textblock-corrections-plan.md:64-74`, but future persistence still needs a privacy review.
- The correction store is not composed with `InboundMessageClassifier`, so precedence over model/rule results is tested only at the local store boundary.

## Verification

- `git diff --check 645cd228` passed for the slice.
- `ANDROID_HOME=/data/data/com.termux/files/home/android-sdk-termux ./gradlew :domain:testDebugUnitTest --tests 'dev.octoshrimpy.quik.textblock.correction.*' -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2` passed.

## Behavior Review Notes

- Records store only an action, typed hash key, and timestamp in `CorrectionModels.kt:39-43`; no raw body field exists.
- Body, token, and sender keys are typed SHA-256 values in `CorrectionModels.kt:18-27`.
- Body-derived inputs are normalized and hashed in `CorrectionSignals.kt:41-58`; token signatures are normalized, de-duplicated, sorted, and hashed in `CorrectionSignals.kt:77-83`.
- `NOT_SPAM` force allow is resolved before `BLOCK_SIMILAR` in `CorrectionStore.kt:40-51`.
- Tests use synthetic fixtures and cover deterministic keys, token signature matching, Not Spam precedence, and no raw-body exposure in `CorrectionSignalsTest.kt:22-83` and `InMemoryCorrectionStoreTest.kt:20-109`.

## Summary

The correction boundary is privacy-preserving for this domain-only slice and has focused deterministic tests. The next risk is in the future persistence/integration slice, not in the current in-memory boundary.
