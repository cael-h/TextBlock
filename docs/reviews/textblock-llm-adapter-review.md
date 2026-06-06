# On-Device LLM Adapter Boundary Review

Review target: `/data/data/com.termux/files/home/projects/TextBlock/quik-textblock-llm-adapter` on branch `textblock-llm-adapter`, reviewed against base commit `645cd228`.

## Findings

No findings.

## Open Questions / Assumptions

- I treated the worker's recorded SDK failure in `docs/slices/textblock-llm-adapter-plan.md:96-101` as stale. The focused LLM adapter tests pass with the current local SDK shim.
- I assumed runtime implementations are expected to follow the explicit-result contract in `OnDeviceLlmRuntime.kt:15-30`; the adapter does not catch arbitrary runtime exceptions.

## Test Gaps / Residual Risk

- There is no test for a runtime implementation that throws. A future Android runtime should either honor the no-throw contract or the adapter should defensively map runtime failures to the safe fallback.
- This slice does not integrate the adapter with fast rules or DI, so end-to-end classifier composition remains unverified.
- There is no timing or cold-start test, which is acceptable for the no-model/domain-only seam but must be revisited when a real runtime is added.

## Verification

- `git diff --check 645cd228` passed for the slice.
- `git status --short data/src/main/java/com/moez/QKSMS/worker ...` showed no receive-worker changes in this worktree.
- Static grep found no production network or model-runtime dependencies in the LLM adapter package; only docs mention future Gemma/LiteRT/MediaPipe targets.
- `ANDROID_HOME=/data/data/com.termux/files/home/android-sdk-termux ./gradlew :domain:testDebugUnitTest --tests 'dev.octoshrimpy.quik.textblock.llm.*' -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2` passed.

## Behavior Review Notes

- The runtime seam is small: `OnDeviceLlmRuntime.classify()` accepts `LlmClassificationRequest` and returns `Classified`, `ModelUnavailable`, or `Deferred` in `OnDeviceLlmRuntime.kt:15-30`.
- The default runtime returns `ModelUnavailable` in `NoModelOnDeviceLlmRuntime.kt:13-18`.
- The safe fallback always returns `ALLOW`, `UNKNOWN`, and zero confidence in `NoModelFallbackClassifier.kt:19-29`.
- The adapter passes through classified results and maps unavailable/deferred runtime states to fallback in `OnDeviceLlmInboundMessageClassifier.kt:22-27`.
- Architecture docs keep receive workers behind `InboundMessageClassifier` and direct future Android runtime code into a separate runtime implementation at `docs/textblock-filter-architecture.md:113-171`.

## Summary

The slice keeps model runtime details out of receive workers, adds no external dependencies, and makes no-model/deferred states allow-like. The main follow-up is defensive behavior once a real runtime exists.
