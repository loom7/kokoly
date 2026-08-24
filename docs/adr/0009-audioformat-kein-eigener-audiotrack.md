# ADR-0009: Audioformat: `start(24000, ENCODING_PCM_16BIT, 1)`, kein eigener AudioTrack, keine Wakelocks

**Status:** angenommen (24.08.2026)
**Kontext-Dokument:** [PROJEKTPLAN.md](../../PROJEKTPLAN.md), Abschnitt 3

## Entscheidung

Audioformat: `start(24000, ENCODING_PCM_16BIT, 1)`, kein eigener AudioTrack, keine Wakelocks

## Begründung

Kokoro liefert 24 kHz mono; einzige Wandlung float→int16. RHVoice ruft exakt dieses Format. Framework-Backpressure taktet die Synthese zwischen den Runs; die Run-Granularität selbst regelt die Segmentierung (2.2, M3)

## Verworfene Alternativen

Eigene Wiedergabe; Resampling; PCM_FLOAT (doppelte Puffer ohne Nutzen)
