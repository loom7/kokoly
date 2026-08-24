# Recherchefeld 1: Die Android-TTS-Engine-Schnittstelle (TextToSpeechService)

> Rechercheablage vom 24.08.2026, erhoben für den PROJEKTPLAN. Inhalt vor Schönheit.

## Kernaussagen

- Fünf Methoden sind Pflicht (onIsLanguageAvailable, onGetLanguage, onLoadLanguage, onStop, onSynthesizeText); die vier Voice-Methoden (onGetVoices, onIsValidVoiceName, onLoadVoice, onGetDefaultVoiceNameFor) sind ab API 21 der eigentliche Kern: setLanguage() einer fremden App läuft seit API 21 intern über onGetDefaultVoiceNameFor + loadVoice — wer die Voice-Methoden sauber implementiert, bedient setLanguage UND setVoice aus einer Logik.
- onSynthesizeText läuft auf genau einem dedizierten Synthese-Thread (HandlerThread 'SynthThread') und MUSS blockieren, bis die Synthese fertig ist; onStop kommt von einem anderen Thread (Binder) — Abbruch daher über ein volatiles Flag, das in der Audio-Chunk-Schleife geprüft wird (exakt das RHVoice-Muster). Auch onIsLanguageAvailable/onLoadLanguage kommen auf Binder-Threads → thread-sicher halten.
- Streaming-Protokoll: callback.start(sampleRate, format, channels) → n × audioAvailable(buffer, offset, length) mit length ≤ getMaxBufferSize() (im AOSP-Wiedergabepfad 8192 Bytes) → done() IMMER, auch nach Fehler oder Stop. Formate: ENCODING_PCM_8BIT/16BIT, PCM_FLOAT ab Android N. audioAvailable blockiert bei vollem Wiedergabepuffer (natürliche Backpressure — energiefreundlich, die Synthese läuft der Wiedergabe nie weit voraus) und liefert nach Stop STOPPED als Rückgabewert.
- Rate und Pitch kommen als SynthesisRequest.getSpeechRate()/getPitch() an: int, 100 = normal (NICHT float 1.0). RHVoice teilt schlicht durch 100 → Faktor. Sprachcodes im Request sind ISO-3 ('deu', 'DEU'), nicht 'de'.
- Manifest-Rezept (aus AOSP-Beispiel-Engine, espeak-ng und RHVoice identisch): <service exported='true'> mit intent-filter android.intent.action.TTS_SERVICE + category DEFAULT und meta-data android.speech.tts → res/xml/tts_engine.xml mit <tts-engine android:settingsActivity=…/>; dazu Activities für CHECK_TTS_DATA und GET_SAMPLE_TEXT. Mehr braucht es nicht, um in den Systemeinstellungen als Engine wählbar zu sein; keine Bind-Permission.
- Lebenszyklus: gebundener Dienst, stirbt nach Unbind; der Framework-Code lädt beim onCreate bereits die Standardsprache (onLoadLanguage) — schweres Modell-Laden daher lazy auslegen und bis dahin ERROR liefern (RHVoice macht genau das). Kein dokumentiertes Zeitlimit für onSynthesizeText; die Engine spielt selbst KEIN Audio ab (das Framework betreibt den AudioTrack) — gut für Energiebudget.
- minSdk: Voice-API vollständig ab API 21; PCM_FLOAT ab 24; rangeStart/Wort-Highlighting ab 26 → Empfehlung minSdk 26 (deckt auch aktuelle onnxruntime-android-Anforderungen ab, dort minSdk separat prüfen).
- 24000 Hz mono PCM16 ist als Engine-Ausgabeformat praxiserprobt (RHVoice startet exakt mit callback.start(24000, ENCODING_PCM_16BIT, 1)) — deckungsgleich mit Kokoros 24-kHz-Ausgabe, keine Resampling-Stufe nötig.

## Bericht

# Recherchefeld 1 — Schnittstellen-Protokoll: Android-TTS-Engine (`android.speech.tts.TextToSpeechService`)

Stand: 24.08.2026. Quellen: AOSP-Quelltext (GitHub-Spiegel `aosp-mirror/platform_frameworks_base`, Zweig `main`), AOSP-Beispiel-Engine, espeak-ng-Android, RHVoice-Android. Alle Signaturen wurden aus dem Quelltext extrahiert, nicht aus Sekundärquellen.

---

## 1. Der Service-Vertrag

`TextToSpeechService` (seit **API 14**) ist die abstrakte Basisklasse. **Fünf Methoden sind abstrakt und müssen implementiert werden:**

```java
protected abstract int      onIsLanguageAvailable(String lang, String country, String variant);
protected abstract String[] onGetLanguage();                       // [lang, country, variant], ISO-3
protected abstract int      onLoadLanguage(String lang, String country, String variant);
protected abstract void     onStop();
protected abstract void     onSynthesizeText(SynthesisRequest request, SynthesisCallback callback);
```

Rückgabecodes für `onIsLanguageAvailable`/`onLoadLanguage` (Konstanten aus `TextToSpeech`):

| Konstante | Wert | Bedeutung |
|---|---|---|
| `LANG_COUNTRY_VAR_AVAILABLE` | 2 | exakte Übereinstimmung inkl. Variante |
| `LANG_COUNTRY_AVAILABLE` | 1 | Sprache + Land |
| `LANG_AVAILABLE` | 0 | nur Sprache |
| `LANG_MISSING_DATA` | −1 | Daten fehlen (nachinstallierbar) |
| `LANG_NOT_SUPPORTED` | −2 | nicht unterstützt |

**Wichtig:** Die `lang`/`country`-Parameter kommen als **ISO-3-Codes** an (`"deu"`, `"DEU"`), denn `SynthesisRequest.getLanguage()` ist dokumentiert als "Gets the ISO 3-letter language code". espeak-ng behandelt defensiv beide Formen — das sollte der Port übernehmen.

### Threading (aus dem AOSP-Quelltext)

- Der Service erzeugt in `onCreate()` einen eigenen `HandlerThread("SynthThread")`. **Alle Aufrufe von `onSynthesizeText` erfolgen auf diesem einen Thread**, nie auf dem Main-Thread. Javadoc: die Synthese "**should block until the synthesis is finished**" — onSynthesizeText darf erst zurückkehren, wenn alles Audio geliefert (oder abgebrochen) ist.
- `onStop()` wird **von einem anderen Thread** ausgelöst (Stop-Anfragen laufen über den Binder und `stopImpl()`, während `onSynthesizeText` noch läuft). Konsequenz: Abbruch über ein `volatile`-Flag, das die Syntheseschleife prüft — siehe RHVoice-Muster in §7. Javadoc onStop: "Any pending data from the current synthesis will be discarded."
- Die **Abfragemethoden** (`onIsLanguageAvailable`, `onLoadLanguage`, `onGetVoices`, …) werden im Binder-Aufruf direkt beantwortet, laufen also auf **Binder-Pool-Threads** → gemeinsamer Zustand (z. B. „aktuell geladene Stimme") muss thread-sicher sein.
- Nach Abarbeitung der Warteschlange sendet der Synthese-Thread (als `MessageQueue.IdleHandler`) den Broadcast `ACTION_TTS_QUEUE_PROCESSING_COMPLETED`.

### Optionale Methoden

```java
protected Set<String> onGetFeaturesForLanguage(String lang, String country, String variant)
// Default: leeres HashSet — für uns z. B. Träger von Engine-Features (siehe §3)
```

---

## 2. SynthesisCallback — das Streaming-Protokoll

Quelle: `SynthesisCallback.java` + `PlaybackSynthesisCallback.java` (AOSP).

```java
int start(int sampleRateInHz, int audioFormat, int channelCount);
int audioAvailable(byte[] buffer, int offset, int length);
int done();
void error();                 // API 14
void error(int errorCode);    // API 21
boolean hasStarted();         // API 21
boolean hasFinished();        // API 21
int getMaxBufferSize();
void rangeStart(int markerInFrames, int start, int end);  // API 26
```

**Regeln (Javadoc wörtlich):**
- Streaming: "The engine can provide streaming audio by calling **start, then audioAvailable until all audio has been provided, then finally done**."
- `done()`: "**done must be called at the end of synthesis, regardless of errors**" — also auch nach `error()` und nach Stop. (Einzige Ausnahme laut Doku: wenn weder start noch error gerufen wurde, etwa weil die Anfrage sofort verworfen wird — dann genügt `error()` + `done()`.)
- Audioformate: `ENCODING_PCM_8BIT` oder `ENCODING_PCM_16BIT`; `ENCODING_PCM_FLOAT` "when targetting Android N and above".
- `getMaxBufferSize()`: "the maximum number of bytes that the TTS engine can pass in a single call of audioAvailable" — "Calls to audioAvailable with data lengths **larger than this value will not succeed**." Im AOSP-Wiedergabepfad ist das `MIN_AUDIO_BUFFER_SIZE = 8192` Bytes. **Nicht hartkodieren, immer abfragen** (bei Datei-Synthese kann der Wert anders sein).

**Verhalten des Framework-Wiedergabepfads** (`PlaybackSynthesisCallback` → `SynthesisPlaybackQueueItem`):
- `start()` legt ein Wiedergabe-Objekt mit `AudioTrack` an — **die Engine selbst berührt nie ein Audiogerät**; auch `synthesizeToFile()` einer Client-App läuft über denselben Callback (dann `FileSynthesisCallback`, schreibt WAV). Für die Portierung heißt das: `SpeechOutput.kt` aus CodeTest (eigene AudioTrack-Wiedergabe) wird im Engine-Pfad **nicht** gebraucht.
- `audioAvailable()` **blockiert, wenn zu viele unkonsumierte Puffer anstehen** ("Might block […] if there are too many buffers waiting to be consumed") — eingebaute Backpressure. Energie-Konsequenz: chunkweises Liefern (z. B. satz- oder blockweise aus Kokoro) taktet die Synthese automatisch mit der Wiedergabe; RAM-Spitzen und sinnlose Vorausberechnung nach einem Stop entfallen.
- Nach `stop()` liefert `audioAvailable()` den Fehlercode **`STOPPED` (−2)** zurück → die Engine erkennt den Abbruch auch am Rückgabewert und soll die Schleife verlassen (espeak-ng bricht dann die native Synthese ab).
- `rangeStart(markerInFrames, start, end)` (API 26) meldet „ab Audio-Frame X wird Textbereich [start,end) gesprochen" — Client-Apps erhalten daraus `UtteranceProgressListener.onRangeStart` (Wort-Highlighting, z. B. für Vorlese-Apps wichtig).

Fehlercodes für `error(int)` (aus `TextToSpeech`): `ERROR_SYNTHESIS` −3, `ERROR_SERVICE` −4, `ERROR_OUTPUT` −5, `ERROR_NETWORK` −6, `ERROR_NETWORK_TIMEOUT` −7, `ERROR_INVALID_REQUEST` −8, `ERROR_NOT_INSTALLED_YET` −9, `STOPPED` −2.

---

## 3. Manifest und Metadaten — so erscheint die Engine im System

Das System (Klasse `TtsEngines`) findet Engines per `PackageManager.queryIntentServices` auf die Intent-Action **`android.intent.action.TTS_SERVICE`** und liest die Metadaten **`android.speech.tts`** als XML (`<tts-engine>`-Tag, Attribut `settingsActivity`). Danach ist die Engine unter *Einstellungen → System/Bedienungshilfen → Text-in-Sprache-Ausgabe* wählbar; es gibt **keine Bind-Permission**. Rezept aus der AOSP-Beispiel-Engine (espeak-ng und RHVoice strukturgleich):

```xml
<service android:name=".KokoroTtsService"
         android:exported="true"                      <!-- ab Android 12 Pflichtangabe -->
         android:label="@string/engine_name">
    <intent-filter>
        <action android:name="android.intent.action.TTS_SERVICE"/>
        <category android:name="android.intent.category.DEFAULT"/>
    </intent-filter>
    <meta-data android:name="android.speech.tts"
               android:resource="@xml/tts_engine"/>
</service>

<!-- res/xml/tts_engine.xml -->
<tts-engine xmlns:android="http://schemas.android.com/apk/res/android"
            android:settingsActivity="…​.TtsSettingsActivity"/>

<!-- Von Systemeinstellungen und Alt-Clients aufgerufen: -->
<activity android:name=".CheckVoiceData" android:exported="true"
          android:theme="@android:style/Theme.NoDisplay">
    <intent-filter>
        <action android:name="android.speech.tts.engine.CHECK_TTS_DATA"/>
        <category android:name="android.intent.category.DEFAULT"/>
    </intent-filter>
</activity>
<activity android:name=".GetSampleText" android:exported="true"
          android:theme="@android:style/Theme.NoDisplay">
    <intent-filter>
        <action android:name="android.speech.tts.engine.GET_SAMPLE_TEXT"/>
        <category android:name="android.intent.category.DEFAULT"/>
    </intent-filter>
</activity>
```

- **CheckVoiceData** (`ACTION_CHECK_TTS_DATA`): unsichtbare Activity, antwortet mit `setResult(CHECK_VOICE_DATA_PASS = 1 / FAIL = 0)` und den Extras `EXTRA_AVAILABLE_VOICES` / `EXTRA_UNAVAILABLE_VOICES` (ArrayList<String> im Format `"deu-DEU"` bzw. `"lang-COUNTRY-variant"`). Legacy, aber von den Systemeinstellungen weiterhin abgefragt — espeak-ng und RHVoice führen sie beide.
- **GetSampleText** (`ACTION_GET_SAMPLE_TEXT`): liefert `EXTRA_SAMPLE_TEXT` für die „Beispiel anhören"-Funktion der Einstellungen — pro Sprache ein Demosatz.
- `ACTION_INSTALL_TTS_DATA` ist optional (espeak-ng nutzt es für den Stimmen-Download; für uns relevant, falls Sprachpakete nachladbar werden sollen).
- Package-Visibility (Android 11+): betrifft nur **Client-Apps** (`<queries>` mit der TTS-Intent-Action); die Engine selbst braucht nichts.

---

## 4. Wie fremde Apps Sprache und Stimme wählen

Entscheidender Mechanismus (aus `TextToSpeech.java`, seit API 21): **`setLanguage(Locale)` ist intern über die Voice-API implementiert**:

1. `setLanguage` → Service `isLanguageAvailable` (Sanitisierung) → **`onGetDefaultVoiceNameFor(lang, country, variant)`** → `loadVoice(name)` → `onLoadVoice(name)`.
2. `setVoice(Voice)` → `loadVoice(name)` → `onLoadVoice(name)` direkt.

Wer also die vier Voice-Methoden sauber implementiert, bedient **beide** Wege aus einer Logik:

```java
public List<Voice> onGetVoices()                    // alle Stimmen aufzählen
public int  onIsValidVoiceName(String voiceName)    // SUCCESS/ERROR
public int  onLoadVoice(String voiceName)           // SUCCESS/ERROR, darf vorladen
public String onGetDefaultVoiceNameFor(String lang, String country, String variant)
```

Die **Default-Implementierungen** sind eine reine Locale-Brücke (Voice-Name = BCP-47-Sprachtag, via `onIsLanguageAvailable`) und taugen nur für Engines mit einer Stimme pro Sprache — für unsere 54 Kokoro-Stimmen müssen alle vier überschrieben werden.

`Voice`-Attribute (Klasse seit API 21): `name` (Javadoc: "Unique voice name" — praktisch: stabil halten, Apps persistieren ihn), `locale`, `quality` und `latency` (je 100=VERY_LOW … 300=NORMAL … 500=VERY_HIGH), `requiresNetworkConnection`, `features` (Set<String>; engine-eigene Schlüssel müssen mit dem Paketnamen der Engine geprägt sein). Für uns: `quality = QUALITY_HIGH (400)`, `latency = LATENCY_NORMAL oder HIGH` (neuronal, on-device), `requiresNetwork = false`.

**Namensschemata in der Praxis:** RHVoice nutzt Sprechernamen (`"Elena"`) plus Fallback-Namen `"rus-default"`; espeak-ng nutzt Sprachtags. Empfehlung für den Port: sprechende, stabile ASCII-Namen wie `de-DE-martin`, `en-US-af_heart` — eindeutig, BCP-47-präfixiert, nie umbenennen. Das An-/Abwählen von Sprachen in den Einstellungen bildet sich sauber ab: abgewählte Sprachen tauchen in `onGetVoices`/`CheckVoiceData` nicht auf und liefern `LANG_NOT_SUPPORTED` (RHVoice filtert exakt so über `getEnabled() && isInstalled()`).

---

## 5. Parameter: Rate und Pitch aus der Standard-API

`SynthesisRequest` (AOSP-Quelltext, verbatim):

```java
public int getSpeechRate()   // "Gets the speech rate to use. The normal rate is 100."
public int getPitch()        // "Gets the pitch to use. The normal pitch is 100."
public CharSequence getCharSequenceText()  // API 21; getText() ist deprecated
public String getVoiceName()               // API 21 — leer, wenn Client nur setLanguage nutzte
public String getLanguage(), getCountry()  // ISO-3
public Bundle getParams()                  // durchgereichte Zusatzparameter des Clients
public int getCallerUid()
```

**Achtung: `int`, nicht float.** 100 = normal; App-`setSpeechRate(2.0f)` kommt als 200 an; fehlt eine App-Vorgabe, setzt das Framework die Systemeinstellung ein (`Settings.Secure.TTS_DEFAULT_RATE/PITCH`). Umsetzung in der Praxis: **RHVoice teilt schlicht durch 100** (`params.setRate(rate/100.0)`), espeak-ng multipliziert seine Grundrate (`rate * rateScale / 100`). Für den Kokoro-Port: Rate → Kokoro-`speed`-Eingang; Pitch → die vorhandene PSOLA-Stufe (100er-Skala → Faktor, Faktor → Halbtöne: `12·log2(pitch/100)`).

---

## 6. Lebenszyklus, Energie, Zeitverhalten

- **Gebundener Dienst:** Das System bindet beim ersten `new TextToSpeech(...)` eines Clients und beendet den Prozess irgendwann nach dem letzten Unbind; keine Foreground-Service-Pflicht, während gebunden gilt der Prozess als aktiv genutzt.
- **onCreate lädt bereits Sprache:** Der Framework-Code ruft beim Start `onLoadLanguage` für die Standard-Locale auf. **Kein dokumentiertes Zeitlimit** existiert für Laden oder Synthese — aber jede Sekunde Ladezeit ist für den Client Wartezeit auf `onInit`. Muster RHVoice: schwer initialisieren **lazy/asynchron**, bis dahin liefert `onSynthesizeText` `callback.error()` ("Not initialized yet") bzw. wird die Initialisierung nachgeholt. Für uns plus die CodeTest-Erkenntnisse: `.ort`-Format (halbe Ladezeit), `AssetFd → FileChannel.map → OrtSession aus ByteBuffer`, Session einmal aufbauen und über Requests hinweg halten; Stimm-Vektoren (522 KB/Stimme) erst bei `onLoadVoice` einlesen.
- **Stop-Reaktionszeit:** onStop muss *schnell* wirken. Da die Framework-Wiedergabe sofort verstummt, genügt es, dass die Engine ihre Schleife am nächsten Chunk-Ende verlässt (volatile-Flag + `STOPPED`-Rückgabewert von `audioAvailable`). Bei Kokoro heißt das: lange Texte NICHT als ein Riesen-Inferenzlauf, sondern in den ohnehin vorhandenen 510-Token-Stücken bzw. Sätzen synthetisieren und zwischen den Stücken das Flag prüfen.
- **Energie:** Die blockierende Backpressure von `audioAvailable` (§2) sorgt dafür, dass die CPU der Wiedergabe nur knapp vorausläuft. Kein eigener AudioTrack, kein Wakelock nötig.
- **Prozess-Tod:** Das System darf den Prozess jederzeit beenden; alles Persistente (aktivierte Sprachen, Stimmwahl) gehört in Preferences, nichts nur in den RAM.

---

## 7. Referenz-Implementierungen (gelesen)

**espeak-ng** (`android/src/com/reecedunn/espeak/TtsService.java`):
- `callback.start(mEngine.getSampleRate(), mEngine.getAudioFormat(), mEngine.getChannelCount())` (22050 Hz, PCM16, mono);
- streamt in einer Schleife `while (offset < audioData.length)` mit Chunks ≤ `mCallback.getMaxBufferSize()`; schlägt `audioAvailable` fehl (= STOPPED), wird `mEngine.stop()` gerufen;
- Rate-Mapping `rate = rate * rateScale / 100`;
- meldet Wortgrenzen über `rangeStart` (API 26+), rechnet dabei espeaks Codepoint-Positionen in UTF-16-Indizes um;
- Kniff: meldet eine Sprache auch dann als `LANG_AVAILABLE`, wenn sie herausgefiltert wurde, aber andere Stimmen da sind — damit Screenreader die Engine nicht komplett verwerfen.

**RHVoice** (`src/android/RHVoice-core/src/main/java/com/github/olga_yakovleva/rhvoice/android/RHVoiceService.java`):
- `callback.start(24000, AudioFormat.ENCODING_PCM_16BIT, 1)` — **exakt Kokoros Format**;
- `volatile boolean speaking`; `onStop()` setzt es auf false; der Audio-Konsument (`Player implements TTSClient`) prüft es in `playSpeech()` vor jedem Chunk und bricht ab;
- `params.setRate(rate/100.0)`, `params.setPitch(pitch/100.0)`;
- Stimmen: konkrete Namen (`"Elena"`) + `"<iso3>-default"`-Fallbackstimmen; `onGetVoices` filtert auf installierte UND in den Einstellungen aktivierte Pakete — Vorbild für unsere an-/abwählbaren Sprachen;
- nicht initialisiert → `onSynthesizeText` meldet Fehler und stößt die Initialisierung an; Lifecycle über AndroidX `ServiceLifecycleDispatcher`.

---

## 8. minSdk-Empfehlung

| Fähigkeit | ab API |
|---|---|
| `TextToSpeechService`, Callback-Grundprotokoll, `getMaxBufferSize` | 14 |
| Voice-API komplett (Voice-Klasse, onGetVoices/onLoadVoice/…, `getVoiceName`, `getCharSequenceText`, `error(int)`, `hasStarted/hasFinished`; setLanguage→Voice-Umleitung) | 21 |
| `ENCODING_PCM_FLOAT` im Callback erlaubt | 24 (Target N) |
| `rangeStart` / `onRangeStart` (Wort-Highlighting) | 26 |

**Empfehlung: minSdk 26.** Darunter verliert man nichts Relevantes an Geräten (Android 8.0, 2017), gewinnt aber `rangeStart` ohne Verzweigungen; PCM16 bleibt ohnehin das Ausgabeformat (PCM_FLOAT bringt bei Kokoro-Ausgabe keinen Mehrwert, kostet doppelte Puffer). Die minSdk-Anforderung von onnxruntime-android ist im ORT-Recherchefeld gegenzuprüfen (aktuelle Pakete verlangen ≥ 24).

---

## 9. Direkte Konsequenzen für den Kokoro-Port

1. **Ein Format, null Resampling:** `callback.start(24000, ENCODING_PCM_16BIT, 1)` — Kokoro liefert 24 kHz mono; nur float→int16-Wandlung.
2. **Chunk-Streaming = vorhandene Stapellogik:** Die Windows-Erkenntnis „continuous ab 400 Phonemen" bleibt Engine-intern; nach jedem synthetisierten Stück wird das Audio in ≤8192-Byte-Häppchen an `audioAvailable` gereicht und das Stop-Flag geprüft.
3. **Voice-Namen als stabile API:** `de-DE-martin`, `en-US-af_heart` … — sie sind der Vertrag mit fremden Apps (setVoice) und mit den Systemeinstellungen; nie umbenennen.
4. **Sprachwahl in den Einstellungen** = Filter in `onGetVoices` + `onIsLanguageAvailable` + `CheckVoiceData` (RHVoice-Muster `getEnabled()`).
5. **Lazy-Init nach CodeTest-Muster** (mmap, .ort, gemessene Thread-Zahl); bis dahin `error()` + `done()`.
6. **onStop-Pfad von Anfang an mitbauen und testen** — er ist der einzige nebenläufige Teil des Vertrags.

---

## Quellen

**Offizielle Referenz (developer.android.com):**
- https://developer.android.com/reference/android/speech/tts/TextToSpeechService
- https://developer.android.com/reference/android/speech/tts/SynthesisCallback
- https://developer.android.com/reference/android/speech/tts/SynthesisRequest
- https://developer.android.com/reference/android/speech/tts/Voice
- https://developer.android.com/reference/android/speech/tts/TextToSpeech.Engine
- https://developer.android.com/reference/android/speech/tts/UtteranceProgressListener

**AOSP-Quelltext (maßgeblich für Signaturen, Threading, Puffergrößen):**
- https://github.com/aosp-mirror/platform_frameworks_base/blob/main/core/java/android/speech/tts/TextToSpeechService.java
- https://github.com/aosp-mirror/platform_frameworks_base/blob/main/core/java/android/speech/tts/SynthesisCallback.java
- https://github.com/aosp-mirror/platform_frameworks_base/blob/main/core/java/android/speech/tts/SynthesisRequest.java
- https://github.com/aosp-mirror/platform_frameworks_base/blob/main/core/java/android/speech/tts/Voice.java
- https://github.com/aosp-mirror/platform_frameworks_base/blob/main/core/java/android/speech/tts/TextToSpeech.java
- https://github.com/aosp-mirror/platform_frameworks_base/blob/main/core/java/android/speech/tts/TtsEngines.java
- https://github.com/aosp-mirror/platform_frameworks_base/blob/main/core/java/android/speech/tts/PlaybackSynthesisCallback.java

**AOSP-Beispiel-Engine (Manifest/tts_engine.xml-Vorlage):**
- https://github.com/aosp-mirror/platform_development/tree/main/samples/TtsEngine (AndroidManifest.xml, res/xml/tts_engine.xml)

**Referenz-Engines:**
- espeak-ng: https://github.com/espeak-ng/espeak-ng/blob/master/android/src/com/reecedunn/espeak/TtsService.java und https://github.com/espeak-ng/espeak-ng/blob/master/android/AndroidManifest.xml
- RHVoice: https://github.com/RHVoice/RHVoice/blob/master/src/android/RHVoice-core/src/main/java/com/github/olga_yakovleva/rhvoice/android/RHVoiceService.java

**API-Level-Belege:**
- API 14 (Klasse + Pflichtmethoden): https://stuff.mit.edu/afs/sipb/project/android/docs/reference/android/speech/tts/TextToSpeechService.html
- API 21 (setLanguage→Voice, onGetVoices): http://android.cn-mirrors.com/reference/android/speech/tts/TextToSpeechService.html (Doku-Spiegel; „Since API level 21 TextToSpeech#setLanguage is implemented by calling TextToSpeech#setVoice with the voice returned by onGetDefaultVoiceNameFor")
- API 26 (rangeStart/onRangeStart): https://learn.microsoft.com/en-us/dotnet/api/android.speech.tts.isynthesiscallback.rangestart und https://developer.android.com/reference/android/speech/tts/UtteranceProgressListener