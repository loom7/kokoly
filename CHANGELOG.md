# Changelog

Format nach [Keep a Changelog 1.1.0](https://keepachangelog.com/de/1.1.0/),
Versionierung nach [SemVer](https://semver.org/lang/de/) (öffentliche API laut ADR-0011:
Stimmnamen, unterstützte Locales, Verhalten der TTS-Schnittstelle).

## [Unreleased]

### Hinzugefügt
- M6-Veröffentlichungsreife: Martin-Lizenzdossier mit revisionsgepinnten
  Belegen (docs/lizenz/martin-kette.md + belege/), THIRD_PARTY_NOTICES mit
  wörtlichen Lizenztexten der gebündelten Stände, F-Droid-Recherche
  (docs/recherche/f-droid.md: aufnahmefähig, NonFreeNet-Etikett),
  models.json auf die belegte HF-Revision gepinnt.
- Leerlauf-Timer: 5 Minuten nach der letzten Synthese wird die ORT-Session
  entladen (espeak bleibt resident); der nächste Auftrag lädt in ~1 s neu.
- M5-Energiemessung: mAh je 1000 Zeichen für fp32/fp16dec × 2/4 Threads,
  Leerlauf-Basislinie und 30-Minuten-Vorlese-Dauerlauf (EnergieMessTest,
  Echtzeit-Taktung; Auswertung scripts/messung/energie_auswertung.py).
  Befund: fp32 schlägt fp16dec in Tempo UND Energie; Temperatur im
  Dauerlauf stabil.
- Tonhöhensteuerung: `getPitch()` wirkt über tempokompensiertes Umtasten
  (F2 revidiert, Nutzer-Entscheid 25.08.2026); ±-Verstellung am Gerät per
  F0-Messung belegt. Formanterhaltendes TD-PSOLA bleibt Stufe-2-Verbesserung.
- Hörproben-Werkzeug für neue Regeln (HoerprobeTest: Satz über den echten
  Dienstweg in eine WAV, Text per Instrumentation-Argument; pinnt Tempo und
  Tonhöhe auf 1,0, damit Systemregler das Urteil nicht verfälschen).
- Segmentdauer-Prüfung für Wortlautregeln (scripts/messung/dauer_pruefung.py);
  das Phonemfeld je Satz steht jetzt im Dienst-Log, und Vokabularverluste
  NACH der Regelstufe werden gemeldet statt still verworfen.
- Golden-Writer-Weg für Regeln nach dem Referenz-Einfrieren (ADR-0013):
  `NEUE_REGEL_ERWARTUNGEN` mit begründeter Herleitung, `endfassung_quelle`
  kennzeichnet jede Erwartung im Golden.
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
- Deutsche Modellvariante endgültig **fp32 mit 4 Threads** (ADR-0017, nach
  M5-Energiedaten); die Martin-Wandlungsvarianten sind aus dem Manifest
  entfernt. Die 8 v1.0-Sprachen bleiben fp16 (ADR-0015).
- ONNX Runtime 1.23.2 → 1.28.0 (SIGILL am SM8850, ADR-0014).

### Behoben
- „-synthese" wird jetzt auch in zusammengesetzten Wörtern berichtigt
  (Sprachsynthese, Biosynthese, Fotosynthese, Mehrzahlformen …): neue
  Regelart Wortteile mit Variantentafel je Betonungslage.
- Absturzfenster geschlossen: OrtSession.close() konnte in einen laufenden
  Modell-Run fallen (Engine-Wechsel oder künftiger Leerlauf-Timer während
  langer Synthese) — Lauf/Schließen jetzt über faires RW-Lock synchronisiert.
- Stop wirkt jetzt IM Modell-Run (RunOptions.setTerminate, Aufgabe 1.2 des
  Plans) statt erst am nächsten Audio-Block.
- Einstellungen: Edge-to-Edge-Insets (Inhalt lag unter Status- und
  Navigationsleiste); Phonempuffer im JNI-Wrapper meldet Überlauf statt still
  zu kürzen (64 KiB statt 16 KiB).
- Engine wurde stumm, nachdem in den Systemeinstellungen die TTS-Engine
  gewechselt und zurückgewechselt wurde: espeak_Terminate in onDestroy tötete
  die Bindung im weiterlebenden Prozess. espeak bleibt jetzt bis zum
  Prozessende resident; der Pipeline-Start heilt sich selbst
  (Regressionstest DienstNeustartTest).
- Haken der Sprach-Auswahl im Dunkelmodus unsichtbar — Haken-Farben hängen
  jetzt explizit an den Textfarben des Themas.
- Tonhöhe×Tempo am Modellfenster: die Klemmung ging zulasten der Dauer,
  jetzt hat Tempotreue Vorrang (Tonhöhe weicht am Rand sanft zurück).
- „Synthese"/„Photosynthese" falsch betont und mit Schwa gesprochen
  („ZÜN-te-se") — Wortlautregeln, ΔK +0,20 (Nutzerfund 25.08.2026).
- Einstellungen folgen jetzt der System-Tag/Nacht-Einstellung (Theme.Kokoly
  mit values-night); vorher im Dark Mode helle Blendfläche.
- Tonhöhenregler der System-TTS-Einstellungen war wirkungslos (getPitch()
  wurde ignoriert) — siehe Tonhöhensteuerung unter „Hinzugefügt".
