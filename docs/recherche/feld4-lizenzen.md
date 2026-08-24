# Recherchefeld 4: Lizenzen für die OpenSource-Veröffentlichung

> Rechercheablage vom 24.08.2026, erhoben für den PROJEKTPLAN. Inhalt vor Schönheit.

## Kernaussagen

- Kokoro-82M-Gewichte, kokoro-onnx-Modelldateien (kokoro-v1.0.onnx, voices-v1.0.bin) und misaki sind Apache-2.0, kokoro-onnx-Code MIT, ONNX Runtime MIT — alle mitverteilbar.
- espeak-ng ist GPL-3.0-or-later OHNE Linking-Ausnahme: Wer libespeak-ng + espeak-ng-data in die App bündelt, muss die ganze App als kombiniertes Werk GPL-3.0-kompatibel lizenzieren. Präzedenz: eSpeak-NG-App und RHVoice liegen als GPL-Apps auf Google Play UND F-Droid — Play verträgt GPL-Apps in der Praxis.
- Konkrete GPL-Falle: die proprietäre Play-Core-/Play-Asset-Delivery-Bibliothek ('You will not modify … or create derivative works') darf nicht in eine GPL-App gelinkt werden — Install-Time-Asset-Packs (reiner AssetManager-Zugriff, keine Play-Core-Lib) oder Eigen-Download der Modelle zur Laufzeit (RHVoice-Muster) verwenden.
- Ein gleichwertiger nicht-GPL-Ersatz für espeak existiert praktisch nicht: gruut (MIT) ist archiviert und kann weder Hindi noch Portugiesisch; goruut (MIT, 140 Sprachen, Betonung) existiert, nutzt aber andere Phonemkonventionen als das espeak-trainierte Kokoro — genau der Integrationsfehler-Typ, der schon den sherpa-Weg ruiniert hat.
- Überraschung Martin-Fine-Tune: die GESAMTE Kette ist Apache-2.0 getaggt (Godelaune-ONNX ← kikiri-tts/kikiri-german-martin ← kikiri-Basis ← hexgrad/Kokoro-82M), und die Stimme ist der LibriVox-Vorleser Martin Harbecke (Public Domain, HUI-Korpus CC0). Restfragen: Erklärungsbefugnis des Uploaders und unbenannter TTS-Generator der synthetischen Basisdaten — dokumentieren, aber Verteilung ist vertretbar.
- JA/ZH-Stack komplett permissiv: fugashi MIT, MeCab GPL/LGPL/BSD-Dreifachlizenz (BSD wählbar), unidic-lite MIT/WTFPL mit UniDic-Daten BSD, jieba/cppjieba MIT, pypinyin MIT, pinyin-to-ipa MIT.
- phonemizer (GPL-3.0) und praat-parselmouth/Praat (GPL-3.0+) betreffen nur die Windows-Pipeline; auf Android ersetzt durch direkten espeak-Aufruf bzw. eigene PSOLA-Implementierung (TD-PSOLA-Patente aus den 1990ern sind abgelaufen).
- Empfehlung: Projektlizenz GPL-3.0-or-later (von espeak-ng erzwungen, kompatibel mit allen übrigen Komponenten); Repo mit LICENSE, THIRD-PARTY-NOTICES.md und NOTICE; Kokoro-Modell + voices + Martin-ONNX dürfen mitverteilt werden (Apache-Hinweise beilegen), zusätzlich F-Droid als Kanal wegen der Play-App-Signing-Debatte.

## Bericht

# Recherchefeld 4: Lizenzen für die OpenSource-Veröffentlichung

Stand: 2026-08-24. Alle Angaben von den Primärquellen (HuggingFace-Modellkarten, GitHub-README/LICENSE, offizielle Projektseiten). Kein Rechtsrat — gesammelte Klauseln und benannte offene Fragen.

---

## 1. Kokoro-82M und die ONNX-Konvertierung

**hexgrad/Kokoro-82M** — Lizenz-Tag der Modellkarte: **apache-2.0**. Wörtlich aus der Karte: *„With Apache-licensed weights, Kokoro can be deployed anywhere from production environments to personal projects."* Die Karte betont Training auf *„permissive/non-copyrighted audio data"* (Public Domain, Apache/MIT-lizenziert, synthetische Ausgaben geschlossener TTS) und dokumentiert CC-BY-Quellen (Koniwa CC BY 3.0, SIWIS CC BY 4.0) — deren Attribution steht bereits in der Modellkarte.
→ https://huggingface.co/hexgrad/Kokoro-82M

**thewh1teagle/kokoro-onnx** — README, Lizenzabschnitt wörtlich: *„kokoro-onnx: MIT"* und *„kokoro model: Apache 2.0"*. Die Modelldateien (kokoro-v1.0.onnx, fp16, int8, voices-v1.0.bin) liegen im GitHub-Release des Projekts.
→ https://github.com/thewh1teagle/kokoro-onnx und https://github.com/thewh1teagle/kokoro-onnx/releases

**Folge:** Modell fp32/fp16/int8 und voices-v1.0.bin dürfen unter Apache-2.0 **mitverteilt** werden (Lizenztext + Attribution beilegen). Der kokoro-onnx-Python-Code ist MIT; wird beim Kotlin-Port Logik daraus übernommen/übersetzt, genügt der MIT-Hinweis in den Third-Party-Notices.

## 2. espeak-ng: GPL-3.0 und die konkreten Folgen

**Lizenz:** README wörtlich: *„eSpeak NG Text-to-Speech is released under the GPL version 3 or later license."* Die COPYING-Datei ist der unveränderte GPL-3.0-Text (*„GNU GENERAL PUBLIC LICENSE Version 3, 29 June 2007"*) — **keine Linking-Exception**, keine Zusatzerlaubnis (die AGPL-Passage in §13 ist Standard-GPL-Text, keine Ausnahme). Die Sprachdaten (espeak-ng-data, die 17 MB) stammen aus demselben Repo und stehen unter derselben Lizenz.
→ https://github.com/espeak-ng/espeak-ng , https://github.com/espeak-ng/espeak-ng/blob/master/COPYING

**Was das für die App heißt:**
- libespeak-ng als .so gebündelt + espeak-ng-data in den Assets = **kombiniertes Werk**. Nach GPL-3.0 §5 muss das Ganze unter GPL-3.0-kompatiblen Bedingungen weitergegeben werden. Praktisch: **die App wird GPL-3.0** (alle übrigen Komponenten — Apache-2.0, MIT, BSD — sind GPL-3.0-kompatibel einbindbar; die Apache/MIT-Hinweise bleiben in den Notices erhalten).
- **App-Store-Verträglichkeit:** GPL-Apps auf Google Play sind gelebte Praxis mit direkter Präzedenz im TTS-Feld: die eSpeak-NG-App selbst (com.reecedunn.espeak, F-Droid weist **GPL-3.0** aus) liegt auf F-Droid und in Play-Ports vor; RHVoice liegt auf Google Play und F-Droid. Der historisch problematische Store ist Apples App Store (VLC-Fall 2011), nicht Play. Diskutiert wird bei Play v.a. **Play App Signing** (Google hält den Signaturschlüssel; GPLv3-„Installation Information"/Anti-Tivoisierung gilt aber nur, soweit der Nutzer keine modifizierte Version installieren kann — Android erlaubt Sideloading eigener Builds). Empfehlung: **zusätzlich F-Droid** bedienen, das entschärft die Debatte vollständig.
  → https://f-droid.org/packages/com.reecedunn.espeak/ , https://f-droid.org/packages/com.github.olga_yakovleva.rhvoice.android/
- **Die eigentliche Falle ist Play Asset Delivery:** Die Play-Core-/Asset-Delivery-Bibliothek steht unter den proprietären „Play Core Software Development Kit Terms of Service", wörtlich: *„Google and its licensors own all right, title and interest … You will not modify, translate, or create derivative works of Redistributable Code."* Diese Bibliothek in eine GPL-App zu linken ist unverträglich. Auswege: (a) **Install-Time-Asset-Packs** — die werden ins APK gemerged und über den normalen AssetManager gelesen, **ohne** Play-Core-Bibliothek; (b) Modelle zur Laufzeit **selbst nachladen** (eigener Server/GitHub-Release — das RHVoice-Muster, das auch die 311-MiB-Frage elegant löst). Die Play-Asset-Delivery-Fallen aus dem CodeTest-Projekt sind hier also auch lizenzrechtlich relevant.
  → https://developer.android.com/guide/playcore
- **Wie lösen es andere?** espeak-ng-Android: schlicht ganz GPL-3.0. RHVoice (Wiki „License"): Kernbibliothek *LGPL v2.1 or later*, aber *„it relies on MAGE which is distributed under GPL v3 or later, so the combination is under GPL v3 or later"*; einzelne Stimmen tragen eigene Nicht-kommerziell-Klauseln. Beide zeigen: GPL-TTS-Engines als System-TTS auf Play/F-Droid funktionieren.
  → https://github.com/RHVoice/RHVoice/wiki/License

**Nicht-GPL-Alternative für IPA-G2P mit Betonung in 7 Sprachen?** Ehrliche Lage:
- **gruut** (rhasspy): MIT, IPA mit Betonungszeichen — aber **archiviert (Okt 2025, read-only)** und ohne Hindi und Portugiesisch. Scheidet aus. → https://github.com/rhasspy/gruut
- **goruut** (neurlang): MIT, 140 Sprachen inkl. de/en/es/fr/it/pt/hi, „Stressed phones: yes", in Go (via gomobile einbindbar). Existiert also — **aber** es erzeugt andere Phonemkonventionen als espeak, auf dessen Ausgabe Kokoro trainiert wurde. Ein G2P-Wechsel wäre exakt die Klasse Integrationsabweichung, die beim sherpa-Weg den ich-Laut zerstört hat (Erkenntnis 9). Qualität wäre komplett neu zu vermessen. → https://github.com/neurlang/goruut
- epitran (MIT) liefert keine Betonung.
**Fazit:** Für die vermessene Pipeline gibt es keinen risikofreien espeak-Ersatz; die GPL-3.0-Konsequenz ist der sachlich richtige Preis.

## 3. misaki und der JA/ZH-Stack

| Komponente | Lizenz (Quelle wörtlich) | URL |
|---|---|---|
| misaki (hexgrad) | **Apache-2.0** (Repo-Lizenzangabe) | https://github.com/hexgrad/misaki |
| fugashi | **MIT** — *„fugashi is released under the terms of the MIT license."* | https://github.com/polm/fugashi |
| MeCab | **Dreifachlizenz GPL / LGPL / BSD** (offizielle Seite: Nutzung/Weitergabe „unter GPL, LGPL oder BSD-Lizenz"); fugashi-README: *„MeCab is … redistributed under the BSD License."* → BSD wählen | https://taku910.github.io/mecab/ |
| unidic-lite (Code) | *„This code is licensed under the MIT or WTFPL license, as you prefer."* | https://github.com/polm/unidic-lite |
| unidic-lite (Wörterbuchdaten) | *„Unidic 2.1.2 is copyright the UniDic Consortium and distributed under the terms of the BSD license."* | ebd. |
| jieba | **MIT** (*„MIT 授权协议"*); Wörterbuch dict.txt ohne separate Lizenzangabe im README (offener Punkt, praktisch als Teil des MIT-Repos verteilt) | https://github.com/fxsjy/jieba |
| cppjieba (C++-Port für Android) | **MIT**; Wörterbuchdaten im Repo ohne eigene Deklaration | https://github.com/yanyiwu/cppjieba |
| pypinyin | **MIT**; nutzt pinyin-data/phrase-pinyin-data (Lizenz dort separat prüfen, wenn Daten extrahiert werden) | https://github.com/mozillazg/python-pinyin |
| pinyin-to-ipa | **MIT** (Badge „License: MIT") | https://github.com/stefantaubert/pinyin-to-ipa |

**Folge:** Der komplette JA/ZH-Pfad ist permissiv abbildbar — lizenzrechtlich steht einem Kotlin/C++-Port von misaki-Logik (Apache-2.0) mit MeCab-BSD + UniDic-BSD-Daten + cppjieba-MIT + pinyin-Daten nichts im Weg. Einzige Kleinigkeiten: die Wörterbuch-Rohdaten von jieba/cppjieba tragen keine eigene Deklaration (im Zweifel als Repo-Bestandteil unter MIT behandeln und in den Notices ausweisen) und pypinyins Datenquellen bei tatsächlicher Übernahme einzeln prüfen.

## 4. Der deutsche Martin-Fine-Tune — Kette vollständig aufgeklärt

Wichtigstes Ergebnis dieser Recherche: die Lage ist **deutlich besser als „ungeprüft"** vermuten ließ.

- **Godelaune/Kokoro-82M-ONNX-German-Martin**: Metadaten-Tag **„License: apache-2.0"**; die Karte nennt sich *„ONNX exported version"* von kikiri-tts/kikiri-german-martin. → https://huggingface.co/Godelaune/Kokoro-82M-ONNX-German-Martin
- **kikiri-tts/kikiri-german-martin**: **Apache 2.0**; *„German single-speaker TTS model fine-tuned on the Martin Harbecke voice"*, „Training samples: 627", Basis kikiri-german-base-51speakers-synthetic, „StyleTTS2 Stage 2 (Kokoro-compatible)". → https://huggingface.co/kikiri-tts/kikiri-german-martin
- **kikiri-tts/kikiri-german-base-51speakers-synthetic**: **Apache 2.0**; Trainingsdaten *„~30,800 samples (synthetic, TTS-generated)"* aus 51 deutschen Sprechern. → https://huggingface.co/kikiri-tts/kikiri-german-base-51speakers-synthetic
- **Trainingsrezept semidark/kikiri-tts** (GitHub): *„Apache License 2.0 — see LICENSE."*; verweist auf den **HUI Audio Corpus** — in Issue #9 wörtlich: *„using the HUI Audio Corpus (326 hours, 122 speakers, CC0)"*; ausdrücklich *„Not a redistributable training dataset"* (betrifft die Daten, nicht die Gewichte). → https://github.com/semidark/kikiri-tts
- **Die Stimme:** „Martin Harbecke" ist ein realer **LibriVox-Vorleser** (Reader-Seite: https://librivox.org/reader/6719). LibriVox-Aufnahmen sind Public Domain; der daraus gebaute HUI-Audio-Corpus-German ist **CC0** (IISYS Hof). → https://opendata.iisys.de/dataset/hui-audio-corpus-german/ , https://github.com/iisys-hof/HUI-Audio-Corpus-German

**Bewertung (sauber benannt):** Alle vier Glieder der Kette tragen Apache-2.0, die Basis-Gewichte (hexgrad) sind Apache-2.0, die Stimme stammt aus Public-Domain-Material. **Mitverbreitung des Martin-ONNX ist damit vertretbar** — deutlich mehr als die Rückfallposition „nur verlinken". Ehrlich offen bleiben zwei Punkte, die ins Projekt-NOTICE gehören: (a) die Apache-Erklärung stammt vom jeweiligen Uploader; ob dieser alle Rechte an den Zwischenschritten hielt, ist nicht unabhängig belegt; (b) welcher TTS die ~30.800 synthetischen Basissamples erzeugte, ist nirgends benannt — Ausgaben geschlossener TTS können Nutzungsbedingungen tragen (dieselbe Unschärfe besteht allerdings auch beim offiziellen Kokoro-82M selbst, das laut eigener Karte teils auf „synthetic audio from closed TTS models" trainiert wurde). Rückfalloption bleibt: Modell nicht ins Repo/Release, sondern Download-Anleitung + Prüfsumme — das verlagert die Restunsicherheit zum Nutzer, beseitigt sie aber nicht; deshalb im NOTICE in beiden Fällen die Kette dokumentieren.

## 5. Übrige Komponenten

- **ONNX Runtime**: *„This project is licensed under the MIT License."* — mitverteilbar; das offizielle Android-AAR bringt ThirdPartyNotices mit, die in die eigenen Notices übernommen werden. → https://github.com/microsoft/onnxruntime
- **phonemizer** (bootphon): **GPL-3.0** — README wörtlich: *„This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License … either version 3 of the License"*. Betrifft nur die Windows-Pipeline; auf Android durch den direkten libespeak-ng-Aufruf ersetzt (was am GPL-Ergebnis nichts ändert, da espeak-ng selbst GPL ist). → https://github.com/bootphon/phonemizer
- **praat-parselmouth**: *„Parselmouth is released under the GNU General Public License, version 3 or later."*; Praat mit Abhängigkeiten GPL-3.0+, Praats eigener Quellcode GPL-2.0+. Für Android ohnehin zu ersetzen. Wichtig für den Ersatz: **keinen Praat-Code portieren**, sondern TD-PSOLA unabhängig implementieren — der Algorithmus als solcher ist nicht schutzfähig, und die PSOLA-Patente (France Télécom, Anmeldungen Ende der 1980er/1990er) sind seit Langem abgelaufen. → https://github.com/YannickJadoul/Parselmouth

## 6. Empfehlung für dieses Projekt

**Projektlizenz: GPL-3.0-or-later.** Sie ist durch das Bündeln von libespeak-ng + espeak-ng-data faktisch vorgegeben, und alle anderen Komponenten (Apache-2.0, MIT, BSD, CC0) sind einbahnfrei GPL-3.0-kompatibel. Eine permissive Projektlizenz wäre nur um den Preis eines G2P-Wechsels (goruut) zu haben — mit unvermessenem Qualitätsrisiko genau an der Stelle, an der der sherpa-Weg scheiterte. GPL-3.0 passt zudem zum Feld (espeak-ng-App, RHVoice) und zum erklärten OpenSource-Ziel.

**Dateien im Repo:**
- `LICENSE` — GPL-3.0-Volltext (SPDX: GPL-3.0-or-later; SPDX-Header in den Quelldateien).
- `THIRD-PARTY-NOTICES.md` — je Komponente: Name, Version, Lizenz, Quelle, Rolle. Mindestens: espeak-ng (GPL-3.0+, Code **und** Sprachdaten), Kokoro-82M-Gewichte (Apache-2.0, hexgrad, inkl. der CC-BY-Trainingsdaten-Attributionen Koniwa/SIWIS aus der Modellkarte), kokoro-onnx (MIT), ONNX Runtime (MIT + dessen ThirdPartyNotices), misaki (Apache-2.0), fugashi (MIT), MeCab (BSD gewählt aus GPL/LGPL/BSD), unidic-lite (MIT/WTFPL, UniDic-Daten BSD), cppjieba/jieba (MIT), pypinyin (MIT), pinyin-to-ipa (MIT), Martin-Kette (Apache-2.0: Godelaune ← kikiri-german-martin ← kikiri-Basis ← Kokoro-82M; Stimme LibriVox/HUI CC0) mit den zwei offenen Punkten aus Abschnitt 4.
- `NOTICE` — Apache-2.0-§4(d)-Hinweise (Kokoro, misaki, kikiri/Godelaune) gesammelt.
- GPL-Quellcodeangebot: erfüllt durch das öffentliche Repo; im Play-Store-Eintrag und in der App (Über-Seite) auf Repo + Lizenz verlinken. Der gebündelte espeak-ng-Stand (Commit/Patches) muss aus dem Repo reproduzierbar sein.

**Verteilung der Modelle:** kokoro-v1.0(.fp16/.int8).onnx und voices-v1.0.bin: **mitverteilen erlaubt** (Apache-2.0). Martin-ONNX: mitverteilen vertretbar (Apache-Kette), alternativ konservativ nur Download-Anleitung — Entscheidung des Maintainers, beide Wege im NOTICE dokumentieren. espeak-ng-data: mitverteilen unter GPL. **Auslieferungsweg:** Install-Time-Asset-Packs oder Eigen-Download; die proprietäre Play-Core-Bibliothek (On-Demand/Fast-Follow-Packs) nicht in die GPL-App linken (*„You will not modify … or create derivative works of Redistributable Code"* ist GPL-unverträglich). **Kanäle:** Google Play + F-Droid (F-Droid verlangt ohnehin den vollständig freien Build und räumt die Play-App-Signing-Debatte ab).

---

### Quellen (Kern)
- https://huggingface.co/hexgrad/Kokoro-82M
- https://github.com/thewh1teagle/kokoro-onnx (+ /releases)
- https://github.com/espeak-ng/espeak-ng (+ /blob/master/COPYING)
- https://f-droid.org/packages/com.reecedunn.espeak/
- https://github.com/RHVoice/RHVoice/wiki/License , https://f-droid.org/packages/com.github.olga_yakovleva.rhvoice.android/
- https://developer.android.com/guide/playcore
- https://github.com/hexgrad/misaki
- https://github.com/polm/fugashi , https://taku910.github.io/mecab/ , https://github.com/polm/unidic-lite
- https://github.com/fxsjy/jieba , https://github.com/yanyiwu/cppjieba , https://github.com/mozillazg/python-pinyin , https://github.com/stefantaubert/pinyin-to-ipa
- https://huggingface.co/Godelaune/Kokoro-82M-ONNX-German-Martin , https://huggingface.co/kikiri-tts/kikiri-german-martin , https://huggingface.co/kikiri-tts/kikiri-german-base-51speakers-synthetic , https://github.com/semidark/kikiri-tts
- https://librivox.org/reader/6719 , https://opendata.iisys.de/dataset/hui-audio-corpus-german/ , https://github.com/iisys-hof/HUI-Audio-Corpus-German
- https://github.com/microsoft/onnxruntime , https://github.com/bootphon/phonemizer , https://github.com/YannickJadoul/Parselmouth
- https://github.com/rhasspy/gruut , https://github.com/neurlang/goruut

*Hinweis: Zitate wurden über Seitenabrufe der genannten Quellen erhoben; vor der Veröffentlichung die LICENSE-/NOTICE-Dateien der tatsächlich gebündelten Versionen wörtlich ins Repo übernehmen. Kein Rechtsrat.*