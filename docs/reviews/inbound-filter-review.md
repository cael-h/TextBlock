# Inbound Filter Slice Review

Worktree: `/data/data/com.termux/files/home/projects/TextBlock/quik-textblock-inbound-filter`
Branch: `textblock-inbound-filter`
Base commit: `fa5ac347`

## Findings

### High: Classified MMS returns before MMSC acknowledgement

`data/src/main/java/com/moez/QKSMS/worker/ReceiveMmsWorker.kt:206-215` returns
immediately when TextBlock suppresses an MMS. That return happens before the MMS
acknowledgement calls at `ReceiveMmsWorker.kt:252-256`, so a successfully
downloaded and synced classified MMS can skip `sendAcknowledgeInd(...)` and
`sendNotifyRespInd(...)`.

The existing blocked-conversation branch also has an early success return, but
this slice expands that behavior to every TextBlock-classified MMS. For carrier
MMS flows, skipping acknowledgement can lead to duplicate delivery attempts or
incorrect MMSC state.

Suggested fix: suppress the notification without returning until after the
ack/notify calls, or restructure the MMS worker so all persisted download paths
acknowledge before exiting.

## Open Questions / Assumptions

- Assumed the MVP intentionally maps `QUARANTINE`, `BLOCK_CONVERSATION`, and
  `DROP` to the existing blocked-conversation path without deleting the message,
  matching `docs/slices/inbound-filter-plan.md:30-57`.
- The quarantine-as-blocked behavior marks the whole thread blocked and read at
  `ReceiveSmsWorker.kt:111-116` and `ReceiveMmsWorker.kt:209-214`. That is
  consistent with the plan, but it means one classified message can suppress
  future conversation notifications until a dedicated quarantine model/UI exists.
- No TextBlock-specific `deleteMessages(...)` path was found. Existing deletion
  paths remain the pre-existing blocked-sender drop and user content-filter
  behavior.

## Test Gaps

- `git diff --check` passed for the reviewed inbound-filter worktree.
- No worker tests exist for these receive paths, and this slice did not add a
  WorkManager/Realm/Android test harness.
- Android compile verification was not completed in this Termux environment
  because the Android SDK is not configured. This is a verification gap, not an
  implementation finding by itself.
- The highest-value missing tests are: SMS allow/quarantine branching, MMS
  allow/quarantine branching with acknowledgement still sent, and DI creation of
  receive workers with `InboundMessageClassifier` assigned.

## Summary

The DI wiring is structurally consistent: `AppModule` provides
`InboundMessageClassifier`, `InjectionWorkerFactory` receives it, and both SMS
and MMS workers get the dependency. The receive paths avoid deleting
TextBlock-classified messages by default. The MMS early return before carrier
acknowledgement should be addressed before merge.
