# ADR-0003: ja/zh gestuft, nicht in Stufe 1

**Status:** angenommen (24.08.2026)
**Kontext-Dokument:** [PROJEKTPLAN.md](../../PROJEKTPLAN.md), Abschnitt 3

## Entscheidung

Stufe 1 liefert die 8 espeak-Sprachen. Stufe 2 beginnt mit zh (Kotlin-Port des misaki-Legacy-Pfads, ~4–6 T), danach ja (Kuromoji-ipadic + HEPBURN-Tabellenport, ~5–8 T, ~13 MB)

## Begründung

misaki ist reines Python; ja über espeak ist unbrauchbar („Chinese letter"), zh verlöre 19 %. Der zh-Legacy-Pfad hat keine Ton-Sandhi → Parität billig erreichbar; ja hat das unidic-Gewichtsproblem

## Verworfene Alternativen

MeCab+unidic-lite als 47-MiB-Download (Rückfallebene, dokumentiert); sherpas zh-Lexikon + Greedy-Matcher
