# ADR-0017: Deutsch läuft fp32 mit 4 Threads (schließt die offenen Punkte aus ADR-0015)

**Status:** angenommen (25.08.2026, Nutzer-Entscheid nach M5-Datenlage)
**Kontext:** ADR-0015 ließ zwei Punkte offen: die deutsche Modellvariante
(Kandidat war die Mischfassung fp16dec, nachdem die fp16-Vollwandlung das
Hör-Gate riss) und die Threadzahl (2 gegen 4, zu entscheiden per mAh-Messung).

## Entscheidung

Die deutsche Sprachgruppe (kokoro-martin) läuft **fp32** mit **4 Threads**
als Vorgabe. Die Mischfassung fp16dec wird nicht angeboten; die gescheiterte
fp16-Vollwandlung verschwindet aus dem Manifest.

## Begründung (M5-Energiemessung, SM-F971B, 25.08.2026, Belege in erkenntnisse.md)

| Konfiguration | RTF | netto mAh je 1000 Zeichen |
|---|---|---|
| **fp32 · 4T** | **0,469** | **7,28** |
| fp32 · 2T | 0,674 | 6,35 |
| fp16dec · 4T | 0,544 | 9,55 |
| fp16dec · 2T | 0,766 | 9,59 |

fp32 schlägt fp16dec auf beiden Threadzahlen in Tempo UND Energie (~30 %
sparsamer) — der RTF-Vorsprung von fp16dec aus M2a (Bildschirm an) kehrt
sich im realen Vorlese-Profil (Bildschirm aus) um. 4 Threads bleiben
Vorgabe: bester RTF-Spielraum und geringste Latenz; der Energieaufpreis
gegenüber 2T (+0,9 mAh je 1000 Zeichen, ~14 %) ist klein. 2 Threads sind
als möglicher Energiesparmodus für Stufe 2 dokumentiert.

Nebengewinn: Deutsch braucht keine eigene Wandlungskette mehr — das
Manifest zeigt auf die Originaldatei des Fine-Tunes, die Lizenz- und
Prüfsummenkette bleibt kurz (F1-Dossier).

## Geltungsbereich und Wiedervorlage

Die v1.0-Gruppe (8 Sprachen) bleibt bei fp16 (ADR-0015: RTF gleich, halbe
Größe, ~110 MB weniger PSS, Hör-Gate bestanden). Deren ENERGIE wurde in M5
nicht gemessen — fällt v1.0-fp16 später energetisch auf, ist die Messung
mit dem M5-Werkzeug (EnergieMessTest) nachzuholen.

## Verworfene Alternativen

- **fp16dec für Deutsch:** klanglich gut (Hör-Gate bestanden), aber am
  Zielprofil langsamer und ~30 % energiehungriger als fp32.
- **2 Threads als Vorgabe:** sparsamer, aber 44 % langsamer; als
  Stufe-2-Energiesparmodus vorgemerkt statt als Standard.
