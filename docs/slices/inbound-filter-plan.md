# Inbound Filter Integration Plan

## Scope

Wire the existing domain `InboundMessageClassifier` into inbound SMS/MMS worker
processing without changing settings UI, quarantine UI, app identity, or
classifier rules.

## Current Receive Flow

- SMS: `ReceiveSmsWorker` loads the persisted message by id, applies existing
  blocked-sender logic, then applies user content filters before updating the
  conversation and posting notifications.
- MMS: `ReceiveMmsWorker` persists and syncs the downloaded MMS, applies active
  conversation read handling, applies existing blocked-sender logic, then
  applies user content filters before updating the conversation and posting
  notifications.
- Worker dependencies are provided by `InjectionWorkerFactory`, which
  reflectively creates workers and manually assigns dependencies supplied by
  Dagger through `AppModule`.

## Integration Boundary

The clear MVP boundary is to run the TextBlock classifier after existing
blocked-sender checks and existing user content filters, and before
conversation notification updates. That preserves current block/drop behavior
and user-managed content filters while ensuring classified messages do not
reach normal notification creation.

`QUARANTINE` should not delete by default. The safest existing app behavior that
already suppresses notifications is QUIK's blocked-conversation path. Using it
as quarantine means filtered messages remain in the existing blocked messages
area rather than a dedicated TextBlock quarantine view. This is a product
tradeoff: it is reviewable and low-risk for MVP, but it conflates TextBlock
classification with user/sender blocking until a dedicated quarantine model/UI
exists.

## Planned Changes

- Add a Dagger provider from `InboundMessageClassifier` to
  `RuleBasedPoliticalClassifier`.
- Add `InboundMessageClassifier` to `InjectionWorkerFactory` and assign it to
  `ReceiveSmsWorker` and `ReceiveMmsWorker`.
- Add classifier dependency fields to both receive workers.
- Build `InboundMessageForClassification` from `Message.getText()`,
  `Message.address`, `Message.date`, `Message.isMms()`, `ContactRepository`,
  and `Message.subId`.
- Call `ConversationRepository.updateConversations()` before TextBlock
  quarantine marking so the existing blocked-conversation path has a
  conversation row to update.
- Handle classifier actions:
  - `ALLOW`: continue unchanged.
  - `QUARANTINE` / `BLOCK_CONVERSATION`: mark the thread read and mark the
    conversation blocked with a TextBlock reason; do not delete the message.
  - `DROP`: document as intentionally not honored in this MVP receive path
    unless an explicit user drop setting exists for TextBlock. Treat as
    quarantine to avoid delete-by-default behavior.
- Keep existing blocked-sender drop and content-filter deletion behavior
  untouched.

## Verification Plan

- Inspect all receive paths after edits to ensure TextBlock classification does
  not call `deleteMessages`.
- Add or identify focused tests where feasible. Existing worker tests are not
  present, so unit tests may require substantial Android/WorkManager/Realm
  scaffolding.
- Run a focused Gradle compile/test command only if Java is available.

## Verification Results

- Focused worker unit tests were identified as not feasible in this slice
  without adding Android/WorkManager/Realm scaffolding; no existing worker test
  harness is present.
- `./gradlew :data:compileDebugKotlin :presentation:compileDebugKotlin` was
  attempted with Java 17 available, but failed before compilation because the
  Android SDK location is not configured. The build requires `ANDROID_HOME` or
  `local.properties` with `sdk.dir`.
- `git diff --check` passed.
- Static inspection found no TextBlock `deleteMessages` path. The remaining
  worker deletion calls are existing blocked-sender drop and user content-filter
  behavior.

## Review Response

- Addressed review finding: classified MMS no longer returns before
  `sendAcknowledgeInd(...)` and `sendNotifyRespInd(...)`.
- TextBlock-classified MMS now marks the thread read and marks the conversation
  blocked, then skips the normal notification/shortcut/badge work without
  entering the generic `conversation.blocked` early-return branch.
- Existing non-TextBlock early returns were left unchanged, including existing
  blocked-conversation and content-filter behavior.
- Review-response verification: `git diff --check` passed, and static scans
  confirmed the TextBlock MMS branch no longer returns before the ACK/notify
  calls.

## Checklist

- [x] Inspect receive workers and dependency injection.
- [x] Confirm message fields needed to construct classifier input.
- [x] Implement classifier injection if the boundary remains clear.
- [x] Implement SMS classifier decision before notification.
- [x] Implement MMS classifier decision before notification.
- [x] Add or identify focused tests.
- [x] Run verification or document build blocker.
- [x] Re-inspect for no TextBlock delete-by-default path.
