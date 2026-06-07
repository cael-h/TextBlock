# TextBlock Quarantine Review Actions Plan

## Goal

Add correction actions to the existing Blocking > Blocked messages review
surface so selected blocked conversations can be marked as Not spam or used to
Block similar future messages. Keep the work scoped to the existing toolbar
selection flow and avoid classifier policy/provider changes owned by the
correction-policy slice.

## UI Plan

- Reuse `BlockedMessagesController`, `BlockedMessagesPresenter`,
  `BlockedMessagesView`, `BlockedMessagesAdapter`, and
  `blocked_messages.xml`.
- Show Not spam and Block similar toolbar actions only when one or more blocked
  conversations are selected, matching Delete and Unblock visibility.
- Use existing selection semantics: selected values are conversation/thread ids.
- When an action is chosen, derive correction signals from the last incoming
  local message in each selected conversation.
- If no selected conversation has usable incoming text, show a short user-facing
  message and keep the list unchanged.
- After a successful correction action, clear selection and show a result count.
- Keep the existing Unblock action separate. Not spam records a correction
  signal; it does not automatically unblock the conversation in this slice.

## Integration Plan

- Use existing public domain APIs:
  - `CorrectionAction`
  - `CorrectionSignals`
  - `CorrectionStore`
- Add a small presentation helper in the blocked-message feature package to:
  - query `MessageRepository.getLastIncomingMessage(threadId)`;
  - choose the latest incoming message with non-blank local text;
  - derive `CorrectionSignals.from(address, body)`;
  - save the selected correction action;
  - return saved/skipped counts for presenter/UI feedback.
- Do not change `domain/src/main/java/com/moez/QKSMS/textblock/**`.
- Do not change receive workers or classifier decision behavior.
- Keep `AppModule.kt` wiring minimal if the branch lacks a final persistent
  correction provider. The local branch provides a singleton `CorrectionStore`
  so UI corrections write through Dagger instead of a private helper store.
  The correction-policy merge should connect that same store to the final
  correction-aware classifier/persistence shape.

## Checklist

- [x] Write this plan before code edits.
- [x] Inspect blocked-message presenter/controller/view/adapter and menus.
- [x] Add blocked-message correction action helper.
- [x] Add toolbar menu items and strings.
- [x] Wire Not spam and Block similar presenter actions.
- [x] Add user feedback for saved and unavailable corrections.
- [x] Add/update nearby tests if a presenter/unit pattern exists; otherwise
      document compile-only verification.
- [x] Run required Kotlin compile command.
- [x] Run `git diff --check`.
- [x] Update this document with verification results and pending integration.
- [x] Commit the slice.

## Risks

- The current branch has correction domain models and a Dagger singleton
  in-memory store, but no visible persisted correction provider or
  receive-worker consumption path. This slice must avoid duplicating the
  correction-policy worker's final classifier/persistence shape.
- Block similar uses token-signature matching and can be broader than exact body
  matching. The UI should label it clearly and only derive signals from local
  text that already exists on device.
- MMS conversations without loaded text parts, outgoing-only conversations, or
  blank bodies cannot supply useful correction signals and should be skipped.
- Keeping Not spam separate from Unblock may surprise users who expect both.
  Combining those behaviors would change blocking state and is deferred unless
  product direction requires it.

## Tests And Verification Plan

- Prefer focused JVM/presenter tests only if a nearby presentation test pattern
  exists.
- Always run:
  `ANDROID_HOME=/data/data/com.termux/files/home/android-sdk-termux ./gradlew :presentation:compileDebugKotlin -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2`
- Run any focused tests added for this slice.
- Run `git diff --check`.

## Verification Results

- No focused presentation test was added because this module has no existing
  `presentation/src/test` files or nearby presenter tests, and the new helper
  reads Realm-backed `MessageRepository` results.
- Required compile passed:
  `ANDROID_HOME=/data/data/com.termux/files/home/android-sdk-termux ./gradlew :presentation:compileDebugKotlin -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2`
- The compile emitted existing unrelated Android resource/Kotlin warnings and
  recovered from a Kotlin daemon crash by using Gradle's fallback compiler.
- `git diff --check` passed.

## Review Finding Status

- Resolved: the high-severity review finding about corrections being saved to a
  private helper store was valid.
- `TextBlockCorrectionReview` now injects `CorrectionStore` through Dagger and
  no longer allocates `InMemoryCorrectionStore()` locally.
- `AppModule` provides a singleton `CorrectionStore` for this branch so the UI
  writes through the same app graph entry point that Slice J's correction-aware
  classifier can read after merge.
- Follow-up required compile passed:
  `ANDROID_HOME=/data/data/com.termux/files/home/android-sdk-termux ./gradlew :presentation:compileDebugKotlin -Pandroid.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2`
- Follow-up `git diff --check` passed.

## Pending Integration

- `TextBlockCorrectionReview` uses the existing public `CorrectionStore` API
  with the branch's Dagger-provided singleton in-memory implementation so this
  UI slice compiles without classifier policy changes.
- The correction-policy slice still needs to supply the final persistent
  correction store and connect saved correction records to classifier decisions.
- This slice intentionally does not change receive workers, classifier policy,
  or database schema.

## Status

- Planning complete.
- Existing blocked-message review surface inspected.
- UI implementation complete.
- Required compile verification passed.
- `git diff --check` passed.
- Review finding fix compile verification passed.
- Commit complete.
