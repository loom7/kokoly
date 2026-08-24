# ADR-0010: Stimmnamen als stabile öffentliche API

**Status:** angenommen (24.08.2026)
**Kontext-Dokument:** [PROJEKTPLAN.md](../../PROJEKTPLAN.md), Abschnitt 3

## Entscheidung

Schema `<bcp47>-<sprechername>`: `de-DE-martin`, `en-US-af_heart`, `hi-IN-hf_alpha` … ASCII, nie umbenennen. Attribute: `QUALITY_HIGH (400)`, `LATENCY_HIGH`, `requiresNetwork=false`; Feature-Schlüssel mit Paketnamen geprägt (deshalb F3 als Gate vor M0). Alle vier Voice-Methoden werden überschrieben

## Begründung

`setLanguage` ist seit API 21 intern über `onGetDefaultVoiceNameFor`+`onLoadVoice` implementiert — eine Logik bedient beide Wege

## Verworfene Alternativen

espeak-Schema (kollidiert bei 42 Stimmen); RHVoice-Schema (nicht selbsterklärend)
