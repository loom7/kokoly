# ADR-0004: Modellvariante per Messung, nicht per Annahme

**Status:** angenommen (24.08.2026) — offene Nachträge siehe unten
**Kontext-Dokument:** [PROJEKTPLAN.md](../../PROJEKTPLAN.md), Abschnitt 3

## Entscheidung

Status: offen bis M2a. Pflichtmatrix {fp32, fp16, ggf. int8 (F7)} × {CPU-EP, XNNPACK} × Threads {1,2,4} auf dem SM-F971B: RTF kalt/eingeschwungen, PSS mit Aufriss private dirty vs. file-backed (dumpsys meminfo), Hauttemperatur, mAh je 1000 Zeichen, Spinning-Kontrollpunkt (CPU = 0 % zehn Sekunden nach Run-Ende, je EP). Tempo-/RAM-Zeilen laufen einmal auf v1.0; das Hör-Gate (ich-Laut-Prüfsatz) läuft je Variante und je Modell — martin und v1.0 sind grafgleiche Geschwister, Leistungswerte übertragen sich, Klang nicht zwingend. Falls Martin nicht als fp16/int8 vorliegt: eigene Konvertierung als Messkandidat oder fp32-only für de dokumentieren. Standardwahl im Einrichtungs-UI nach Gerätekasse (`ActivityManager.MemoryInfo`/`isLowRamDevice`)

## Begründung

Gemessene Widersprüche verbieten Raten: int8 war am Gerät langsamer als fp32 (RTF 0,84–0,93 vs. 0,70–0,74); fp16 halbiert die Dateigröße, die RAM-Halbierung ist unbelegt (Cast-Paar-Risiko, Supertonic-Befund: +54 % Zeitkosten); welcher EP gewinnt, ist je Graph unvorhersehbar

## Verworfene Alternativen

Blind fp16 „weil kleiner"; int8 „weil schnell" (beides durch Messungen widerlegt bzw. ungedeckt); Ein-Default-für-alle-Geräte
