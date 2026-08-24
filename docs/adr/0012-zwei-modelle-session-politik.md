# ADR-0012: Zwei Modelle: Session-Politik

**Status:** angenommen (24.08.2026) — offene Nachträge siehe unten
**Kontext-Dokument:** [PROJEKTPLAN.md](../../PROJEKTPLAN.md), Abschnitt 3

## Entscheidung

Status: offen bis M2a. Deutsch braucht `kokoro-martin.onnx`, die 7 übrigen Sprachen `kokoro-v1.0.onnx` (je ~311 MiB fp32). Kandidaten: (a) LRU-1 mit Session-Swap je Sprachwechsel (RAM-schonend; Preis: voller Ladepfad je Swap — Flash-I/O, Sessionaufbau, Prepacking), (b) beide resident (heiß fp32 realistisch deutlich über dem Ein-Modell-Korridor), (c) Ein-Modell-Hypothese: kann Martin mit den v1.0-Stimmvektoren die anderen Sprachen akzeptabel mitbedienen (grafgleicher Fine-Tune)? Hör-Gate je Sprache, auf Windows vorprüfbar (Aufgabe 2a.0) — falls ja, halbiert sich Download, Flash und RAM auf einen Schlag. Entscheidung per Messung: Swap-Zeit, Swap-mAh, PSS beider Politiken

## Begründung

`setLanguage` über Sprachgrenzen ist Kern von Z1; die Kosten des Wechsels dürfen nicht ungemessen bleiben

## Verworfene Alternativen

Stillschweigend eine Session annehmen (der Fehler des Entwurfs); Martin verwerfen und de aus v1.0 nehmen (v1.0 hat kein Deutsch)
