# Contributing

*(Skeleton — grows with M3 [pronunciation-rule workflow] and M6 [final].)*

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
  logic changes. The full path (issue → reproduction → rule + golden → release)
  is described in `docs/regelwerk.md` (from M3).
- Definition of Done (every milestone): tests green · measured claims carry
  date/device/method · architecture diagrams updated in the same commit ·
  CHANGELOG entry.

## Reporting a pronunciation error

Open an issue with: the word or sentence, what it sounds like now, what it
should sound like (issue template from M3).
