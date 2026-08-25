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

- M4 Mehrsprachigkeit: 42 Stimmen über 8 Sprachen in der Voice-API, Stimmwahl
  per setVoice, Sprachen in den Einstellungen an-/abwählbar; Beispieltexte und
  CheckVoiceData je Sprache; Session-Tausch beim Gruppenwechsel (ADR-0012).
- M2b Modellbezug: In-App-Downloader (nur ungetaktet, SHA-256 vor Umbenennen,
  Fortsetzen), noBackup-Lager mit Entwicklungsweg-Vorrang, einmalige
  on-device-.ort-Wandlung, npz-Stimmbank-Leser (keine Hilfsdateien mehr).
- ADR-0016: GPU und NPU als geprüfte Nicht-Ziele (Nutzer-Entscheid).
- M2a-Messmatrix (21 Konfigurationen am Gerät): fp16+CPU-EP als Standard
  entschieden (ADR-0015); XNNPACK und int8 verworfen — beide gemessen schlechter.
- M3 Regelwerk: deutsche Text-, Betonungs- und Wortlautregeln als Kotlin-Tabellen
  (Golden-Stufe C, 42 Sätze samt Gegenproben), in die Dienst-Pipeline verdrahtet;
  docs/regelwerk.md mit dem vollen Einpflege-Weg.
- M1 Service-Skelett: KokolyTtsService echt (Streaming über SynthesisCallback,
  ISO-3-Sprachaushandlung, Voice de-DE-martin, Stop-Flag je Block), CheckVoiceData,
  satzweise Pipeline; Framework-Klient-Abnahmetest grün.

### Geändert
- ONNX Runtime 1.23.2 → 1.28.0 (SIGILL am SM8850, ADR-0014).
