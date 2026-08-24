# ADR-0006: Regelwerke als datennahe Kotlin-Tabellen, strukturgleich zur Python-Referenz

**Status:** angenommen (24.08.2026)
**Kontext-Dokument:** [PROJEKTPLAN.md](../../PROJEKTPLAN.md), Abschnitt 3

## Entscheidung

textregeln/betonung/wortlaute werden als je eine Kotlin-Datei mit Tabellenteil (Datenklassen-Listen) + kleinem Anwendungsmotor portiert; Struktur und Reihenfolge spiegeln die Python-Dateien. Jede neue Regel = Tabelleneintrag + Golden-Testfall (Lebenszyklus: ADR-0013). Meldweg: Issue-Vorlage (Wort, Ist-Klang, Soll-Klang). Ehrlich dokumentiert: ein Aussprachefix erreicht Nutzer erst mit dem nächsten APK-Release; für kontextfreie Wortregeln wird in Stufe 2 ein Laufzeit-Nutzerlexikon (Datei in filesDir) erwogen

## Begründung

betonung.py enthält Kontextprüfungen und die Zählprobe — Logik, kein reines JSON; eine externe Datenschicht schüfe zwei Wahrheiten. Typsicherheit + JVM-Tests ohne Emulator

## Verworfene Alternativen

Externe JSON/CSV zur Laufzeit (Kontextprüfungen nicht abbildbar, Drift); In-App-Regeleditor (Stufe-3-Idee)
