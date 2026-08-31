# Recherchefeld: F-Droid-Aufnahmefähigkeit (M6, Nutzer-Entscheid F6)

Recherchiert am 31.08.2026 (Web-Abruf mit adversarialer Gegenprüfung der
Kernaussagen; Policy-Revisionen im Text). **Ergebnis: aufnahmefähig** — alle
drei kritischen Fragen sind durch Policy-Wortlaut UND gelebte Präzedenzfälle
gedeckt. Kein Blocker; verbleibende Pflichten unten.

## 1. Vorgebautes onnxruntime-android — erlaubt

Die Inclusion Policy verlangt Quellbauten, öffnet aber ausdrücklich für
vorgebaute FLOSS-Binaries aus vertrauenswürdigen Maven-Repositories; Maven
Central steht namentlich in der Liste („Dependency Handling: Library
dependencies must be built from source or provided in a trusted Maven
repo."). Bedingung ist nur die freie Lizenz des Artefakts — das POM von
`com.microsoft.onnxruntime:onnxruntime-android` deklariert MIT.
Beleg: https://f-droid.org/en/docs/Inclusion_Policy/ (fdroid-website
5f9e2980851de0556e115ff220fa03ccf6246ac2);
https://repo1.maven.org/maven2/com/microsoft/onnxruntime/onnxruntime-android/1.28.0/onnxruntime-android-1.28.0.pom

Gelebte Praxis ist sogar großzügiger: SherpaTTS (im Hauptrepo) bezieht sein
sherpa-onnx-AAR über JitPack, dessen Rezept fertige AARs per wget von
GitHub-Releases holt. Der direkte Maven-Central-Weg von Kokoly ist der
sauberere, explizit gedeckte Fall.

## 2. Laufzeit-Nachladen der ~300-MiB-Modelle — zulässig, kostet ein Etikett

Die Policy verbietet nur das Nachladen **ausführbarer** Binärdateien ohne
Opt-in; Modellgewichte werden in der Praxis als Daten behandelt. Apps mit
Hugging-Face-/GitHub-Modelldownload tragen das Anti-Feature **NonFreeNet**
(Warnetikett, kein Ausschlussgrund). Wichtig: Nicht deklarierte
Anti-Features führen zur Ablehnung — das Etikett gehört von uns selbst in
den fdroiddata-Merge-Request, nach dem etablierten Muster
`AntiFeatures: NonFreeNet: en-US: "Downloads the TTS models from …"`.

Präzedenzfälle (alle live im Hauptrepo, fdroiddata-Revisionen dokumentiert):
- **SherpaTTS** (org.woheller69.ttsengine, GPL-3.0-only): System-TTS-Engine,
  lädt Stimmen von Hugging Face — der 1:1-Fall zu Kokoly
  (fdroiddata 940ffc429406af459d253e5eeddad0dcba393aec).
- **WhisperIME** (org.woheller69.whisper, MIT): Whisper-Modell von Hugging
  Face, nur NonFreeNet (fdroiddata 197f661fc9e7ef4e7e67011b44ed4b575aed482b).
- **RHVoice** (GPL-3.0-or-later): seit Jahren das Muster »APK ohne Gewichte +
  gebündeltes Paketmanifest (packages.json mit dataUrl + MD5) +
  Laufzeit-Download mit Wiederaufnahme« — strukturell exakt unser
  models.json-Weg, nur mit SHA-256 statt MD5
  (fdroiddata aa9fca1fa0193753cfd3f2c36909659ffbc100a5).

Kokolys Downloader erfüllt das Opt-in ohnehin: Download startet nur auf
Knopfdruck in den Einstellungen, standardmäßig nur über ungetaktetes Netz.

## 3. Verbleibende Pflichten für den fdroiddata-MR (wenn F6 so entschieden wird)

1. `AntiFeatures: NonFreeNet` selbst deklarieren (Begründungstext: Modell-
   Download von GitHub-Releases/Hugging Face).
2. Reproduzierbarer Quellbau: gegeben (CI baut das APK samt espeak-1.52.0-
   FetchContent; keine eigenen Binärartefakte im Repo).
3. Prüfen, ob die espeak-ng-Datendateien im APK den F-Droid-Scanner
   auslösen — SherpaTTS setzt für seine Assets `scanignore`; denselben
   Handgriff ggf. einplanen.
4. Keine formale Vorabzusage möglich: Aufnahme bleibt Einzelfall-Review
   („this is not a precise science").

## Offene Ermessensfrage

Ob ONNX-Graphen im Grenzfall je als »ausführbar« gewertet würden, ist
nirgends schriftlich geklärt; die gelebte Praxis (SherpaTTS, WhisperIME)
wertet sie als Daten. Risiko: gering, dokumentiert.
