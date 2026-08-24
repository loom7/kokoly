# ADR-0013: Referenzhoheit und Golden-Lebenszyklus

**Status:** angenommen (24.08.2026)
**Kontext-Dokument:** [PROJEKTPLAN.md](../../PROJEKTPLAN.md), Abschnitt 3

## Entscheidung

Bis M3-Abnahme ist die Windows-Pipeline die Referenz; ihre Goldens entstehen zweistufig (Stufe A: Roh-espeak-Ausgabe vor Lautersatz/Filter; Stufe B: Endphonemkette). Mit M3-Abnahme werden die Kotlin-Tabellen die einzige gepflegte Wahrheit: die Windows-Ära-Goldens werden eingefroren (`app/src/test/resources/golden/frozen-windows/`, nie regeneriert), der Windows-Stand wird als Archiv gesichert und nicht weitergepflegt (kein Regel-Rückfluss). Neue Goldens erzeugt ein kleiner Golden-Writer (JVM-Werkzeug) aus der Kotlin-Pipeline; die erwartete Phonemkette wird im PR aus Roh-espeak-Ausgabe + Regelwirkung hergeleitet und begründet (Review-Pflicht), nicht blind aus dem Ist-Output kopiert. Nur die Roh-espeak-Stufe A bleibt dauerhaft plattformverglichen (gepinnter Commit beidseitig). Golden-Generator und Mess-Skripte (K-Kennzahl, F0, Stapelnaht) wandern als Python-Werkzeuge ins neue Repo (`scripts/golden/`, `scripts/messung/`)

## Begründung

Ohne diese Regelung erzeugt der Windows-Generator ab der ersten Kotlin-only-Regel falsche Goldens — Doppelpflege oder stille Divergenz. Ein OpenSource-Projekt darf nicht auf einen unversionierten Privatordner zeigen

## Verworfene Alternativen

Windows-App dauerhaft mitpflegen (zwei Wahrheiten); Goldens nur aus Ist-Output (Test prüft dann nur Selbstkonsistenz — abgemildert durch die Herleitungspflicht)
