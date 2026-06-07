# Quarantine Review Actions Review

## Findings

### High - Corrections are saved to a private store that the classifier never reads

`presentation/src/main/java/com/moez/QKSMS/feature/blocking/messages/TextBlockCorrectionReview.kt:25-30` constructs `TextBlockCorrectionReview` with only `MessageRepository` injected, then creates its own private `InMemoryCorrectionStore`. The review actions save corrections through that private instance at `TextBlockCorrectionReview.kt:43-50`.

Slice J wires the live classifier to a Dagger-provided singleton `CorrectionStore` in `presentation/src/main/java/com/moez/QKSMS/injection/AppModule.kt:233-241`. Because Slice K does not inject that `CorrectionStore`, a user can tap "Not spam" or "Block similar" and see a saved-result toast, but future inbound classification will still resolve against a different store and ignore the correction.

Suggested fix: inject `CorrectionStore` into `TextBlockCorrectionReview` and remove the local `InMemoryCorrectionStore()` allocation so Slice K writes into the same store used by `CorrectionAwareInboundMessageClassifier`.

## Review Target

Worktree: `/data/data/com.termux/files/home/projects/TextBlock/quik-textblock-quarantine-actions`
Branch: `textblock-quarantine-actions`
Commit: `e234f28a`
Base: `textblock-filter-foundation` at `32d06eaf`

## Open Questions / Assumptions

- I assumed this slice is intended to record correction signals only, not necessarily unblock or restore the currently selected conversation immediately. If "Not spam" should also unquarantine the selected blocked thread, that behavior is not implemented in `BlockedMessagesPresenter.kt:97-108`.

## Test Gaps / Residual Risk

- No tests were added around `TextBlockCorrectionReview`, so the store-sharing bug above is not caught.
- No presenter/controller tests cover menu item routing, mixed saved/skipped feedback, or selection clearing after correction actions.
- The action derives text from the latest incoming local message through `messageRepo.getLastIncomingMessage(threadId).firstOrNull { it.hasNonWhitespaceText() }` in `TextBlockCorrectionReview.kt:35-39`; this matches the stated intent, but it remains untested for MMS text parts and empty local message bodies.

## Verification

- `git diff --check 32d06eaf..e234f28a` passed.
- Reviewed the changed controller, presenter, view contract, menu, and string resources.

## Summary

The menu actions and feedback plumbing are present, but the correction data is written into an isolated in-memory store. This blocks integration with Slice J until the shared `CorrectionStore` is injected.
