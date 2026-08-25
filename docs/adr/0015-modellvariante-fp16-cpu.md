# ADR-0015: Modellvariante fp16 + CPU-EP (Nachtrag zu ADR-0004)

**Status:** angenommen (25.08.2026), Hör-Gate für Deutsch noch offen
**Kontext:** ADR-0004 verlangte die Entscheidung per Messung — sie liegt vor.

## Entscheidung

Standardvariante wird **fp16 über den CPU-Execution-Provider**; XNNPACK und
int8 werden nicht angeboten. Threadzahl (2 gegen 4) bleibt offen bis zur
mAh-Messung in M5.

## Begründung (Messwerte 25.08.2026, SM-F971B, ORT 1.28.0)

| Variante (CPU-EP, 4T) | RTF warm | PSS geladen/Synthese | Platte |
|---|---|---|---|
| **fp16** | **0,408** | **294/434 MB** | 156 MiB |
| fp32 | 0,410 (martin) / 0,780 (v1.0-Lauf, thermisch belastet) | 403/532 MB | 311 MiB |
| int8 | 2,233 | 216/382 MB | 109 MiB |

fp16 liefert fp32-Tempo bei halber Dateigröße und ~110 MB weniger PSS — der
SM8850 rechnet fp16 nativ. XNNPACK war in JEDER Zelle langsamer (RTF 1,1–2,4)
und +250 MB schwerer (Prepacking) — die sherpa-Erfahrung („XNNPACK ist
Pflicht") gilt für den direkten ORT-Weg nicht. int8 bestätigt den
Nova-Befund in verschärfter Form.

## Offen

- **Hör-Gate fp16 für Deutsch:** kokoro-martin existiert nur als fp32; die
  fp16-Konvertierung (Werkzeug: onnxconverter-common) und die deutsche
  Hörprobe stehen aus. Bis dahin läuft Deutsch als fp32.
- Threads 2/4 per mAh je 1000 Zeichen (M5). 2T: RTF 0,58–0,60, tragfähig.

## Verworfene Alternativen

XNNPACK (gemessen schlechter), int8 (Echtzeit verfehlt), NNAPI/QNN (ADR der
Vorprojekte: Kompatibilitäts- und Wartungspreis ohne belegten Gewinn).
