# Kokoly — Arbeitsanweisungen für KI-Sitzungen

Kokoly ist eine Android-System-TTS-Engine (`de.tilly.kokoly.tts`): der 1:1-Port der
vermessenen Kokoro-Windows-Pipeline aus `C:\Users\jtill\Documents\_Claude\TTS Test`.
Plan und Entscheidungen: [PROJEKTPLAN.md](PROJEKTPLAN.md) · Architektur (einziger Ort
der gepflegten Diagramme): [docs/architektur.md](docs/architektur.md) · ADRs:
[docs/adr/](docs/adr/).

## Nur lesen, niemals ändern

- `C:\Users\jtill\Documents\_Claude\TTS Test` — die Windows-Referenz. Ihre Python-
  Regeldateien sind ab M3-Abnahme **eingefroren** (ADR-0013); bis dahin ist sie die
  Wahrheit für Golden-Dateien.
- `C:\Users\jtill\Documents\_Claude\CodeTest` — Nova; Quelle erprobter Android-Muster
  (SupertonicSynthesizer: mmap→OrtSession; EspeakData.ensure(); .ort-Wandlung).

## Die neun Nicht-Verhandelbaren (alle gemessen, Belege in docs/erkenntnisse.md)

1. **Kein sherpa-onnx-Kokoro-Frontend.** Der historische ich-Laut-Fehlschlag („itsch")
   lag im Integrationsweg, nicht im Modell (ADR-0001).
2. **Vokabularfilter meldet jeden Verlust.** Niemals still filtern — das kurze ü hat
   genau so monatelang gefehlt.
3. **espeak: Sprachwechselmarken entfernen** (auf Windows `language_switch=remove-flags`)
   — sonst wird `(en)…(de)` GESPROCHEN. Entfernung nach Sprachcode-Muster NACH der
   Interpunktions-Abtrennung, nie mit `\(.+?\)`.
4. **Betonungszeichen wirkt nur unmittelbar VOR dem Vokal** und nicht satzfinal.
   Prosodie wird im Satzrahmen gemessen, nie am isolierten Wort.
5. **ja/zh niemals über espeak** (ja: Kanji → „Chinese letter"; zh: 19 % Tonverlust).
   Stufe 2, über misaki-äquivalente Wege.
6. **Pausen:** clause_pause unter ~0,13 ist wirkungslos (Modelllücke 120 ms).
   Gesetzt: 0,22 / 0,35 s.
7. **continuous ab 400 Phonemen**, sonst reißt die Stapelnaht (F0-Sprung 87→173 Hz).
8. **Der Vokoder würfelt** (seedlose Zufallsknoten): Ausgabe nie bitgenau vergleichen —
   Golden-Tests prüfen PHONEMKETTEN, Audio prüft das Ohr bzw. F0/Dauer-Metriken.
9. **Lautersatz ʏ→y** vor dem Filter (Trainingskonvention des Modells).

## Golden-Testarchitektur (zweistufig, ADR-0013)

- **Stufe A** — Roh-espeak: gleicher espeak-Commit wie die Windows-Seite, beidseitig
  gepinnt (Ermittlung: Aufgabe 0.2). JVM-Tests laden den Host-Build; kleine
  Instrumented-Stichprobe belegt arm64 == Host.
- **Stufe B** — Kotlin-Frontend (Textregeln, Betonung, Wortlaute, Filter): reine
  JVM-Tests ohne Robolectric, zeichengenau gegen die Goldens.
- `app/src/test/resources/golden/frozen-windows/` wird NIE regeneriert. Neue Goldens
  nur über den Golden-Writer mit begründeter Herleitung im PR.

## Messkultur

- Energie in **mAh** (BatteryManager/dumpsys batterystats), **nie am Kabel**.
- Erstmessung zählt; dritte Position auf heißem Gerät ist wertlos (Thermik-Falle).
- PSS-Aufriss private dirty vs. file-backed (dumpsys meminfo), nicht nur die Summe.
- Jede Zahl in docs/erkenntnisse.md trägt Datum, Gerät, Methode. Unbelegtes wird als
  solches gekennzeichnet.
- Zielgerät: SM-F971B (Fold, SM8850, 16 GB, Android 17).

## Konventionen

- **Sprache:** Doku deutsch (Tiefendoku), README/CONTRIBUTING-Kern englisch (ADR-0007).
  Code-Bezeichner englisch, Kommentare deutsch.
- **Kommentare** erklären das Warum und die Messung dahinter, nie das Was.
- **Commits:** Conventional Commits, deutsch im Betreff erlaubt. Lizenz-Header:
  `SPDX-License-Identifier: GPL-3.0-or-later` in eigenen Quelldateien.
- **Diagramme:** nur in docs/architektur.md, Nachzug im selben Commit wie die
  Architekturänderung (Definition of Done, Punkt 4). Mermaid-flowchart, keine
  C4-Spezialsyntax, > 15 Knoten → aufteilen.
- **ADRs sind unveränderlich**; Ersetzung nur per neuem ADR.
- Gewichte (`*.onnx`, `*.ort`, `*.bin`, `*.npz`) kommen NIE ins Git — `models.json`
  ist das Manifest mit URLs + SHA-256.

## Gerätezugriff

`adb` gegen das per USB verbundene Zielgerät; Installation über
`gradlew :app:installDebug` genügt hier (keine Asset-Packs). WAV-Vergleiche per
`adb pull`. Vor destruktiven adb-Befehlen: nachsehen, was auf dem Gerät liegt.
