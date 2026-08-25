# Das Regelwerk — Aussprachefehler melden und einpflegen

Kokolys deutsche Aussprache wird von drei Regelschichten getragen, alle 1:1
aus der vermessenen Windows-Referenz portiert (ADR-0013: die Referenz ist seit
M3 eingefroren, **dieses Repo ist die Wahrheit**):

| Schicht | Ort | Wirkt auf | Beispiele |
|---|---|---|---|
| **Textregeln** | `rules/de/Textregeln.kt` | Text, VOR espeak | „z.B." → „zum Beispiel", „am 3. Mai" → „am dritten Mai" (Kasus!), Uhrzeit, Einheiten |
| **Betonung** | `rules/de/Phonemregeln.kt` (BETONUNG) | Phoneme, NACH espeak | `ˈaʊɡʊst` → `aʊɡˈʊst` (Monat Augúst, nur mit Signalwort), Büro, Hotel, Osnabrück … |
| **Wortlaute** | `rules/de/Phonemregeln.kt` (WORTLAUTE) | Phoneme, NACH espeak | Erbse, Journal, Frauchen, Martin (langes i), Tuch-Familie |

## Die zwei Grundsätze

**1. Kontext entscheidet am Text.** Auf der Phonemebene sind Monat und Vorname
beide `ˈaʊɡʊst` — ob eine Regel greifen darf, prüft ihr Kontextmuster am Text
vor der Fundstelle („im/seit/dritten/3. …"). „August Meier" bleibt unberührt.

**2. Die Zählprobe schützt.** Findet sich das Phonemmuster nicht genau so oft
wie das Wort im Text, wird die Regel übersprungen und gemeldet („Die Regie der
Regierung": `reːʒˈiː` steckt auch in `reːɡˈiːrʊŋ`). Ein Eingriff an der
falschen Stelle wäre schlimmer als der bekannte Fehler.

## Einen Aussprachefehler melden

Ein Issue mit drei Angaben genügt:

1. **Wort oder Satz**, so wie er eingegeben wurde.
2. **Was er jetzt spricht** (beschrieben oder als Aufnahme).
3. **Was er sprechen sollte.**

## Eine Regel einpflegen (der volle Weg)

1. **Reproduzieren.** Den Satz durch die Pipeline schicken; das Phonemfeld
   zeigt, was das Modell bekommt. Damit ist klar, ob espeak (falsche Phoneme)
   oder das Modell (richtige Phoneme, falscher Klang) die Ursache ist.
2. **Ebene wählen.**
   - Falscher Text (Abkürzung, Zahl, Datum): **Textregeln**.
   - Falsche Betonung: **BETONUNG** — das Zeichen `ˈ` gehört unmittelbar VOR
     den Vokal der betonten Silbe (`aʊɡˈʊst`, nicht `aʊˈɡʊst` — gemessen,
     alles andere ist wirkungslos). Satzfinal wirkt es nicht.
   - Falsche Laute: **WORTLAUTE**. Achtung Reihenfolge: Flexionsformen vor der
     Grundform (siehe Martins/Martin — sonst steigt die Zählprobe aus).
3. **Bei Betonungsregeln: messen, nicht raten.** Die K-Kennzahl-Skripte liegen
   unter `scripts/messung/` (Python, gegen die Windows-Referenzumgebung);
   Regeln ohne messbare Wirkung (ΔK ≤ 0,1) werden nicht aufgenommen — vier
   solcher Kandidaten sind in der Referenz dokumentiert abgelehnt.
4. **Regelzeile eintragen** — ein Tabelleneintrag, keine Logikänderung.
5. **Golden-Satz ergänzen:** `scripts/golden/korpus.py` → `REGEL_KORPUS`
   (Treffer UND, wenn sinnvoll, Gegenprobe), dann
   `"../TTS Test/.venv/Scripts/python.exe" scripts/golden/generieren.py`.
   Der Generator schlägt fehl, wenn die Frontend-Spezifikation bricht.
6. **Tests laufen lassen:** `./gradlew :app:testDebugUnitTest` — die neue Regel
   ist erst fertig, wenn ihr Golden-Satz grün ist.
7. **CHANGELOG-Eintrag.** Ein Aussprachefix erreicht Nutzer mit dem nächsten
   Release.

## Grenzen (ehrlich)

- Die Regelwerke sind **deutsch**; andere Sprachen laufen roh durch espeak.
- Vier Fehlbetonungen sind bekannt und **nicht behebbar** über das Zeichen
  (Silvester, Zigarette, Oberhausen, Wuppertal — ΔK ≤ 0,04).
- espeaks Kompositum-Zweitbetonung ist eine falsche *Regel*, keine Lücke: eine
  Wortliste skaliert dafür nicht (offener Punkt der Referenz).
