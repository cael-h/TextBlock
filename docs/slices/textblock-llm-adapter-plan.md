# TextBlock LLM Adapter Slice Plan

## Goal

Add a domain-only adapter seam for optional on-device LLM classification behind
`InboundMessageClassifier`. The app must continue to work without a model, and
receive workers must not depend on Gemma, LiteRT, MediaPipe, or any model runtime
API directly.

## File Boundary

Planned production files:

- `domain/src/main/java/com/moez/QKSMS/textblock/llm/LlmClassificationRequest.kt`
  - Minimal immutable request passed to a future local model runtime.
  - Mirrors only the inbound metadata needed for classification.
- `domain/src/main/java/com/moez/QKSMS/textblock/llm/OnDeviceLlmRuntime.kt`
  - Small synchronous runtime interface.
  - Returns an explicit result state instead of throwing for unavailable models.
- `domain/src/main/java/com/moez/QKSMS/textblock/llm/NoModelOnDeviceLlmRuntime.kt`
  - Deterministic no-model runtime implementation.
  - Always reports that no model is configured.
- `domain/src/main/java/com/moez/QKSMS/textblock/llm/NoModelFallbackClassifier.kt`
  - Deterministic safe fallback classifier.
  - Returns `ALLOW`/`UNKNOWN` with zero confidence so no model absence can block
    messages by surprise.
- `domain/src/main/java/com/moez/QKSMS/textblock/llm/OnDeviceLlmInboundMessageClassifier.kt`
  - `InboundMessageClassifier` adapter that translates domain inbound messages to
    the runtime request and maps unavailable/deferred runtime states to the safe
    fallback.

Planned test files:

- `domain/src/test/java/dev/octoshrimpy/quik/textblock/llm/NoModelFallbackClassifierTest.kt`
- `domain/src/test/java/dev/octoshrimpy/quik/textblock/llm/OnDeviceLlmInboundMessageClassifierTest.kt`

Planned docs:

- `docs/textblock-filter-architecture.md`
  - Update the LLM section with the new adapter seam and future runtime plug-in
    instructions if useful after implementation.

Out of scope:

- Model files or download flows.
- Network inference.
- Android service/runtime lifecycle.
- Settings UI.
- Receive worker changes.
- Correction persistence.
- Raw message sample storage.

## Adapter Contract

`OnDeviceLlmRuntime` will stay small and synchronous for this slice:

- Input: `LlmClassificationRequest`.
- Output: `OnDeviceLlmRuntimeResult`.
- Supported result states:
  - `Classified(ClassificationResult)` for a completed local model result.
  - `ModelUnavailable(reason)` when no runtime/model is configured or loaded.
  - `Deferred(reason)` when a future runtime chooses not to synchronously classify.

The adapter maps `ModelUnavailable` and `Deferred` through
`NoModelFallbackClassifier`, which returns allow-like behavior. This preserves
receive-worker safety until a later slice introduces an explicit async pipeline
with message-state updates.

## Future Gemma/LiteRT/MediaPipe Plug-In Guidance

Future runtime code should implement `OnDeviceLlmRuntime` in an Android/runtime
module and inject it into `OnDeviceLlmInboundMessageClassifier`. It should not:

- Modify `ReceiveSmsWorker` or `ReceiveMmsWorker`.
- Store raw message text for training, debugging, or retries.
- Perform network inference.
- Block receive handling on model download or cold-start.

If Gemma/LiteRT/MediaPipe needs asynchronous initialization, the runtime should
return `ModelUnavailable` until loaded. If inference needs to happen later, it
should return `Deferred`; a later persistence/worker slice can decide how to
apply delayed results.

## Checklist

- [x] Inspect existing TextBlock classifier contracts and docs.
- [x] Write this required plan before coding.
- [x] Add domain LLM runtime request/result boundary.
- [x] Add deterministic no-model runtime and fallback classifier.
- [x] Add `InboundMessageClassifier` adapter implementation.
- [x] Add focused JVM tests for no-model and adapter behavior.
- [x] Update architecture docs with plug-in guidance.
- [x] Run `git diff --check`.
- [x] Try focused domain tests and document blockers if Gradle/Android SDK fails.

## Verification

- `git diff --check` passed.
- `./gradlew :domain:testDebugUnitTest --tests 'dev.octoshrimpy.quik.textblock.llm.*'`
  could not run because this worktree has no Android SDK configured. Gradle
  reported that `ANDROID_HOME` or `local.properties` with `sdk.dir` is required.

## Tests

Focused JVM tests should cover:

- No-model fallback always returns `FilterAction.ALLOW`,
  `FilterCategory.UNKNOWN`, and zero confidence.
- Default adapter with no model returns the deterministic safe fallback.
- Runtime `Deferred` maps to the safe fallback.
- Runtime `Classified` result is passed through unchanged.
- Runtime receives the message metadata expected by future local model
  implementations.

## Risks

- A future runtime may want async inference. This slice keeps the boundary
  synchronous but includes an explicit `Deferred` state so receive workers do not
  need to change.
- A model result could be overconfident or unsafe. Policy tuning can be added at
  the adapter later without touching receive workers.
- The package path uses the existing project convention where files live under
  `com/moez/QKSMS` while the Kotlin package is `dev.octoshrimpy.quik`.
