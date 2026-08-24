# espeak-ng-Pin

**Referenzstand: Tag `1.52.0`** (https://github.com/espeak-ng/espeak-ng/releases/tag/1.52.0)

## Herleitung (Aufgabe 0.2, erledigt 24.08.2026)

Die Windows-Referenz phonemisiert über `espeakng-loader 0.2.4` (Abhängigkeit von
kokoro-onnx). Die dort gebündelte `espeak-ng.dll` meldet über `espeak_Info()`:
**„1.52.0"** — ein getaggter Release, kein Zwischenstand.

## Regeln

- Der NDK-Build (Aufgabe 0.4) und der Host-Build für die JVM-Golden-Tests
  bauen BEIDE exakt diesen Tag. Patches (falls je nötig) liegen als Dateien
  neben dieser Notiz und gelten für beide Builds.
- Ob Host-Build == Windows-Referenz-DLL, beweist Golden-Stufe A zeichengenau —
  der Pin ist die Voraussetzung, der Test der Beweis.
- Ein espeak-Update ist ein API-relevantes Ereignis (Phonemketten können sich
  ändern): eigener PR, alle Goldens neu begründet, CHANGELOG-Eintrag.
