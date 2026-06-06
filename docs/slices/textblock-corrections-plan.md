# Local Correction Data Boundary Plan

## Scope

Add a domain-level correction API for future TextBlock actions such as Not spam
and Block similar. The slice should stay local, deterministic, and testable
without receive-worker, settings, UI, model-runtime, or Android persistence
changes.

## Privacy Boundary

- Do not persist raw message bodies in correction data, repository data, or test
  fixtures.
- Build correction keys from sanitized signals only.
- If body-derived matching is needed, hash normalized body-derived inputs with a
  standard-library SHA-256 implementation.
- Document exactly which inputs are hashed so a future persistence layer can
  preserve the same boundary.

## File Boundary

- Add production APIs under
  `domain/src/main/java/com/moez/QKSMS/textblock/correction/`.
- Add JVM unit tests under
  `domain/src/test/java/dev/octoshrimpy/quik/textblock/correction/`.
- Leave receive workers, UI, settings, LLM/runtime APIs, and persistence modules
  untouched.
- Leave `domain/build.gradle` unchanged unless additional test dependencies are
  necessary.

## Planned Concepts

- `CorrectionAction`: the user action that created a local correction, such as
  `NOT_SPAM` or `BLOCK_SIMILAR`.
- `CorrectionKey`: a typed, privacy-preserving key, such as a body fingerprint,
  token signature, or sender fingerprint.
- `CorrectionSignals`: deterministic sanitized keys derived from an
  `InboundMessageForClassification`.
- `CorrectionDecision`: the local decision a caller can compose with
  `InboundMessageClassifier`, such as forcing allow for Not spam or quarantine
  for Block similar.
- `CorrectionStore`: an interface for saving and resolving corrections.
- `InMemoryCorrectionStore`: a small testable implementation for this slice.

## Hash Inputs

- Body fingerprint: SHA-256 over the versioned normalized message body.
- Token signature: SHA-256 over a versioned sorted/distinct token list derived
  from the normalized body.
- Sender fingerprint: SHA-256 over the versioned normalized sender address.

These keys are deterministic for matching, but they are not raw body storage and
should be treated as local private data if persisted later.

## Tests

- Same synthetic message yields the same body and token keys.
- Formatting and Unicode compatibility variants normalize to the same keys.
- Store resolves Not spam by exact body fingerprint.
- Store resolves Block similar by token signature.
- Not spam takes precedence over Block similar when both match.
- Stored records do not expose raw message bodies.

## Risks

- Hashes can still be sensitive if persisted or exfiltrated, especially for
  low-entropy text. Future persistence should keep this data local and avoid
  syncing it.
- Token signatures are intentionally broad enough for Block similar, so they may
  over-match. This slice exposes the behavior but does not wire it into inbound
  classification yet.
- Phone-number hashes can be correlated if a caller uses predictable unsalted
  inputs. This slice avoids persistence and documents the key type for future
  storage review.

## Verification Results

- `git diff --check` passed from the assigned worktree.
- `./gradlew :domain:testDebugUnitTest` was attempted, but Gradle failed before
  compilation because no Android SDK path is configured. The worktree needs
  `ANDROID_HOME` or `local.properties` with `sdk.dir`.
- `kotlinc` is not installed in this environment, so a standalone Kotlin
  compiler check was not available.
- Static scan found no production storage for raw message bodies. The focused
  test fixtures use synthetic message text only.

## Checklist

- [x] Inspect existing TextBlock domain classes and tests.
- [x] Write this slice plan before coding.
- [x] Add correction domain API.
- [x] Add in-memory store.
- [x] Add focused synthetic JVM tests.
- [x] Run `git diff --check`.
- [x] Attempt focused Gradle tests or document blocker.
- [x] Review diff for raw body persistence or fixture leakage.
