# ADR-0011: Versionierung und öffentliche API der App

**Status:** angenommen (24.08.2026)
**Kontext-Dokument:** [PROJEKTPLAN.md](../../PROJEKTPLAN.md), Abschnitt 3

## Entscheidung

SemVer 2.0.0; `0.x` bis zum stabilen Alltagseinsatz. Öffentliche API: (a) Verhalten gegenüber der Android-TTS-API inkl. Stimmnamen, (b) Format der Regeltabellen, (c) `models.json`-Schema. Bruch von a–c ⇒ MAJOR. CHANGELOG nach Keep a Changelog 1.1.0

## Begründung

SemVer ohne API-Deklaration ist bei Apps hohl

## Verworfene Alternativen

Datumsversionen
