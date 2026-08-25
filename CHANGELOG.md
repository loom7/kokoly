# Changelog

Format nach [Keep a Changelog 1.1.0](https://keepachangelog.com/de/1.1.0/),
Versionierung nach [SemVer](https://semver.org/lang/de/) (öffentliche API laut ADR-0011:
Stimmnamen, unterstützte Locales, Verhalten der TTS-Schnittstelle).

## [Unreleased]

### Hinzugefügt
- Projektplanung (PROJEKTPLAN.md), 13 ADRs, Rechercheablage, Repo-Gerüst.
- M0-Prüfstein vollständig: espeak-ng 1.52.0 arm64 (Geräteparität zeichengenau),
  Kotlin-Frontend (8 Sprachen golden-geprüft), nackte Kokoro-Inferenz über ORT,
  Gate-WAVs für die ich-Laut-Hörprobe.

- M1 Service-Skelett: KokolyTtsService echt (Streaming über SynthesisCallback,
  ISO-3-Sprachaushandlung, Voice de-DE-martin, Stop-Flag je Block), CheckVoiceData,
  satzweise Pipeline; Framework-Klient-Abnahmetest grün.

### Geändert
- ONNX Runtime 1.23.2 → 1.28.0 (SIGILL am SM8850, ADR-0014).
