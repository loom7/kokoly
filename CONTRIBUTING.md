# Contributing

## Language policy (ADR-0007)

The README and this file are English; the in-depth documentation under `docs/`
is German — this is a German-first project and the pronunciation rules target
German speech. Issues and PRs are welcome in either language.

## Licensing of contributions

Inbound = outbound: contributions are accepted under **GPL-3.0-or-later**, the
project license. Every commit must carry a DCO sign-off
(`git commit -s`, "Signed-off-by"). No CLA.

## Ground rules

- Conventional Commits.
- No PR is merged without green CI (JVM tests + lint).
- New pronunciation rule = one table entry + one golden test — never silent
  logic changes. The full path (issue → reproduction → measurement where
  stress is involved → rule + golden → device listening check → release) is
  described in [`docs/regelwerk.md`](docs/regelwerk.md). Rules newer than the
  frozen Windows reference go through the golden-writer path documented there.
- Definition of Done (every milestone): tests green · measured claims carry
  date/device/method · architecture diagrams updated in the same commit ·
  CHANGELOG entry.

## Reporting a pronunciation error

Use the issue template "Ausspracheregel melden": the word, a full example
sentence (pronunciation is judged in sentence context, never on the isolated
word), what it sounds like now, and what it should sound like.
