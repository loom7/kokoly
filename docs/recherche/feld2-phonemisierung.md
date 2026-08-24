# Recherchefeld 2: Phonemisierung auf Android (espeak-ng-Port, misaki-Ersatz für ja/zh, sherpa-Vergleich, Hindi)

> Rechercheablage vom 24.08.2026, erhoben für den PROJEKTPLAN. Inhalt vor Schönheit.

## Kernaussagen

- espeak-ng: Der richtige Weg ist ein eigener NDK-Build (CMake, statisch) mit dünnem JNI um espeak_TextToPhonemes — exakt die Funktion, die phonemizer auf Windows per ctypes aufruft (phonememode = ord('_')<<8 | 0x02 = IPA + Separator). Verhaltensparität zur Windows-App ist damit prinzipiell erreichbar und per Goldwert-Tests belegbar.
- Betonungszeichen ˈ ˌ stehen in der IPA-Ausgabe von espeak_TextToPhonemes IMMER drin; phonemizers with_stress=True heißt nur 'nichts entfernen'. Sprachwechselmarken (en)…(de) kann KEINE espeak-API unterdrücken — phonemizer (remove-flags) und piper-phonemize filtern beide nachträglich (Regex \(.+?\) auf der Phonemzeile). Der Port muss außerdem phonemizers Interpunktions-Erhalt nachbauen (Satzzeichen vor espeak abtrennen, danach wieder einsetzen), sonst gehen Kokoros Pausen/Prosodie-Zeichen verloren.
- espeak-ng-data: komplett 18 MB (114 Wörterbücher), beschneidbar auf ~1,4 MB für die 7 espeak-Sprachen des Projekts: Kern phontab+phonindex+phondata+intonations ≈ 680 KB, dazu de 69 KB, en 168 KB (deckt en-us UND en-gb), es 49 KB, fr 64 KB, it 154 KB, pt 76 KB, hi 92 KB, plus wenige lang-Dateien. Beschneidung = Dateien weglassen, pro Sprache eine Datei. Daten müssen ins filesDir kopiert werden (C-Bibliothek liest nicht aus dem APK) — das Muster EspeakData.ensure() existiert fertig in CodeTest.
- Hindi bestätigt: hi_dict (92 KB) liegt in espeak-ng-data, lokal geprüft; die Windows-Hörprobe ost-hi-hf_alpha.wav existiert bereits. Kein Zusatzaufwand über die espeak-Einbindung hinaus.
- Lizenzfolge: espeak-ng ist GPL-3.0+ — die gebündelte App wird damit de facto GPL-3.0 (Präzedenzfall: Piper heißt jetzt 'piper1-gpl', weil espeak-ng eingebettet wurde). Mit der geplanten OpenSource-Veröffentlichung vereinbar (Apache-2.0-Teile dürfen rein), aber die Projektlizenz ist damit faktisch festgelegt.
- misaki/zh (der Windows-Pfad, ZHG2P legacy) ist klein und vollständig portierbar: cn2an (Zahlen) → Satzzeichen-Map → jieba.lcut → pypinyin lazy_pinyin(TONE3) → pinyin_to_ipa (reine MIT-Tabelle, 284 Zeilen) → Tonkonturen zu Pfeilen →↗↘↓. KEINE Ton-Sandhi im Legacy-Pfad. Kotlin-Port: jieba-analysis (pure Java, Apache-2.0, Maven Central) oder cppjieba (MIT) + exportierte pypinyin-Tabellen (772 KB + 2,5 MB) + Tabellenport. Aufwand ~4–6 Tage, Daten ~8 MB (packbar auf ~4 MB).
- misaki/ja läuft auf Windows im cutlet-Standardpfad: fugashi/MeCab + unidic-lite (249 MB entpackt: sys.dic 179 MB + matrix.bin 69 MB!) + HEPBURN-Tabelle (164 Einträge) + ja_words.txt (1,9 MB) + num2kana — OHNE Pitch-Akzent (die Pfeil-Logik im pyopenjtalk-Zweig ist auskommentiert). Empfehlung: ja in Ausbaustufe 2. Dann zuerst Kuromoji-ipadic evaluieren (12,7 MB Jar, pure Java, Apache-2.0, liefert Katakana-Lesungen) + Cutlet-Tabellenport, ~5–8 Tage; volle Parität ginge nur mit MeCab-NDK-Build + unidic-lite (~47 MB Download / 250 MB entpackt) — für 'wenig RAM/Speicher' unverhältnismäßig.
- sherpa-onnx bestätigt die Projektlehre: Kokoro läuft dort über Wort→Token-Lexika (lexicon-zh.txt 2,36 MB, lexicon-en 6+6 MB) + Greedy-PhraseMatcher (kein cppjieba im Kokoro-Pfad) + espeak-Fallback durch tokens.txt (die verdächtigte ich-Laut-Filterstelle). Japanisch wird dort GAR NICHT unterstützt (fiele auf espeak zurück = unbrauchbar). Ihr zh-Lexikon wurde selbst mit misaki aus den pypinyin-Wörterbüchern erzeugt — der eigene misaki-Port ist also mindestens gleichwertig (echte jieba-Segmentierung statt Greedy-Match).
- Aufwandssumme Empfehlung: espeak-NDK-Build+JNI+Datenkopie 3–5 Tage; phonemizer-Nachbau in Kotlin (Interpunktionserhalt, remove-flags, Stress behalten, ʏ→y, Separatoren) 1–2 Tage; zh-Port 4–6 Tage (+~8 MB); ja Ausbaustufe 2 mit 5–8 Tagen (+12,7 MB) — Stufe 1 liefert damit 8 Sprachen (7 espeak + de) in ~1 Woche Phonemisierungs-Arbeit mit nur ~1,4 MB espeak-Daten und ~2 MB Bibliothek.

## Bericht

# Recherchefeld 2: Phonemisierung auf Android

Stand 24.08.2026. Alle lokalen Größen selbst gemessen (Pfade angegeben), alle Quelltextaussagen gegen die Originaldateien geprüft.

---

## 1. espeak-ng als Android-Bibliothek (7 Sprachen: de, en-us, en-gb, es, fr-fr, it, pt-br + hi)

### 1.1 Die richtige API — und was die Windows-App wirklich tut

Die Windows-App ruft phonemizer auf; phonemizer ruft per ctypes **`espeak_TextToPhonemes`** auf (`.venv/Lib/site-packages/phonemizer/backend/espeak/wrapper.py`, Z. 378–425):

```
phonemes_mode = ord("_") << 8 | 0x02      # Bit 1 (0x02) = IPA, Bits 8–23 = Trennzeichen '_'
espeak_Initialize(0x02, 0, data_path, 0)  # AUDIO_OUTPUT_SYNCHRONOUS, kein Audio nötig
```

Laut `speak_lib.h`: Bit 1 = IPA als UTF-8, Bits 8–23 = Separator zwischen Phonemnamen. Die Funktion liefert die Phoneme **klauselweise** (bis Satzende/Komma) und muss über den textptr in Schleife aufgerufen werden.

Drei hart erarbeitete Punkte, die der Port exakt nachbauen muss:

1. **Betonung:** Die IPA-Ausgabe enthält ˈ ˌ **immer**. phonemizers `with_stress=True` bedeutet nur: die Stress-Regex (`[ˈˌ'-]+`, espeak.py Z. 33) wird NICHT angewandt. Der Android-Port lässt die Zeichen einfach stehen — nichts zu tun.
2. **Sprachwechselmarken:** Es gibt **keine espeak-API**, die `(en)…(de)` unterdrückt. phonemizer (`language_switch.py` Z. 84: Regex `\(.+?\)`) und piper-phonemize (`phonemize.cpp`: "Filter out (lang) switch (flags)") filtern beide **nachträglich** im Aufrufer. Das ist sicher, weil echte Klammern aus dem Text nie in die Phonemzeile gelangen (siehe 3.) — genau die Eigenschaft, die `phonemisierung.py` dokumentiert.
3. **Interpunktion:** `espeak_TextToPhonemes` gibt Satzzeichen nicht aus. phonemizer erhält sie über seine Punctuation-Klasse (vor dem Aufruf abtrennen, danach wieder einfügen — `preserve_punctuation=True`). Kokoro braucht die Zeichen für Pausen/Prosodie (gemessen: 0,22/0,35 s). **Dieser Baustein gehört mit in den Kotlin-Port** und ist der subtilste Teil des Nachbaus.

Zusatzregeln aus der Windows-Pipeline, die im Kotlin-Nachbau landen: `ʏ→y` (LAUT_ERSATZ), NFC + Leerraum-Zusammenfassung vor espeak, sichtbarer Verlustbericht (Vokabularfilter), ein globales Lock (espeak ist nicht threadfest — Windows nutzt `_espeak_lock`, Android nimmt einen Mutex um alle JNI-Aufrufe).

### 1.2 Drei Einbindungswege im Vergleich

| Weg | Was es ist | Bewertung |
|---|---|---|
| **Offizieller android/-Zweig im espeak-ng-Repo** | Komplette TTS-App (com.reecedunn.espeak) mit eigenem JNI, gradle, TextToSpeechService | Falsches Vehikel: JNI ist auf Sprachsynthese ausgelegt, nicht auf `TextToPhonemes`; als **Beweis der NDK-Baubarkeit** und Nachschlagewerk nützlich |
| **sherpa-onnx-Weg** | Bündelt den csukuangfj/espeak-ng-Fork (basiert auf Rhasspys Piper-Patches) per CMake statisch, alle Extras aus (`USE_ASYNC/MBROLA/KLATT/… OFF`), läuft produktiv in deren Android-AARs | **Vorlage für den eigenen Build**: dieselben CMake-Schalter übernehmen, aber NICHT sherpas Kokoro-Frontend (siehe Abschnitt 3) |
| **piper-phonemize** | C++-Bibliothek um Rhasspys espeak-ng-Fork mit Zusatzfunktion `espeak_TextToPhonemesWithTerminator` (liefert das Klausel-Endzeichen mit) | Keine offiziellen Android-Builds; Projekt eingefroren, Nachfolger **piper1-gpl** bettet espeak-ng direkt ein. Die Terminator-Idee ist nützlich, aber nicht nötig — die Windows-App arbeitet auch ohne |

**Empfehlung: eigener NDK-Build** (CMake + Android-Toolchain, statische Bibliothek, Features wie sherpa abschalten) plus **eigenes dünnes JNI** (~150 Zeilen: init(dataPath), setVoice(lang), textToPhonemes(text) in Schleife, terminate). Damit bleibt der Aufrufpfad identisch zur vermessenen Windows-Pipeline. Die Bibliothek liegt in der Größenordnung 1–2 MB pro ABI (Schätzung; sherpa faltet sie in ihre JNI-so).

**Zwei Android-Fallen, beide in CodeTest bereits gelöst:** espeak-ng öffnet seine Wörterbücher **per Dateipfad** und kann nicht aus dem APK/AssetManager lesen → beim ersten Start nach `filesDir` kopieren. CodeTest hatte dafür `EspeakData.ensure()` (siehe `NovaApp.kt`, retireEspeakData(): „espeak-ng is a C library that opens its dictionaries by path and cannot read from the AssetManager"). Das Kopier- und Aufräum-Muster ist dort erprobt.

### 1.3 Datengröße und Beschneidung (lokal gemessen)

Gemessen an `.venv/Lib/site-packages/espeakng_loader/espeak-ng-data` (dieselben Daten, mit denen alle Windows-Messungen entstanden):

| Bestandteil | Größe | Nötig? |
|---|---|---|
| Gesamt (114 Wörterbücher) | **18 MB** | nein |
| Kern: phondata 555 KB, phontab 59 KB, phonindex 43 KB, intonations 2,3 KB | **~680 KB** | immer |
| de_dict 69 KB · en_dict 168 KB (deckt en-us **und** en-gb) · es_dict 49 KB · fr_dict 64 KB · it_dict 154 KB · pt_dict 76 KB (deckt pt-br) · **hi_dict 92 KB** | **~672 KB** | ja (7 Sprachen) |
| lang/-Definitionen (nur die benötigten, je wenige KB) | ~20 KB | ja |
| voices/ (Varianten), alle übrigen dicts (größte: ru 8,2 MB, cmn 1,5 MB, yue 552 KB) | 16+ MB | nein |

**Getrimmt: ~1,4 MB** statt 18 MB. Beschneidung ist gefahrlos, weil espeak pro Sprache genau eine dict-Datei beim `SetVoice` lädt — fehlende Dateien betreffen nur nicht ausgelieferte Sprachen. (Für die Einstellungs-Abwahl von Sprachen genügt es, die Stimme nicht anzubieten; die Daten können trotzdem komplett getrimmt beiliegen.)

### 1.4 Lizenzfolge (entscheidend für OpenSource)

espeak-ng ist **GPL-3.0+**. Wer es einbettet, stellt die App effektiv unter GPL-3.0 — der Piper-Nachfolger heißt exakt deshalb **piper1-gpl**. Für die geplante OpenSource-Veröffentlichung ist das machbar (Apache-2.0-Bausteine wie misaki-Portierungen, ONNX Runtime (MIT) usw. dürfen in ein GPLv3-Projekt einfließen, nicht umgekehrt). **Die Projektlizenz ist damit faktisch auf GPL-3.0 festgelegt** — das gehört als Entscheidung ins Lizenzdokument.

**Aufwand espeak-Teil: 3–5 Tage** (Build je ABI, JNI, Datenkopie, Golden-Tests gegen Windows-Phonemausgaben) **+ 1–2 Tage** für den phonemizer-Nachbau in Kotlin (Interpunktionserhalt, remove-flags-Regex, ʏ→y, Separator-/Leerzeichenlogik).

---

## 2. ja/zh ohne Python — was misaki wirklich tut (Quelltext gelesen)

### 2.1 Chinesisch: klein, sauber, portierbar

Die Windows-App nutzt `ZHG2P()` ohne Argumente = **Legacy-Pfad** (`misaki/zh.py`, nur 85 Zeilen, komplett gelesen):

1. `cn2an.transform(text, 'an2cn')` — arabische Ziffern → chinesische Zahlwörter (Paket 177 KB, MIT)
2. `map_punctuation` — CJK-Satzzeichen → westliche (im Quelltext vollständige Tabelle)
3. Segmentweise Han/Nicht-Han-Trennung (Regex `[\u4E00-\u9FFF]`)
4. **`jieba.lcut`** — Wortsegmentierung (dict.txt **4,9 MB**; idf.txt der analyse-Sparte wird NICHT gebraucht)
5. **`pypinyin.lazy_pinyin(w, Style.TONE3, neutral_tone_with_five=True)`** — Pinyin je Wort mit Tonziffer (Daten: pinyin_dict.json **772 KB** + phrases_dict.json **2,5 MB**; das 108-MB-Paket pypinyin_dict wird nur vom v1.1-Frontend gebraucht, nicht vom Legacy-Pfad)
6. **`pinyin_to_ipa`** (`misaki/transcription.py`, 284 Zeilen) — reine Tabellen-/Regelfunktion ohne externe Daten, „ADAPTED from stefantaubert/pinyin-to-ipa", **MIT**
7. `retone` — Tonkonturen → Pfeile: ˧˩˧→**↓**, ˧˥→**↗**, ˥˩→**↘**, ˥→**→**; U+0329/U+032F-Bereinigung (chr(815))

**Wichtig: keine Ton-Sandhi im Legacy-Pfad** — Parität verlangt also keine Sandhi-Logik. Die Pfeile stehen im Kokoro-Vokabular (in der Windows-App belegt: 0 % Verlust).

**Android-Gegenstücke, alle vorhanden:**
- Segmentierung: **jieba-analysis** (pure Java, **Apache-2.0**, Maven Central `com.huaban:jieba-analysis`) oder **cppjieba** (C++, MIT) — pure Java bevorzugt: kein NDK, kein JNI
- Pinyin: pypinyin-Tabellen beim Build am PC nach Kotlin-lesbarem Binärformat exportieren (Wort→TONE3-Pinyin; MIT-Daten aus mozillazg/pinyin-data bzw. phrase-pinyin-data)
- IPA: `pinyin_to_ipa` 1:1 nach Kotlin portieren (Maps + Kombinatorik, keine Python-Spezifika)
- Zahlen: cn2an-Minimalport (nur `an2cn`-Richtung, ~1 Tag)

**Aufwand zh: 4–6 Tage** inkl. Goldwert-Tests gegen die Windows-misaki-Ausgaben. **Daten ~8 MB roh** (jieba 4,9 + Pinyin 3,3), binär gepackt eher ~4 MB. RAM: Tabellen lazy laden, nur bei aktivierter zh-Stimme.

*Abkürzungsoption:* sherpas `lexicon-zh.txt` (2,36 MB, Wort→Phoneme, mit misaki erzeugt — siehe 3.) plus Greedy-Matcher wäre in 1–2 Tagen angebunden, verliert aber jiebas echte Segmentierung und das Wachstum eigener Regeln. Nicht empfohlen, aber als Rückfallebene dokumentiert.

### 2.2 Japanisch: der ehrliche Befund

`JAG2P` hat zwei Modi (`misaki/ja.py` Z. 251: `version='cutlet'` ist **Standard**) — die Windows-App läuft also im **cutlet-Modus**:

- **cutlet-Modus** (misakis MIT-adaptierter Fork von polm/cutlet, 423 Zeilen): fugashi/**MeCab** mit **unidic-lite** → Lesung (`w.feature.pron or w.feature.kana`), kata2hira, **HEPBURN-Tabelle** (164 Einträge Kana→IPA: ɕi, kʲi, ɯ, ʥi …), ja_words.txt (1,9 MB), num2kana (318 Zeilen). **Kein Pitch-Akzent** — die Ausgabe ist flach.
- **pyopenjtalk-Modus**: `run_frontend` liefert pron+Akzent, M2P-Tabelle (193 Einträge), Akzentlogik vorhanden, aber **die Pfeil-Einfügung ist im Quelltext auskommentiert** (ja.py Z. 311–323) — zweitklassig gepflegt.

**Das Gewichtsproblem (lokal gemessen):** unidic-lite = **249 MB entpackt** (sys.dic 179 MB + matrix.bin 69 MB), ~47 MB als Download. Der pyopenjtalk-Weg (OpenJTalk ist C, BSD; Wörterbuch open_jtalk_dic_utf_8-1.11 ~23 MB komprimiert, ~100 MB entpackt) ist kleiner, aber misakis pyopenjtalk-Zweig ist der ungepflegte.

**Alternativen für Android:**

| Variante | Bausteine | Größe | Aufwand | Risiko |
|---|---|---|---|---|
| A: volle Parität | MeCab per NDK + unidic-lite + Cutlet-Port | ~47 MB Download / 250 MB entpackt | 4–6 Tage | Speicher sprengt das Ziel „wenig RAM/Energie"; mmap hält RAM klein, Storage bleibt |
| **B: empfohlen (Stufe 2)** | **Kuromoji-ipadic** (pure Java, **Apache-2.0**, 12,7 MB Jar inkl. Wörterbuch, liefert Katakana-Lesungen) + Port der HEPBURN/M2P-Tabelle + num2kana | **~13 MB** | **5–8 Tage** (inkl. Qualitätsvergleich) | Lesungen aus ipadic weichen gelegentlich von unidic ab — gegen Windows-Goldwerte messen |
| C: OpenJTalk-Weg | OpenJTalk C per NDK + naist-jdic + M2P-Port (+ später echte Pitch-Pfeile möglich) | ~23 MB komprimiert / ~100 MB entpackt | 7–10 Tage | misaki-Zweig zweitklassig; mehr Neuland |

**Ehrliche Empfehlung: ja und zh entkoppeln.** zh ist billig und risikoarm → Ausbaustufe 1 möglich (oder früh in Stufe 2). **ja in Ausbaustufe 2** mit Variante B zuerst; da die Windows-App ohnehin flach (ohne Pitch-Akzent) spricht, ist die erreichbare Qualität gleichwertig zur heutigen Referenz. Ein 250-MB-Sprachpaket als optionaler Download (Variante A) bleibt als spätere Option dokumentiert.

---

## 3. Wie sherpa-onnx es für Kokoro macht (Quelltext `kokoro-multi-lang-lexicon.cc`)

- **Han-Erkennung per Regex** `[\u4e00-\u9fff]`; chinesische Abschnitte gegen **Lexika** aufgelöst: `lexicon-zh.txt` (2,36 MB) im Format **Wort → Token-Folge**, Matching über einen eigenen **PhraseMatcher** (Greedy über alle Lexikonwörter) — **cppjieba wird im Kokoro-Pfad NICHT benutzt** (das dict/-Verzeichnis und die FSTs number-zh/date-zh/phone-zh dienen der Textnormalisierung bzw. anderen Frontends wie MeloTTS).
- **OOV bei Chinesisch: wird stillschweigend übersprungen** („Skip OOV").
- **Nicht-Chinesisch:** erst Lexikon (lexicon-us-en.txt 5,96 MB / lexicon-gb-en.txt 6,37 MB), OOV dann über **espeak-ng**, dessen Phoneme über **tokens.txt** auf IDs abgebildet werden — genau die Filterstelle, die im Verdacht steht, damals den deutschen ich-Laut („itsch") zerstört zu haben. Die Lehre „eigene Pipeline portieren, nicht sherpas Frontend" wird durch den Quelltext gestützt.
- **Japanisch: nicht unterstützt.** Kein ja-Lexikon im Modellpaket; Kana/Kanji fielen in den espeak-Zweig — das ist exakt der auf Windows als unbrauchbar belegte Weg („Chinese letter").
- **Deren zh-Lexikon wurde mit misaki erzeugt:** `scripts/kokoro/v1.0/generate_lexicon_zh.py` ruft `misaki.zh.ZHG2P` über die pypinyin-Wörterbücher (pinyin_dict + phrases_dict) auf und schreibt Wort→Phoneme. Folgerung: der eigene misaki-Legacy-Port (Abschnitt 2.1) ist dem sherpa-Weg **mindestens gleichwertig** — mit echter jieba-Segmentierung statt Greedy-Matching sogar überlegen, und er behält die Tonpfeile-Kette identisch zur vermessenen Windows-Pipeline.
- Positiv übernehmbar: sherpas **CMake-Einbindung von espeak-ng** (Fork csukuangfj/espeak-ng, statisch, alle Audio-Extras OFF) als Vorlage für den eigenen NDK-Build; ihre Android-AARs beweisen, dass genau dieser espeak-Build auf Android produktiv läuft.

---

## 4. Hindi über espeak: bestätigt

`hi_dict` (92 KB) liegt in espeak-ng-data (lokal geprüft im espeakng_loader-Datenverzeichnis; dieselben Daten erzeugten die vorhandene Windows-Hörprobe `ost-hi-hf_alpha.wav`). Hindi kostet auf Android nichts extra: dict beilegen, Stimme „hi" anbieten.

---

## 5. Empfehlung je Sprache (Aufwand, Daten, Weg)

| Sprache(n) | Empfohlener Weg | Aufwand | Daten auf Gerät |
|---|---|---|---|
| de, en-us, en-gb, es, fr-fr, it, pt-br, hi | Eigener espeak-ng-NDK-Build + dünnes JNI um espeak_TextToPhonemes + Kotlin-Nachbau der phonemizer-Logik (Interpunktionserhalt, remove-flags, Stress behalten, ʏ→y) | 3–5 Tage (Build/JNI) + 1–2 Tage (Nachbau) | ~1,4 MB espeak-ng-data (getrimmt) + ~1–2 MB Bibliothek/ABI |
| zh | Kotlin-Port von misaki-Legacy: jieba-analysis (Java) + exportierte pypinyin-Tabellen + pinyin_to_ipa-Port + cn2an-Mini | 4–6 Tage | ~8 MB roh (~4 MB gepackt), lazy geladen |
| ja | **Ausbaustufe 2.** Kuromoji-ipadic (pure Java) + HEPBURN-Tabellenport; Parität gegen Windows-Goldwerte messen. Rückfallebene: MeCab+unidic-lite als optionaler 47-MB-Download | 5–8 Tage | ~13 MB (Variante B) |

Durchgängige Absicherung: **Goldwert-Tests** — die Windows-Pipeline schreibt für einen festen Textkorpus die Phonemketten heraus; JVM-Tests (Testkultur aus CodeTest, ohne Robolectric) vergleichen die Kotlin-Ausgaben zeichengenau. Das macht die drei hart erarbeiteten espeak-Erkenntnisse (Stress, Flaggen, Verlustsichtbarkeit) dauerhaft prüfbar.

**Lizenzübersicht der Bausteine** (dist-info bzw. Repos geprüft): espeak-ng **GPL-3.0+** (prägt die App-Lizenz); misaki **Apache-2.0**; jieba, pypinyin, cn2an, cutlet(-Upstream), pinyin-to-ipa, cppjieba **MIT**; jieba-analysis, Kuromoji **Apache-2.0** (ipadic-Wörterbuch mit eigener permissiver Lizenz — Text beilegen); unidic-lite: MIT-Wrapper, Wörterbuch dreifach BSD/LGPL/GPL (BSD wählbar); fugashi MIT/BSD-3; pyopenjtalk MIT (OpenJTalk selbst BSD-3).

---

## Quellen

**Lokal gemessen/gelesen:**
- `C:/Users/jtill/Documents/_Claude/TTS Test/.venv/Lib/site-packages/espeakng_loader/espeak-ng-data` (18 MB; Kern 680 KB; dict-Größen; hi_dict 92 KB)
- `…/site-packages/phonemizer/backend/espeak/{wrapper.py, espeak.py, language_switch.py}` (phonememode-Bits, Stress-Regex, remove-flags-Regex)
- `…/site-packages/misaki/{zh.py, ja.py, cutlet.py, transcription.py, num2kana.py, data/ja_words.txt}` (Pipelines, Standard-Modi, Tabellen)
- `…/site-packages/{unidic_lite (249 MB), jieba (dict.txt 4,9 MB), pypinyin (772 KB + 2,5 MB), pypinyin_dict (108 MB), cn2an}` + dist-info-Lizenzen
- `C:/Users/jtill/Documents/_Claude/TTS Test/phonemisierung.py` (Referenzverhalten)
- `C:/Users/jtill/Documents/_Claude/CodeTest/app/src/main/java/de/tilly/nova/NovaApp.kt` (EspeakData-/filesDir-Muster), `app/build.gradle.kts` (sherpa-AAR)

**Netz:**
- [piper-phonemize phonemize.cpp](https://github.com/rhasspy/piper-phonemize) — espeak-Aufruf, IPA 0x02, Flaggenfilter
- [phonemizer](https://github.com/bootphon/phonemizer) — Referenz-Backend der Windows-App
- [espeak-ng speak_lib.h](https://github.com/espeak-ng/espeak-ng/blob/master/src/include/espeak-ng/speak_lib.h) — espeak_TextToPhonemes-Doku; [android/-Zweig](https://github.com/espeak-ng/espeak-ng/tree/master/android)
- [hexgrad/misaki](https://github.com/hexgrad/misaki) — ja.py/zh.py/cutlet.py/transcription.py; [stefantaubert/pinyin-to-ipa](https://github.com/stefantaubert/pinyin-to-ipa) (MIT-Ursprung)
- [sherpa-onnx kokoro-multi-lang-lexicon.cc](https://github.com/k2-fsa/sherpa-onnx/blob/master/sherpa-onnx/csrc/kokoro-multi-lang-lexicon.cc); [cmake/espeak-ng-for-piper.cmake](https://github.com/k2-fsa/sherpa-onnx/blob/master/cmake/espeak-ng-for-piper.cmake); [scripts/kokoro/v1.0/generate_lexicon_zh.py](https://github.com/k2-fsa/sherpa-onnx/blob/master/scripts/kokoro/v1.0/generate_lexicon_zh.py); [Kokoro-Modelldoku](https://k2-fsa.github.io/sherpa/onnx/tts/pretrained_models/kokoro.html)
- [csukuangfj/kokoro-multi-lang-v1_0 (Dateigrößen)](https://huggingface.co/csukuangfj/kokoro-multi-lang-v1_0/tree/main)
- [piper1-gpl (GPL-Präzedenz)](https://github.com/OHF-Voice/piper1-gpl)
- [huaban/jieba-analysis (Java, Apache-2.0)](https://github.com/huaban/jieba-analysis); [cppjieba (MIT)](https://github.com/yanyiwu/cppjieba); [fxsjy/jieba](https://github.com/fxsjy/jieba)
- [atilika/kuromoji (Apache-2.0, kuromoji-ipadic 12,7 MB)](https://github.com/atilika/kuromoji); [Maven Central](https://central.sonatype.com/artifact/com.atilika.kuromoji/kuromoji-ipadic)
- [polm/cutlet (MIT-Upstream)](https://github.com/polm/cutlet); [pyopenjtalk](https://github.com/r9y9/pyopenjtalk); [Open-JTalk-Wörterbuch](https://sourceforge.net/projects/open-jtalk/files/Dictionary/open_jtalk_dic-1.11/)