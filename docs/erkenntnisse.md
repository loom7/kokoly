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
| 25.08.2026 | **M3: Regelwerk-Port zeichengenau** — Textregeln (42 Muster), Betonung (11), Wortlaute (15) über 42 Regel-Golden-Sätze inkl. Gegenproben referenzgleich; Zählprobe und Kasusendung mitgeprüft | RegelnGoldenTest, JVM, erster Lauf grün |
| 25.08.2026 | **M2a-Matrix (SM-F971B, ORT 1.28.0, je 4 Sätze, Erstlauf je Konfiguration):** fp16+CPU-EP+4T ist der Sieger — RTF 0,393/0,408 bei 294/434 MB PSS. fp32 gleichauf im Tempo (0,40 martin-de), aber +110 MB. **XNNPACK durchweg schlechter** (RTF 1,1–2,4, +250 MB PSS — Gegenteil der sherpa-Erfahrung; ORT-CPU-EP/MLAS gewinnt auf diesem SoC). **int8 unbrauchbar** (RTF 1,9–7,1; bestätigt und verschärft den Nova-Befund). 2 Threads: RTF 0,58–0,60 — tragfähig, ~45 % langsamer als 4T; Energieentscheid in M5 | MessMatrixTest, 21 Konfigurationen, messungen/ |
| 25.08.2026 | **Martin-fp16 (eigene Wandlung, ORT-Rezept): RTF 0,359/0,350, PSS 292 MB geladen** — schneller UND kleiner als fp32 (0,401 / 403 MB). onnxconverter-common scheitert an dieser Graphfamilie („Sub bound to different types"); ORTs OnnxModel.convert_float_to_float16 ist das Upstream-Rezept | MessMatrixTest + scripts/fp16-wandlung.py |
| 25.08.2026 | **HÖR-GATE fp16-DEUTSCH DURCHGEFALLEN:** Martin-fp16 (Vollwandlung) krächzt; Martin-fp32 sauber. Englisch v1.0 klingt in fp32/fp16/int8 durchweg gut (Nutzerurteil) | Hörprobe Nutzer |
| 25.08.2026 | Ursachensuche: Graphen strukturell gleich (Upstream-fp16 gegen Eigenwandlung, beide voll fp16, 443 Initialisierer), Gewichte beider fp32-Modelle bleiben unter der fp16-Grenze → der Schaden entsteht in den AKTIVIERUNGEN des Fine-Tunes zur Laufzeit; das Original verträgt fp16, der Fine-Tune nicht | Graph-/Gewichtsanalyse scripts/.venv |
| 25.08.2026 | Mischpräzision gebaut (node_block_list; zwei Wandlerfehler behoben: doppelte Cast-Knotennamen, identische Cast-Zwillinge mit gleichem Ausgangsnamen): **fp16dec** (Decoder fp32) RTF 0,356/0,362 stabil, PSS 383/506 MB, 258 MiB; **fp16gen** (nur Vocoder fp32) driftet 0,44→0,87 — Präzisionsnaht mitten im Rechenweg | MessMatrixTest |
| 25.08.2026 | **HÖR-GATE Mischfassungen BESTANDEN (beide):** fp16dec UND fp16gen klingen gut (Nutzerurteil). Entscheidung fp32/fp16dec/fp16gen bewusst OFFEN bis zur M5-Energiemessung; Favorit des Nutzers nach RTF: fp16dec | Hörprobe Nutzer |
| 25.08.2026 | **Threads über 4 bringen nichts:** fp16dec mit 6T (0,44–0,64) und 8T (0,41–0,94) schlechter/streuender als 4T (0,356) — passt zur Big-Core-Zahl des SoC. Messungen thermisch belastet (spät in langer Serie, Erstmessungsregel!), Richtung aber eindeutig und deckungsgleich mit den Vorprojekten | MessMatrixTest |
| 25.08.2026 | fp16gen frisch nachgemessen: 0,65–0,83 auch in Einzellage (thermisch belastet) — die Nahtkosten der Präzisionsgrenze mitten im Decoder sind real, nicht nur Thermik | MessMatrixTest |
| 25.08.2026 | **M2b + M4 am Gerät:** Framework-Klient sieht 42 Stimmen, wählt en-US-af_heart per setVoice, synthetisiert Deutsch UND Englisch (Gruppenwechsel martin→v1.0 inklusive) in einem Lauf — 24 s gesamt | TtsClientTest |
| 25.08.2026 | Session-Politik entschieden (ADR-0012): EINE residente Session, Tausch beim Gruppenwechsel (0,8–1,0 s) statt ~700 MB Dauer-PSS für zwei | M2a-Zahlen + TtsClientTest |
| 25.08.2026 | Ladezeiten: CPU-EP 0,80–1,17 s (Pfad-Route, ohne .ort); XNNPACK +0,2–1,0 s (Prepacking) | MessMatrixTest |
| 25.08.2026 | Messfalle dokumentiert: String.format ohne Locale.ROOT schreibt auf deutschem Gerät Dezimal-KOMMAS ins JSON | erste Matrixserie |
| 25.08.2026 | Regelstufe lebt hinter der System-Schnittstelle: „Martin 1× berichtigt", „Abkürzung → zum Beispiel", „Uhrzeit ausgeschrieben" im Dienstprozess-Log | TtsClientTest + logcat |
| 25.08.2026 | **Synthese-Regel gemessen:** espeak spricht „Synthese" als `zˈyntəsə` („ZÜN-te-se"); Berichtigung `zyntˈeːzə` hebt K von 0,895 auf 1,094 (ΔK +0,20, Klasse widersprechen). Photosynthese analog; Reihenfolge Photosynthese VOR Synthese (Teilstück-Falle der Zählprobe) | scripts/messung/k_pruefung.py (Satzrahmen, create_timed + parselmouth, Windows-Referenz) |
| 25.08.2026 | **Golden-Writer-Weg etabliert:** neue Regeln nach dem Einfrieren der Referenz (ADR-0013) bekommen ihre Endfassung aus `NEUE_REGEL_ERWARTUNGEN` (korpus.py) mit Herleitung; `endfassung_quelle` im Golden unterscheidet windows-referenz von golden-writer. 45 Regelsätze, davon 3 Golden-Writer, alle grün | RegelnGoldenTest, JVM |
| 25.08.2026 | **Tonhöhe am Gerät belegt:** setPitch 1,4 → F0-Faktor 1,448, setPitch 0,75 → 0,769 (F0-Median der gezogenen WAVs), Dauerabweichung ≤ 4 % — die Tempokompensation hält die Länge. Verfahren: Modell um p langsamer, Umtasten um p schneller (Tonhoehe.kt; Formantverschiebung als bekannte Grenze dokumentiert) | PitchTest (SM-F971B) + parselmouth host-seitig |
| 25.08.2026 | Einstellungen folgen Tag/Nacht: Theme.Kokoly löst isLightTheme in beiden uiModes richtig auf (Gerät war gesperrt — Sichtprüfung des Nutzers steht aus) | ThemaTest (SM-F971B) |
| 25.08.2026 | **Engine stumm nach Engine-Wechsel (Nutzerfund Wiedergabe-Knopf):** onDestroy rief espeak_Terminate, der Dienstprozess überlebte aber; beim Rebind kehrte starte() wegen vokabular!=null sofort um → espeak tot → IllegalStateException → callback.error(). Beweis: Player-Historie (AudioTrack erzeugt, nie gestartet, 2× stopped) + Nachsteller rot am alten, grün am neuen Stand. Festlegung berichtigt: espeak resident bis Prozessende, init idempotent vor jeder Abkürzung | dumpsys audio Player-Log + DienstNeustartTest (SM-F971B) |
| 25.08.2026 | kokoly_jni.c enthielt in drei Zeichenliteralen ROHE NUL-Bytes statt  -Escape — kompiliert identisch, aber jedes Werkzeug (git, grep, file) hielt die Quelle für binär; eine Leseanalyse hielt den Puffer deshalb fälschlich für unterminiert. Zu Escapes normalisiert, Geräteparität unverändert zeichengenau | cat -A/od + EspeakParityTest |
| 25.08.2026 | Doppel-Klemmung Tonhöhe×Tempo aufgelöst: bei Tempo 1,64 und Tonhöhe 0,75 wollte das Modell 2,19 (geklemmt 2,0) — der Fehler ging voll in die DAUER. Jetzt Vorrang Tempotreue: der Lesefaktor trägt den Klemm-Rest, die Tonhöhe weicht am Fensterrand zurück | PitchTest-Rotlauf mit Systemtempo 164 % |
| 25.08.2026 | Haken der Sprach-Checkboxen im Dunkelmodus unsichtbar (Nutzerfund): DeviceDefault-Vorgabetönung trägt auf dunklem Grund nicht — Haken-Farben hängen jetzt explizit an den Textfarben des Themas; Edge-to-Edge-Insets (targetSdk 37) nachgerüstet, sonst läge die Überschrift unter der Statusleiste | Gegenprüfung + Sichtprüfung Nutzer offen |
| 25.08.2026 | **Gegenprüfung fand Absturzfenster:** OrtSession.close() hat in ORT 1.28.0 KEINE Absicherung gegen einen laufenden run() (Bytecode geprüft: unsynchronisiertes closed-Flag) — onDestroy mitten in langer Synthese wäre Use-after-free. Fix: faires RW-Lock um Lauf/Schließen in KokoroSynthesizer plus RunOptions.setTerminate — damit ist auch die seit Aufgabe 1.2 GEPLANTE Stop-Beschleunigung („Stop mitten im langen Run") endlich implementiert statt nur behauptet | Werkzeug-Review (12 Agenten) + DienstNeustartTest-Stressmethoden |
| 25.08.2026 | **Messfalle Hörprobe:** HoerprobeTest übernahm ungepinnt die Systemregler — die Synthese-Hörprobe entstand mit Tempo 164 %/Tonhöhe 83 % (Modelltempo 1,98) und klang nach verschluckten Silben; Pipeline und Phonemkette waren korrekt (Phonemfeld-Log = Golden-Endfassung, Segmentdauern der Referenz gesund, Gerätedauer 2,67 s zu Referenz 2,59 s bei 1,0). Hörproben pinnen jetzt Tempo/Tonhöhe 1,0; neues Werkzeug dauer_pruefung.py prüft Segmentdauern je Regelkandidat | HoerprobeTest + scripts/messung/dauer_pruefung.py |
| 25.08.2026 | **Nutzerfund 2. Runde: „-synthese" auch in KOMPOSITA falsch** (Sprachsynthese). espeak realisiert den Wortteil je Betonungslage verschieden — frei/Photo- `zˈyntəsə`, Bio-/Foto- `zˌyntəsə`, Sprach-/Klang-/Protein- `zyntəsə` (unbetont), Mehrzahl frei `zˈynteːzən` (nur Betonung falsch), Mehrzahl im Kompositum `zyntˌeːzən` (schon richtig). Neue Regelart WORTTEILE: Textmuster ohne Wortgrenzen + Variantentafel, Zählprobe über die Variantensumme; ersetzt die zwei Einzelregeln, alle Alt-Goldens zeichengleich. Gerätebeweis: „synthese 3× berichtigt" im gemischten Satz (Nebenton+Hauptton+Mehrzahl), 51 Regelsätze grün, Segmentdauern aller Zielfassungen gesund | Wortbatterie gegen Referenz-DLL + dauer_pruefung.py + RegelnGoldenTest + Phonemfeld-Log SM-F971B |
| 25.08.2026 | **M5-Energie (SM-F971B, abgesteckt, Bildschirm aus, drahtloses adb, Erstmessung je Konfiguration nach Abkühlung, 8037 Zeichen fester Lesetext):** Wach-Leerlauf-Basislinie 224 mA (9 min, enthält den Messstand: wache CPU + adb-Sitzung — echter Standby ist mit Verbindung prinzipiell nicht messbar). Netto je 1000 Zeichen: **fp32-2T 6,35 · fp32-4T 7,28 · fp16dec-4T 9,55 · fp16dec-2T 9,59 mAh**; RTF 0,674 / 0,469 / 0,544 / 0,766. **fp32 schlägt fp16dec auf beiden Threadzahlen in Tempo UND Energie** — Umkehrung der M2a-RTF-Rangfolge (dort Bildschirm an; cpuset war /foreground, Ursache vermutlich Governor bei Bildschirm aus; offen, für den Variantenvergleich unerheblich, da Bedingungen je Paar gleich). Vorbehalt: fp16dec-2T startete 2,6 °C wärmer (Abkühlgrenze Umgebung), erklärt den Rückstand von +39 % nicht. Ladungszähler quantisiert in ~4,2-mAh-Schritten (±4 mAh je Messung) | EnergieMessTest + energie_auswertung.py, messungen/m5/ |
| 25.08.2026 | **M5-Dauerlauf:** 30 min Vorlesen in Echtzeit-Taktung (fp32, 4T, 24 600 Zeichen): 323,4 mAh roh, netto ≈ 211 mAh; Temperatur 31,2→33,9 °C, **stabil, kein thermisches Weglaufen**. Achtung Feldbedeutung: im echtzeit-Modus ist rechenSekunden die Wanduhr (Taktschlafen inbegriffen), das rtf-Feld dort bedeutungslos. Engine hält im Leerlauf keine Wakelocks (dumpsys power geprüft) — Standby-Beitrag der Engine 0 | EnergieMessTest echtzeit + dumpsys battery cc |
| 25.08.2026 | Messfalle: WAKE_LOCK-Berechtigung nur im DEBUG-Manifest (Prüfstand) — das Release bleibt wakelock-frei (Z6); Samsung-WLAN schläft trotz wifi_sleep_policy=2 unter Ruhe ein, Abhilfe im Messfenster: host-seitiges Polling alle ~12 s | app/src/debug/AndroidManifest.xml |
| 25.08.2026 | **ENTSCHIEDEN (F9, ADR-0017): Deutsch läuft fp32 mit 4 Threads.** fp16dec nach M5-Daten verworfen; die gescheiterte fp16-Vollwandlung aus dem Manifest entfernt. v1.0 bleibt fp16 (ADR-0015; Energie dort ungemessen — Wiedervorlage bei Auffälligkeiten) | Nutzer-Entscheid auf M5-Datenlage |
| 31.08.2026 | **Leerlauf-Timer umgesetzt** (letzter M5-Punkt): 5 min nach der letzten Synthese fällt die ORT-Session (~300–400 MB PSS frei), espeak bleibt resident; Daemon-Uhr je Fenster neu gestellt (läuft bei langen Texten nie ab), kein Wakelock/Alarm — im Gerätetief feuert die Prüfung einfach später. Gerätebeweis: Session da → nach Leerlauf weg → nächster Auftrag lädt neu | LeerlaufTest (SM-F971B) |
