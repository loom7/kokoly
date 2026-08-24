# Recherchefeld 3: ONNX Runtime auf Android — Paketgröße, Execution Provider, Präzision, RAM, Energie, Modellbezug

> Rechercheablage vom 24.08.2026, erhoben für den PROJEKTPLAN. Inhalt vor Schönheit.

## Kernaussagen

- onnxruntime-android kommt von Maven Central; Version 1.23.2 ist der Sweet Spot: erste 16-KB-Page-kompatible Version (Pflicht ab Android 15/Play-Policy, relevant fürs Android-17-Zielgerät) bei 19,3 MiB arm64-Bibliothek — die aktuelle 1.29.0 ist auf 32,1 MiB gewachsen (WebGPU-EP einkompiliert). Nur arm64-v8a ausliefern; selbst nachgemessen am 24.08.2026.
- Execution Provider: XNNPACK ist für Kokoro Pflicht — im eigenen Geräte-Spike (sherpa, SM8650) RTF 0,70–0,74 gegen 3,6–4,0 mit CPU-EP; NNAPI ist seit Android 15 deprecated und für LSTM/STFT-Graphen ungeeignet; QNN/GPU bleibt verworfen. Aber: welcher EP gewinnt, ist je Graph unvorhersehbar (Supertonic: CPU-EP schlägt XNNPACK) — beide Arme messen, Threads 1/2/4 sweepen.
- fp16 halbiert Datei und Gewichts-RAM (156 MiB), ist auf CPU aber eher tempo-neutral bis langsamer (Cast-Knoten um Operatoren ohne fp16-Kernel, Kokoro enthält LSTM); int8 war am Gerät LANGSAMER als fp32 (RTF 0,84–0,93 gegen 0,70–0,74) und Kokoro ist quantisierungsempfindlich (Rausch-Artefakte). Empfehlung: fp32 als Referenz, fp16 als RAM-Kandidat mit Hörprüfung (ich-Laut-Gate!), int8 nur als Speicheroption.
- RAM-Anker (gemessen, fp32 über sherpa, SM8650): PSS ~300 MB nach Init, bis ~750 MB während Synthese. Drückbar über: .ort-Format + session.use_ort_model_bytes_directly/use_ort_model_bytes_for_initializers über MappedByteBuffer (Gewichte bleiben file-backed/verdrängbar statt kopiert), Arena-Shrinkage je Lauf (memory.enable_memory_arena_shrinkage=cpu) oder Arena aus, mem_pattern aus (dynamische Formen), nur aktivierte Stimmen laden (522 KB statt 27 MiB). Zielkorridor: fp32 heiß ~350–500 MB, fp16 ~200–300 MB, entladen ~40–80 MB — am Gerät zu verifizieren.
- Energie/Lebenszyklus: TextToSpeechService bleibt von Clients (TalkBack, Browser) dauerhaft gebunden — onDestroy ist kein Entladehaken. Muster: lazy laden beim ersten Satz, Sessions nach 2–3 min Leerlauf per eigenem Timer entladen. Neuladen kostet prognostisch 0,5–1,0 s auf dem SM8850 (gemessen 1,3–2,2 s auf SM8650 mit .onnx; .ort halbiert den Aufbau, ~4500 Knoten; +75 % CPU) — akzeptabel für den ersten Satz nach Minuten Stille. Zusätzlich Thread-Spinning abschalten (session.intra_op.allow_spinning=0), sonst brennen die Pool-Threads nach jedem Lauf Strom.
- Modellbezug: Gewichte nie ins Git (GitHub-Limit 100 MB/Datei, LFS-Freikontingent unbrauchbar). Eigener GitHub-Release (bis 2 GiB je Datei, kostenlos) + In-App-Download beim ersten Start mit SHA-256-Prüfung, Range-Resume und noBackupFilesDir — exakt das F-Droid-erprobte Muster von RHVoice und SherpaTTS. APK ohne Modell ~70–80 MB (ORT 19 MiB + espeak-ng-Daten 17 MiB + voices.bin 27 MiB). Play Asset Delivery (1,5 GiB je Pack) nur als optionaler Zusatz, falls später Play — funktioniert außerhalb des Play Store nicht, der Downloader ist ohnehin nötig.

## Bericht

# Recherchefeld 3: ONNX Runtime auf Android — Speicher und Energie

Stand 24.08.2026. Zahlen ohne Quellenangabe sind eigene Messungen dieser Sitzung (Maven-Central-Artefakte per HEAD/unzip vermessen) oder dokumentierte Gerätemessungen aus dem Nova-Projekt (`C:/Users/jtill/Documents/_Claude/CodeTest`), dort mit Dateipfad belegt.

---

## 1. onnxruntime-android: Paket, API, Execution Provider

### Paketgröße (selbst vermessen, Maven Central)

Artefakt: `com.microsoft.onnxruntime:onnxruntime-android` (AAR mit vier ABIs, Java-Klassen, C-Headern). Größen der `jni/arm64-v8a/libonnxruntime.so` (unkomprimiert, das ist was im APK zählt):

| Version | AAR gesamt | arm64-v8a .so | Anmerkung |
|---|---:|---:|---|
| 1.22.0 (Mai 2025) | 28,5 MB | 18,2 MiB | keine 16-KB-Pages |
| **1.23.2 (Okt 2025)** | **30,2 MB** | **19,3 MiB** | **erste 16-KB-kompatible Version** |
| 1.24.3 (März 2026) | — | 25,8 MiB | |
| 1.27.0 (Juni 2026) | — | 28,0 MiB | |
| 1.29.0 (Aug 2026, aktuell) | 51,9 MB | 32,1 MiB | WebGPU-EP einkompiliert |

Dazu je ABI `libonnxruntime4j_jni.so` (~100 KB) und `classes.jar` (~140 KB) — vernachlässigbar. armeabi-v7a wäre 13,2 MiB (1.22.0) extra; die x86-ABIs fliegen per `abiFilters` raus.

**Empfehlung: 1.23.2 pinnen.** Begründung: (a) Die 16-KB-Page-Kompatibilität ist seit November 2025 Play-Pflicht für targetSdk ≥ 35 und auf Android-15+-Geräten (das Zielgerät läuft Android 17) real nötig; das JNI-Problem ([Issue #24902](https://github.com/microsoft/onnxruntime/issues/24902)) ist laut [Changelog-Referenzen](https://pub.dev/packages/onnxruntime_v2/versions/1.23.2+1/changelog) in 1.23.2 behoben. (b) Sie ist mit 19,3 MiB die kleinste kompatible Version — jede spätere schleppt 6–13 MiB mehr, primär für EPs (WebGPU), die eine 82M-CPU-TTS nie nutzt. Erst upgraden, wenn ein konkreter Fix es verlangt. APK-seitig: AGP lässt .so-Dateien seit minSdk 23 unkomprimiert im APK (`useLegacyPackaging=false`) — Play komprimiert den Transport (~40 %, also ~8 MiB Download-Anteil); ein direkt verteiltes GitHub-APK trägt die vollen 19 MiB. Wer das APK kleiner will, kann später einen [Custom Build mit reduziertem Operator-Satz](https://onnxruntime.ai/docs/build/android.html) bauen (historisch: 3,3 MiB statt 12 MiB in 1.11; das fertige „onnxruntime-mobile"-Paket ist seit 1.13.1 eingestellt) — Wartungslast, nicht für den Start.

### Java/Kotlin-API

Paket `ai.onnxruntime`: `OrtEnvironment` / `OrtSession` / `OnnxTensor` — exakt die API, die `SupertonicSynthesizer.kt` in CodeTest schon produktiv nutzt (AssetFd → `FileChannel.map` → `createSession(ByteBuffer)`); die Muster portieren 1:1. Per `javap` aus dem 1.22-AAR verifiziert: `setIntraOpNumThreads`, `setCPUArenaAllocator(boolean)`, `setMemoryPatternOptimization(boolean)`, `addConfigEntry(String,String)`, `addXnnpack(Map)`, `addNnapi(...)`, `addQnn(Map)`.

### Execution Provider für Kokoro-82M

- **XNNPACK-EP: für Kokoro Pflicht.** Der Nova-Geräte-Spike (sherpa-onnx 1.13.4, Kokoro-82M v1.1, Xperia SM8650, dokumentiert in `C:/Users/jtill/Documents/_Claude/CodeTest/PROMPT-KOKORO20260803.md`) maß: CPU-EP RTF 3,6–4,0 (disqualifizierend), XNNPACK fp32 mit 4 Threads RTF 0,70–0,74. Kokoros Conv/GEMM-lastiger Graph liegt XNNPACK; siehe auch [XNNPACK-EP-Doku](https://onnxruntime.ai/docs/execution-providers/Xnnpack-ExecutionProvider.html). **Aber**: dieselbe Codebasis maß für Supertonic das Gegenteil (CPU-EP 0,406 gegen XNNPACK 0,511) — welcher EP gewinnt, ist je Graph unvorhersehbar, also beide Arme als Konstruktor-Parameter messbar halten (das Muster steht in `SupertonicSynthesizer.kt`, inkl. der Falle: bei XNNPACK `intra_op_num_threads` als EP-Option setzen und ORTs eigenen Pool auf 1, sonst doppelte Kern-Belegung).
- **NNAPI: nicht verwenden.** [Seit Android 15 deprecated](https://developer.android.com/ndk/guides/neuralnetworks/migration-guide); für einen Graphen mit LSTM/STFT partitioniert NNAPI schlecht (viele CPU-Fallback-Grenzen = Kopierkosten). Google verweist auf LiteRT bzw. ORT+QNN — beides für dieses Projekt verworfen (QNN/HTP will quantisierte, statische Formen; Kokoro ist fp32 mit dynamischen Längen).
- **Threads:** Die Hausdaten widersprechen sich produktiv: Kokoro über sherpa brauchte 4 (2 Threads → RTF 1,2–1,4), Supertonic lief mit 2 am besten (bandbreitengebunden, 42 % schneller als 4, einziger Wert unter der 38-°C-Drosselschwelle — Sweep in `PROMPT-SUPERTONIC20260814.md`). Auf dem SM8850 (+75 % CPU) ist 2 wieder im Rennen. **Sweep 1/2/4 mit Hauttemperatur ist Pflichtmessung**, Erwartung 2–4.

---

## 2. fp16 gegen fp32 gegen int8 auf ARM

Verfügbare Artefakte: die bekannten kokoro-onnx-Varianten (fp32 311 MiB, fp16 156 MiB, int8 109 MiB) plus die [onnx-community-Exporte](https://huggingface.co/onnx-community/Kokoro-82M-v1.0-ONNX/tree/main/onnx): q8f16 86 MB, quantized 92,4 MB, uint8f16 114 MB, uint8 177 MB, q4f16 155 MB.

### Was zu ERWARTEN ist

- **fp32**: Referenzqualität. Gemessen RTF 0,70–0,74 (XNNPACK, 4 Threads, SM8650); unter Dauerlast Drift auf 0,85–1,1. Prognose SM8850: ~0,4–0,5 kalt — komfortabel echtzeitfähig.
- **int8 (dynamisch)**: am Gerät **langsamer als fp32** gemessen (RTF 0,84–0,93 gegen 0,70–0,74, XNNPACK) — dynamische Quantisierung ist hier eine Größen-, keine Geschwindigkeitsoptimierung, deckungsgleich mit Desktop-Befunden ([int8 4 s gegen fp32 1,5 s auf M3-CPU](https://www.adrianlyjak.com/p/onnx/)). Dazu das Qualitätsrisiko: Kokoro ist quantisierungsempfindlich — naive Vollquantisierung erzeugt „weird static sounds"; brauchbare Exporte schließen empfindliche Schichten aus, und [Paritätsmessungen fehlen](https://huggingface.co/tonythethompson/Kokoro-82M-Q8F16-ONNX). int8 nur als Option für knappe Geräte, nach bestandener Hörprüfung.
- **fp16**: halbiert Datei und Gewichts-RAM — das ist der eigentliche Gewinn. Tempo auf CPU ist **nicht** automatisch besser: ORTs CPU-EP hat fp16-Kernel (MLAS, aarch64) nur für einen Teil des Operatorsatzes; um alles andere legt ORT Cast-Paare fp16↔fp32, was fp16-Modelle messbar langsamer machen kann ([Issue #22242](https://github.com/microsoft/onnxruntime/issues/22242), [#13838](https://github.com/microsoft/onnxruntime/issues/13838)). Kokoro enthält LSTM- und STFT-Pfade — genau die Kandidaten ohne fp16-Kernel. Hauspräzedenz: Supertonics fp16-Graph starb an einer Fusion mit fp32-only-Kernel und brauchte BASIC_OPT (`SupertonicSynthesizer.kt`, Kommentar zu `basicOpt`). Die fp16-Hardware der ARM-Kerne (ARMv8.2 FEAT_FP16, auf dem SM8850 sicher vorhanden) nutzt nur, wessen Kernel sie anspricht. **Erwartung: fp16 = halber RAM, Tempo neutral bis leicht langsamer, Qualität nahe fp32 — sofern der Export empfindliche Schichten in fp32 beließ (für die vorliegende Datei ungemessen).**

### Was GEMESSEN werden muss (Pflicht-Matrix am SM8850)

{fp32, fp16, ggf. int8} × {CPU-EP, XNNPACK} × Threads {1, 2, 4}: RTF kalt und eingeschwungen (20 Durchgänge), Hauttemperatur, PSS. Dazu das Qualitäts-Gate je Variante: deutsche Hörprobe mit **ich-Laut-Prüfsatz** (die historische sherpa-Falle!), WER-Werkzeug existiert (`CodeTest/tools/wer_check.py`). Messfallen aus dem Supertonic-Sweep übernehmen: ersten Lauf nach Installation verwerfen (~30 % langsamer), Abkühlpausen zwischen Sweep-Zeilen, `am force-stop` je Lauf.

---

## 3. RAM-Rechnung und PSS

### Der gemessene Anker

Sherpa+Kokoro fp32 auf dem SM8650: **PSS ~300 MB nach Init, bis ~750 MB während der Synthese**, nach `release()` wieder frei (`PROMPT-KOKORO20260803.md`). Zusammensetzung: Gewichtskopie ~330 MB (anonym/dirty, weil ORT die Initializer aus dem Protobuf in eigene Allokationen kopiert) + Arena-Hochwasser + Aktivierungen.

### Die Hebel (Schlüssel aus `onnxruntime_session_options_config_keys.h` des AAR verifiziert)

1. **Gewichte in-place statt kopiert**: Modell einmalig ins **.ort-Format** wandeln (on-device möglich: `addConfigEntry("session.save_model_format","ORT")` + `setOptimizedModelFilePath` — das Muster läuft in `CodeTest/.../QwenBenchActivity.kt`), dann per `FileChannel.map` als `MappedByteBuffer` laden mit `session.use_ort_model_bytes_directly=1` **und** `session.use_ort_model_bytes_for_initializers=1`. Die Gewichte bleiben dann file-backed/clean: verdrängbar unter Druck, PSS schrumpft statt dass der Prozess stirbt. Vorbehalt: das Pre-Packing kopiert GEMM-Gewichte trotzdem — `session.disable_prepacking=1` tauscht Tempo gegen RAM; beide Arme messen.
2. **Arena zähmen**: `mem_pattern` bringt nur bei statischen Formen etwas — Kokoros Formen sind je Äußerung dynamisch, also `setMemoryPatternOptimization(false)`. Entweder `setCPUArenaAllocator(false)` (Allokationen gehen sofort ans OS zurück, kleiner Tempoaufschlag) oder Arena behalten und nach jeder Äußerung per RunOption `memory.enable_memory_arena_shrinkage=cpu` das Hochwasser freigeben (Schlüssel in `onnxruntime_run_options_config_keys.h` verifiziert). Das adressiert genau die 300→750-MB-Spreizung.
3. **Stimmvektoren selektiv**: nur aktivierte Stimmen laden — 522 KB je Stimme statt 27 MiB Gesamtdatei.
4. **In Stücken synthetisieren und streamen** (kokoro-onnx teilt bei 510 Token ohnehin): hält Aktivierungs-Spitzen und Latenz klein.

### Realistischer Zielkorridor (zu verifizieren)

| Zustand | fp32 (311 MiB) | fp16 (156 MiB) |
|---|---:|---:|
| heiß, Synthese läuft | ~350–500 MB PSS (Gewichte großteils clean) | ~200–300 MB |
| geladen, leer | ~330–380 MB | ~180–220 MB |
| entladen (Sessions zu) | ~40–80 MB | ~40–80 MB |

Ohne die .ort-in-place-Route ist das gemessene 300/750-MB-Muster die ehrliche Erwartung. Die 16 GB des Zielgeräts tragen das locker — aber ein TTS-Dienst, der 300+ MB parkt, ist ein bevorzugtes LMK-Opfer: Entladen ist auch Überlebensstrategie, nicht nur Höflichkeit.

---

## 4. Energie und Service-Lebenszyklus

- **Bindungsrealität**: `TextToSpeechService` ist ein Bound Service; Clients wie TalkBack oder Browser bleiben stundenlang gebunden. `onDestroy` ist darum **kein** brauchbarer Entladehaken — die Engine muss selbst haushalten.
- **Muster**: lazy laden beim ersten `onSynthesizeText`/`onLoadLanguage`; eigener Leerlauf-Timer entlädt die ORT-Sessions (Textregeln/Phonemisierer bleiben resident, das sind Kilobytes). Ein Plattform-Standard für den Timer existiert nicht; **Empfehlung 2–3 Minuten** nach der letzten Äußerung, weil der Wiedereinstieg billig ist (nächster Punkt) und weil ein dauerhaft fetter Prozess ohnehin vom System gekillt würde — der Kaltstartpfad muss also sowieso robust sein.
- **Ladezeit-Prognose**: gemessen 1,3–2,2 s Init (.onnx über sherpa, SM8650); erste Audioausgabe 1,5–1,9 s. Der ORT-Session-Aufbau skaliert mit der Knotenzahl (Kokoro ~4500 Knoten), das .ort-Format halbiert ihn (beides Projektbefund aus CodeTest, Bench in `QwenBenchActivity.kt`). Mit .ort und +75 % CPU des SM8850: **Prognose ~0,5–1,0 s bis zur Session, ~1–1,5 s bis zum ersten Ton** nach Kaltstart. **Abwägung klar benannt**: 2–3 min Timer kostet im schlimmsten Fall eine gute Sekunde Latenz beim ersten Satz nach längerer Stille — dafür fallen ~300 MB Standby-RAM weg. Wer Vorlese-Sitzungen fährt, merkt nichts (innerhalb des Fensters spricht alles sofort).
- **Spinning abschalten**: ORTs Thread-Pools spinnen nach Läufen per Default weiter — bei einem burstartigen TTS reine Verlustleistung. `session.intra_op.allow_spinning=0` (+ inter_op) bzw. `session.force_spinning_stop` setzen (Schlüssel im AAR-Header verifiziert).
- **Threads = Wärme**: der Supertonic-Sweep zeigt, dass zu viele Threads bei bandbreitengebundenen Graphen langsamer UND heißer sind (Drosselschwelle 38 °C Haut). Für lange Vorlesestrecken (die 400-Phonem-continuous-Fälle) den eingeschwungenen RTF messen, nicht den kalten.
- Keine Wakelocks nötig: die Synthese läuft im Callback-Faden des gebundenen Dienstes; Audio geht über `SynthesisCallback` an das System, nicht über einen eigenen AudioTrack.

---

## 5. Modellbezug: 300 MiB weder ins Git noch ins APK

- **Git**: GitHub blockt Dateien > 100 MB hart; Git LFS ist mit dem Freikontingent (~1 GiB Bandbreite/Monat) für ein öffentliches 311-MiB-Modell unbrauchbar. Gewichte gehören nie ins Repo — nur Prüfsummen und Bezugs-URLs.
- **GitHub Releases**: bis 2 GiB je Datei, kostenlos, unlimitierter Traffic für öffentliche Repos — der kanonische FOSS-Weg; kokoro-onnx selbst verteilt seine Modelle so, sherpa-onnx ebenfalls.
- **Das erprobte Vorbild-Muster** (beide F-Droid-gelistet, d. h. Laufzeit-Download frei lizenzierter Modelle ist F-Droid-verträglich):
  - [RHVoice](https://f-droid.org/en/packages/com.github.olga_yakovleva.rhvoice.android/): App lädt ein JSON-Paketverzeichnis ([RHVoice/packages](https://github.com/RHVoice/packages)) und daraus versionierte Sprach-/Stimmpakete mit Prüfsummen — Pakete aktualisierbar ohne App-Update.
  - [SherpaTTS](https://f-droid.org/en/packages/org.woheller69.ttsengine/) ([woheller69/ttsengine](https://github.com/woheller69/ttsengine)): lädt beim ersten Start das gewählte Modell von HuggingFace; Internet-Berechtigung nur dafür, danach vollständig offline; Sideload als Fallback.
- **Konkrete Empfehlung für dieses Projekt**:
  - **APK enthält**: Code + ORT 1.23.2 (19,3 MiB) + espeak-ng (Bibliothek wenige MiB + 17 MiB Daten, Handling-Muster aus CodeTest) + `voices-v1.0.bin` 27 MiB → **APK ~70–80 MB, Modell nicht enthalten**.
  - **Erster Start**: Variantenwahl (fp32 „Qualität" / fp16 „ausgewogen"), Download vom **eigenen GitHub-Release** (nicht HF hotlinken: CDN-Redirects, Ratenlimits, keine Pinnung), **SHA-256-Prüfung**, HTTP-Range-Resume, atomares Umbenennen nach `noBackupFilesDir` (300 MB würden das Auto-Backup sprengen), Fortschritt in der Engine-Settings-Activity. Das JSON-Manifest-Muster von RHVoice übernehmen, dann sind Modell-Updates vom App-Zyklus entkoppelt.
  - **.ort-Erzeugung on-device** nach dem Download (einmalig, Muster aus `QwenBenchActivity.kt`): löst die Versionsbindung des .ort-Formats an die ORT-Ausgabe elegant — die App baut es sich bei Bedarf neu, verteilt wird das kanonische .onnx.
  - **Play Asset Delivery**: nur relevant, falls später Play-Vertrieb dazukommt; die Limits tragen es locker ([1,5 GiB je Asset Pack, 4 GiB install-time kumulativ](https://support.google.com/googleplay/android-developer/answer/9859372)). PAD funktioniert außerhalb des Play Store nicht (F-Droid, GitHub-APK) — der eigene Downloader ist also ohnehin Pflicht und kommt zuerst; PAD wäre reiner Zusatzkanal.

---

## Quellen

**Eigene Messungen (24.08.2026)**: AAR-Größen und .so-Inhalte direkt von [repo1.maven.org/…/onnxruntime-android](https://repo1.maven.org/maven2/com/microsoft/onnxruntime/onnxruntime-android/) (HEAD + unzip); Session-Options-/RunOptions-Schlüssel und Java-API aus den Headern und `classes.jar` des 1.22.0-AAR.

**Projektinterne Gerätemessungen**: `C:/Users/jtill/Documents/_Claude/CodeTest/PROMPT-KOKORO20260803.md` (Kokoro-Spike SM8650: RTF je EP/Präzision/Threads, Init, PSS), `C:/Users/jtill/Documents/_Claude/CodeTest/PROMPT-SUPERTONIC20260814.md` (Thread-/Wärme-Sweep, Messfallen, .ort/IoBinding-Befunde), `C:/Users/jtill/Documents/_Claude/CodeTest/app/src/main/java/de/tilly/nova/voice/SupertonicSynthesizer.kt` (ORT-Muster, XNNPACK-Thread-Falle, fp16/BASIC_OPT-Befund), `C:/Users/jtill/Documents/_Claude/CodeTest/app/src/debug/java/de/tilly/nova/bench/QwenBenchActivity.kt` (.ort-Erzeugung on-device).

**Extern**: [XNNPACK-EP-Doku](https://onnxruntime.ai/docs/execution-providers/Xnnpack-ExecutionProvider.html) · [NNAPI-Migrationsleitfaden (Deprecation, Android 15)](https://developer.android.com/ndk/guides/neuralnetworks/migration-guide) · [16-KB-Pages: ORT-Issue #24902](https://github.com/microsoft/onnxruntime/issues/24902), [Fix-Hinweis 1.23.2](https://pub.dev/packages/onnxruntime_v2/versions/1.23.2+1/changelog), [Android-Doku 16-KB](https://developer.android.com/guide/practices/page-sizes) · fp16-auf-CPU-Problematik: [ORT #22242](https://github.com/microsoft/onnxruntime/issues/22242), [ORT #13838](https://github.com/microsoft/onnxruntime/issues/13838) · Kokoro-Quantisierungsempfindlichkeit: [Adrian Lyjak, „Exporting and quantizing Kokoro to ONNX"](https://www.adrianlyjak.com/p/onnx/), [Kokoro-82M-Q8F16-Modellkarte](https://huggingface.co/tonythethompson/Kokoro-82M-Q8F16-ONNX) · Varianten: [onnx-community/Kokoro-82M-v1.0-ONNX](https://huggingface.co/onnx-community/Kokoro-82M-v1.0-ONNX/tree/main/onnx) · Vorbilder: [RHVoice auf F-Droid](https://f-droid.org/en/packages/com.github.olga_yakovleva.rhvoice.android/), [RHVoice/packages](https://github.com/RHVoice/packages), [SherpaTTS auf F-Droid](https://f-droid.org/en/packages/org.woheller69.ttsengine/), [woheller69/ttsengine](https://github.com/woheller69/ttsengine) · [Play-Größenlimits](https://support.google.com/googleplay/android-developer/answer/9859372) · [ORT Android-Build/Custom-Build](https://onnxruntime.ai/docs/build/android.html)