# Drive Sample Identifiers

Extracted from the user-provided Google Drive folder on 2026-06-02. This file
intentionally records identifiers and rule signals, not raw message bodies.

## Scope

- 17 text exports inspected.
- 1 exact duplicate ignored.
- Several conversations included MMS media placeholders.
- All inspected samples were political campaign, PAC, petition, or donation
  messages.

## Strong Domains

- `ak4m.org`
- `bldg-blue.org`
- `brooks4pa.com`
- `dasspac.org`
- `demconservationalliance.org`
- `fight-fascism.org`
- `got-pac.org`
- `jg4co.org`
- `jp4tn.org`
- `mm4ca.org`
- `nl4sc.com`
- `prochoicemaj.com`
- `ro4congress.org`
- `senmajority.com`
- `takeit-back.org`
- `w-2l.org`

## Link Patterns

- `/l/`
- `/actblue_secure_`
- query-string tracking such as `?t=...`

## Message Markers

- `End2End` footer appeared across the sample set.
- Exported MMS messages can contain `[image/png]`, `[image/jpeg]`, and `null`
  lines that should be stripped before scoring.
- Some messages use stylized Unicode letters, so classification should apply
  compatibility normalization before keyword matching.

## Language Clusters

- fundraising: `chip in`, `pitch in`, `rush even just`, `donation`,
  `contribution`, dollar amounts, `fundraising`
- election/race: `primary election`, `special election`, `Election Day`,
  `polls open`, `tossup`, district codes like `PA-07` or `SC-01`
- PAC/campaign: `PAC`, `grassroots campaign`, `Democratic nominee`,
  `House majority`, `Senate majority`
- petition: `add your name`, `petition`, `signatures`, `sign your name`,
  `urge`, `take action`
- urgency: `LAST CHANCE`, `EMERGENCY`, `11:59 PM`, `24 hours`, `right now`,
  `tomorrow`

## Classifier Changes

These identifiers were added to:

- `domain/src/main/java/com/moez/QKSMS/textblock/RuleBasedPoliticalClassifier.kt`
