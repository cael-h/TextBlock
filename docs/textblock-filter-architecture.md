# TextBlock Filter Architecture

## Repo Strategy

TextBlock is a fork-style app built from upstream QUIK. Keep upstream code in this
checkout and make TextBlock changes on project branches, so upstream fixes can be
rebased or merged later.

Current baseline:

- Upstream: `https://github.com/quik-sms/quik`
- Branch: `textblock-filter-foundation`
- Baseline commit: `92004691 chore(repo): Delete app/build/outputs/apk/debug/app-debug.apk`

## Product Goal

TextBlock should be the default SMS/MMS app. It should inspect inbound messages
before notification and route unwanted political spam out of the normal inbox
without relying on cloud services.

Primary behavior:

- Normal messages stay in the regular conversation flow.
- Political spam is suppressed from normal notifications.
- Filtered messages remain reviewable in a blocked/quarantine area unless the
  user enables hard drop.
- The user can correct false positives and false negatives.

## Existing QUIK Hooks

QUIK already has the right receive path for this:

- `data/src/main/java/com/moez/QKSMS/receiver/SmsReceivedReceiver.kt`
  receives `SMS_DELIVER`, persists the inbound SMS, and can apply the
  TextBlock receive policy before enqueueing SMS worker work.
- `data/src/main/java/com/moez/QKSMS/worker/ReceiveSmsWorker.kt`
  evaluates blocked senders, content filters, and the TextBlock receive policy
  before notification as a fallback for allowed/pre-filter-failed SMS.
- `data/src/main/java/com/moez/QKSMS/receiver/MmsReceivedReceiver.kt`
  hands downloaded MMS work to a worker.
- `data/src/main/java/com/moez/QKSMS/worker/ReceiveMmsWorker.kt`
  evaluates blocked senders and content filters before notification.
- `data/src/main/java/com/moez/QKSMS/repository/MessageContentFilterRepositoryImpl.kt`
  already implements user-managed body filters.

TextBlock should add a new classifier layer beside the existing content-filter
repository instead of rewriting the receive pipeline.

The initial classifier boundary lives in:

- `domain/src/main/java/com/moez/QKSMS/textblock/InboundMessageClassifier.kt`
- `domain/src/main/java/com/moez/QKSMS/textblock/RuleBasedPoliticalClassifier.kt`

## Proposed Classifier Boundary

Add a domain-level classifier interface:

```kotlin
interface InboundMessageClassifier {
    fun classify(message: InboundMessageForClassification): ClassificationResult
}
```

Suggested result shape:

```kotlin
data class ClassificationResult(
    val action: FilterAction,
    val category: FilterCategory,
    val confidence: Float,
    val reason: String?
)
```

Suggested actions:

- `ALLOW`
- `QUARANTINE`
- `BLOCK_CONVERSATION`
- `DROP`

Suggested categories:

- `POLITICAL`
- `FUNDRAISING`
- `SCAM`
- `UNKNOWN`

The SMS receiver should call this classifier before starting WorkManager when it
has the full SMS body, so filtered SMS does not trigger Android's expedited
worker foreground notification. Receive workers still call the classifier before
normal notification creation as a fallback and for MMS, where the message must be
downloaded first. Notification creation must also refuse blocked/quarantined
conversations.

## First Implementation Pass

Do not start with Gemma. Start with a deterministic local classifier:

- sender reputation rules
- political fundraising domains
- opt-out phrases
- campaign disclaimer phrases
- PAC/candidate/committee terms
- repeated-short-code behavior
- user allowlist for contacts

This gets useful blocking quickly and creates labeled examples from corrections.

The first feature milestone should include:

- local classifier interface
- rule-based political classifier implementation
- settings toggle for TextBlock political filtering
- quarantine/blocked routing behavior
- correction actions: "Not spam" and "Block similar"
- tests for classification rules

## On-Device LLM Adapter

LLM filtering should be a later adapter behind `InboundMessageClassifier`.
Do not put LLM runtime calls directly in `ReceiveSmsWorker` or
`ReceiveMmsWorker`.

The domain adapter seam lives under:

- `domain/src/main/java/com/moez/QKSMS/textblock/llm/LlmClassificationRequest.kt`
- `domain/src/main/java/com/moez/QKSMS/textblock/llm/OnDeviceLlmRuntime.kt`
- `domain/src/main/java/com/moez/QKSMS/textblock/llm/OnDeviceLlmInboundMessageClassifier.kt`
- `domain/src/main/java/com/moez/QKSMS/textblock/llm/NoModelOnDeviceLlmRuntime.kt`
- `domain/src/main/java/com/moez/QKSMS/textblock/llm/NoModelFallbackClassifier.kt`

`OnDeviceLlmInboundMessageClassifier` still implements
`InboundMessageClassifier`, so receive workers keep depending on only that
domain interface. A future Android runtime should implement `OnDeviceLlmRuntime`
and be injected into the adapter from composition code.

The runtime contract is intentionally small and synchronous for now:

```kotlin
interface OnDeviceLlmRuntime {
    fun classify(request: LlmClassificationRequest): OnDeviceLlmRuntimeResult
}
```

Runtime results are explicit:

- `Classified` returns a completed `ClassificationResult`.
- `ModelUnavailable` means no model/runtime is configured or loaded.
- `Deferred` means classification should not happen synchronously.

`ModelUnavailable` and `Deferred` map to `NoModelFallbackClassifier`, which
returns `ALLOW`, `UNKNOWN`, zero confidence, and an explanatory reason. This
keeps phones without a model from blocking or quarantining messages by surprise.

Expected adapter flow:

1. Fast rules run first.
2. Obvious allow/block decisions return immediately.
3. Ambiguous messages call the optional LLM adapter.
4. Missing, unloaded, or deferred model runtime returns the no-model fallback.
5. Future async LLM results update the message state and optionally suppress future
   notifications for similar senders/content.

This prevents slow model startup from blocking SMS receive handling.

Potential Android runtime targets:

- Google AI Edge / LiteRT-LM for Gemma-family local inference.
- MediaPipe LLM Inference API if it remains the simpler supported entry point.
- A no-LLM fallback for lower-end phones.

Future Gemma/LiteRT/MediaPipe integration should stay in an Android/runtime
module and plug in by implementing `OnDeviceLlmRuntime`. If model loading is
asynchronous, return `ModelUnavailable` until the runtime is ready. If inference
needs a later worker, return `Deferred` and add persistence in a separate slice.
Do not store raw message samples for runtime debugging, retries, or tests.

## Privacy Constraints

- No message body leaves the device by default.
- Model prompts should include only the minimal message text, sender metadata,
  and a small local history summary.
- Corrections should be stored locally and exportable.
- Any optional cloud mode must be opt-in and visibly labeled.

## Open Decisions

- Whether filtered political messages should map to QUIK's existing blocked
  conversation UI or a new "TextBlock" quarantine view.
- Whether "drop" should ever be enabled by default. Initial default should be
  quarantine, not delete.
- Whether the model package is bundled, downloaded on demand, or user-supplied.
- Whether message Q&A should operate only on quarantined messages or the full
  local SMS/MMS corpus.
