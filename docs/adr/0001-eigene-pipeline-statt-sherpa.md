# ADR-0001: Eigene vermessene Pipeline statt sherpa-onnx-Kokoro-Frontend

**Status:** angenommen (24.08.2026)
**Kontext-Dokument:** [PROJEKTPLAN.md](../../PROJEKTPLAN.md), Abschnitt 3

## Entscheidung

Der komplette Windows-Pfad (Textregeln → espeak → Phonemregeln → Vokabularfilter → Kokoro) wird selbst portiert; sherpa-onnx wird nicht eingebunden. Das Stopp-Gate liegt in M0

## Begründung

Kokoro lief schon einmal via sherpa auf Android und wurde wegen des zerstörten ich-Lauts verworfen; das Modell spricht mit espeak nachweislich sauber — der Fehler lag im Integrationsweg (Verdacht tokens.txt-Filterung, durch Quelltextlektüre gestützt). sherpa kann zudem kein ja und löst zh per Greedy-Lexikon statt Segmentierung

## Verworfene Alternativen

sherpa-onnx-AAR (schneller Start, aber genau der belegte Irrweg); piper-phonemize (eingefroren, keine Android-Builds)
