# Erkenntnisse — das Messjournal

Nachfolger von VERBESSERUNGEN.md aus der Windows-Referenz: hier steht nur
Belegtes. Jede Zahl trägt Datum, Gerät und Methode; Energie in mAh, nie am
Kabel erhoben. Unbelegte Übernahmen sind gekennzeichnet.

## Aus der Windows-Referenz übernommen (Belege: TTS Test/VERBESSERUNGEN.md)

| # | Erkenntnis | Beleg (Windows, 08/2026) |
|---|---|---|
| 1 | sherpa-onnx-Kokoro-Frontend erzeugte den ich-Laut-Fehler („itsch"); das Modell spricht mit espeak-Phonemisierung sauber | A/B-Messung 23.08.2026; Ursache Integrationsweg |
| 2 | kokoro-onnx filtert still aufs 114er-Vokabular; kurzes ü (ʏ) fehlte dadurch komplett („fünf"→`fˈnf`) | breite Lautprobe: ʏ einziger regulärer Verlust |
| 3 | espeak-Sprachwechselmarken `(en)…(de)` werden mitgesprochen (bei „Team" 725 von 1325 ms) | duration-Ausgang, 24.08.2026 |
| 4 | Betonungszeichen wirkt nur unmittelbar vor dem Vokal, nicht satzfinal | F0-Eichung an Trochäen/Jamben, K-Kennzahl |
| 5 | ja über espeak unbrauchbar (Kanji→„Chinese letter"), zh verliert 19 % (Tonziffern) | Vokabularabgleich 24.08.2026 |
| 6 | clause_pause ≤0,10 wirkungslos (Modelllücke 120 ms); wirksam ab ~0,13; gesetzt 0,22/0,35 | Pausenleiter |
| 7 | Stapelnaht ab 510 Token reißt hörbar (F0 87→173 Hz, 210 ms); continuous ab 400 Phonemen, Mehrkosten 0,96× | Nahtmessung |
| 8 | Vokoder mit seedlosen Zufallsknoten: nicht bitstabil (F0-Streuung 0,37 Hz, folgenlos fürs Ohr) | 10 Läufe |
| 9 | Lautersatz ʏ→y ist die Trainingskonvention des Modells | kikiri-Trainingsrezept |

## Vormessungen aus dem Nova-Archiv (ANDERER Weg — sherpa 1.13.4, Xperia SM8650)

Übertragungsvorbehalt: fremde Laufzeit, schwächerer SoC als das Zielgerät (SM8850).

| Konfiguration | RTF |
|---|---|
| CPU-Provider | 3,6–4,0 (disqualifizierend) |
| fp32, XNNPACK, 4 Threads | 0,70–0,74 |
| int8, XNNPACK, 4 Threads | 0,84–0,93 — **int8 langsamer als fp32** |
| 2 Threads | 1,2–1,4 |

Init 1,3–2,2 s · erstes Audio 1,5–1,9 s · PSS ~300 MB nach Init, bis ~750 MB in
der Synthese. Quelle: CodeTest/PROMPT-KOKORO20260803.md (03.08.2026).

## Android-Messungen (dieses Projekt)

*(ab M0 — Format: Datum · Gerät · Methode · Zahl.)*

| Datum | Befund | Methode |
|---|---|---|
| 24.08.2026 | Windows-Referenz nutzt **espeak-ng 1.52.0** (Pin für NDK- und Host-Build, s. native/espeak-ng/PIN.md) | espeak_Info() der espeakng-loader-0.2.4-DLL |
| 24.08.2026 | **arm64-espeak == Referenz-DLL, zeichengenau** (alle de-Stufe-A-Segmente inkl. Sprachwechsel- und Klammernfälle; Stimmwechsel stabil) | EspeakParityTest, SM-F971B, eigener 1.52.0-NDK-Build |
| 24.08.2026 | **Kotlin-Frontend == Referenz-Endfassung, zeichengenau, 8 Sprachen** (46 Korpussätze; Stufe-A-Fixtures spielen espeak) | FrontendGoldenTest, JVM |
| 24.08.2026 | **ORT 1.23.2 stürzt am SM8850 mit SIGILL in libonnxruntime** beim ersten Run (2× reproduziert) — 1.28.0 läuft fehlerfrei (ADR-0014) | GateWavTest, logcat crash-Puffer |
| 24.08.2026 | Kokoro-fp32 über ORT 1.28.0 direkt, 4 Threads, SM-F971B: **RTF 0,711 / 0,812 / 0,540** (drei Gate-Sätze, Erstlauf), **Session-Laden 0,80–1,04 s** (Pfad-Route, ohne .ort) | GateWavTest, System.nanoTime |
| 24.08.2026 | Ganze Kette Text→WAV am Gerät hörbar; A/B-Paare gegen Windows unter pruefstein/ | GateWavTest + Referenzlauf |
| 25.08.2026 | **M0-GATE BESTANDEN:** die drei A/B-Paare (ich-Laut-Satz, Frauchen/rauchen, Martin-Begrüßung) klingen für den Nutzer identisch zur Windows-Referenz | Hörprobe Nutzer |
| 25.08.2026 | **M1: Framework-Klient spricht** — TextToSpeech-Bindung an die Engine, setLanguage(GERMANY)→LANG_COUNTRY_AVAILABLE, Voice de-DE-martin vergeben, synthesizeToFile über den Streaming-Callback in 3,0 s (2 Sätze) | TtsClientTest, SM-F971B |
| 25.08.2026 | Engine erscheint in der System-Dienstauflösung (TTS_SERVICE-Intent) | cmd package query-services |
