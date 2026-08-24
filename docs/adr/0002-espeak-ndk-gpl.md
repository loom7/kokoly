# ADR-0002: espeak-ng per NDK-Build, auf den Referenz-Commit gepinnt → Projektlizenz GPL-3.0-or-later

**Status:** angenommen (24.08.2026)
**Kontext-Dokument:** [PROJEKTPLAN.md](../../PROJEKTPLAN.md), Abschnitt 3

## Entscheidung

Eigener CMake/NDK-Build (Schalterbelegung nach sherpa-Vorlage: USE_ASYNC/MBROLA/KLATT … OFF), gepinnt auf den espeak-ng-Stand hinter espeakng-loader 0.2.4 (Aufgabe 0.2; Rückfallebene: Windows-Goldens mit dem gewählten Commit neu erzeugen). Dünnes JNI (~150 Zeilen) um `espeak_TextToPhonemes` (phonememode: IPA-Bit 0x02, Separator-Bits), globaler Mutex, resident je Prozess (Querschnittsfestlegung in 2.2). Damit wird die App GPL-3.0 (kombiniertes Werk, keine Linking-Exception); LICENSE und Notices liegen ab Commit 1 im Repo

## Begründung

Kokoro ist auf espeak-Phoneme trainiert; jeder G2P-Wechsel ist exakt die Fehlerklasse von damals. Wörterbücher ändern sich zwischen espeak-Ständen — ohne Commit-Gleichheit ist „zeichengenau" nicht prüfbar. GPL-TTS-Engines sind gelebte Praxis (espeak-ng-App, RHVoice)

## Verworfene Alternativen

gruut (MIT, aber archiviert, ohne hi/pt); goruut (MIT, andere Phonemkonventionen = unvermessenes Qualitätsrisiko); epitran (keine Betonung)
