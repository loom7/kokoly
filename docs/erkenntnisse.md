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
