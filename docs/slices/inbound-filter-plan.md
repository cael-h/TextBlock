# Inbound Filter Integration Plan

## Scope

Wire the existing domain `InboundMessageClassifier` into inbound SMS/MMS worker
processing without changing settings UI, quarantine UI, app identity, or
classifier rules.

## Current Receive Flow

- SMS: `SmsReceivedReceiver` persists the inbound message and now runs the
  TextBlock receive policy before enqueueing `ReceiveSmsWorker`. Classified
  quarantine/drop SMS messages are handled there so Android does not show the
  expedited worker foreground notification for messages the user will not see.
  Allowed SMS messages still enqueue `ReceiveSmsWorker`, which keeps the
  worker-side classifier as a fallback after blocked-sender and user content
  filter checks.
- MMS: `ReceiveMmsWorker` persists and syncs the downloaded MMS, applies active
  conversation read handling, applies existing blocked-sender logic, then
  applies user content filters before updating the conversation and posting
  notifications.
- Worker dependencies are provided by `InjectionWorkerFactory`, which
  reflectively creates workers and manually assigns dependencies supplied by
  Dagger through `AppModule`.

## Integration Boundary

The clear MVP boundary is to run the TextBlock classifier before conversation
notification updates. SMS also runs the classifier once in the receiver before
WorkManager starts, because expedited receive workers can show a foreground
notification before worker code reaches the classifier. MMS still classifies in
the worker because the message payload must be downloaded and persisted first.

`NotificationManagerImpl.update(threadId)` also refuses to post notifications
for blocked conversations, so later update paths cannot surface a quarantined
conversation by accident.

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
- Add classifier dependencies to `SmsReceivedReceiver` and classify SMS before
  enqueueing the receive worker when TextBlock can make a local decision.
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
  - `DROP`: delete the received message only when the user has explicitly set
    TextBlock filter mode to drop.
- Keep existing blocked-sender drop and content-filter deletion behavior
  untouched.
- Add a notification-manager guard that cancels and returns for blocked
  conversations with unread/unseen messages.

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
- `git diff --check` passed.
- `ANDROID_HOME=/data/data/com.termux/files/home/android-sdk-termux ./gradlew
  --no-daemon :presentation:assembleRelease
  -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2`
  passed.
- Release lint still reports pre-existing `ExtraTranslation` errors for
  `compose_send_group_summary` and `scheduled_options`; these did not fail
  `assembleRelease`.
- Static inspection confirmed classified SMS quarantine/drop paths return
  without enqueueing `ReceiveSmsWorker`, allowed SMS still uses the existing
  worker path, and pre-worker classification exceptions fall back to the worker.
- TextBlock `deleteMessages` is now active only for the explicit TextBlock drop
  effect, which requires the user-selected drop filter mode. Default quarantine
  marks the thread read and blocked instead.

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
- [x] Implement pre-worker SMS classifier decision to suppress the receive
  worker foreground notification for classified SMS.
- [x] Implement MMS classifier decision before notification.
- [x] Guard notification updates for blocked/quarantined conversations.
- [x] Add or identify focused tests.
- [x] Run verification or document build blocker.
- [x] Re-inspect for no TextBlock delete-by-default path.
