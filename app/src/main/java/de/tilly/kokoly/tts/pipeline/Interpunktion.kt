// SPDX-License-Identifier: GPL-3.0-or-later
package de.tilly.kokoly.tts.pipeline

/**
 * Exakter Port der Interpunktionsbehandlung der Windows-Referenz
 * (phonemizer.punctuation.Punctuation, preserve/restore).
 *
 * WARUM ein Port und keine Eigenlogik: Die Referenz entfernt die espeak-
 * Sprachwechselmarken mit dem Muster `\(.+?\)` NACH dieser Abtrennung —
 * echte Klammern überleben nur, weil sie hier vorher herausgelöst wurden.
 * Jede Abweichung in der Abtrennung verschöbe dieses Gleichgewicht. Die
 * Golden-Tests halten jeden Satz zeichengenau gegen die Referenz.
 *
 * Festwerte der Referenz: Marks `;:,.!?¡¿—…"«»«»(){}[]`; Komma und Punkt
 * gelten NICHT als Interpunktion, wenn sie zwischen Ziffern stehen (schützt
 * "0,1" und "8.30"); ein Markentreffer schließt umgebenden Leerraum ein.
 */
object Interpunktion {

    /** Position einer Marke: Anfang, Ende, innen, oder ganze Zeile. */
    enum class Position { B, E, I, A }

    data class Marke(val mark: String, val position: Position)

    data class Zerlegung(val chunks: List<String>, val marken: List<Marke>)

    // Wortgetreu das Muster der Referenz (Punctuation._marks_re).
    private val MARKS = Regex(
        """(\s*(?:[“»:";¡\[¿!?{”}«…—)\](]|(?<![0-9])[,.]|[,.](?![0-9]))+\s*)+"""
    )

    fun preserve(zeile: String): Zerlegung {
        val treffer = MARKS.findAll(zeile).toList()
        if (treffer.isEmpty()) return Zerlegung(listOf(zeile), emptyList())

        if (treffer.size == 1 && treffer[0].value == zeile) {
            return Zerlegung(emptyList(), listOf(Marke(zeile, Position.A)))
        }

        val marken = treffer.mapIndexed { i, m ->
            val position = when {
                i == 0 && zeile.startsWith(m.value) -> Position.B
                i == treffer.lastIndex && zeile.endsWith(m.value) -> Position.E
                else -> Position.I
            }
            Marke(m.value, position)
        }

        // Referenzverfahren: fortlaufendes split an der jeweils nächsten Marke.
        val chunks = mutableListOf<String>()
        var rest = zeile
        for (marke in marken) {
            val teile = rest.split(marke.mark)
            chunks.add(teile[0])
            rest = teile.drop(1).joinToString(marke.mark)
        }
        chunks.add(rest)

        // Die Referenz filtert leere Chunks erst NACH der Zerlegung.
        return Zerlegung(chunks.filter { it.isNotEmpty() }, marken)
    }

    /**
     * Wiedereinsetzung — der Referenzalgorithmus mit den festen Aufrufwerten
     * der Windows-Kette: Wortseparator " ", strip=true.
     */
    fun restore(chunksEin: List<String>, markenEin: List<Marke>): List<String> {
        var text = chunksEin.toMutableList()
        var marken = markenEin.toMutableList()
        val ergebnis = mutableListOf<String>()
        var pos = 0

        while (text.isNotEmpty() || marken.isNotEmpty()) {
            when {
                marken.isEmpty() -> {
                    ergebnis.addAll(text)
                    text = mutableListOf()
                }
                text.isEmpty() -> {
                    ergebnis.add(marken.joinToString("") { it.mark })
                    marken = mutableListOf()
                }
                else -> {
                    // Einzeiler-Eingabe: der Zeilenindex der Referenz ist immer 0,
                    // also platziert nur pos==0 Marken; danach werden Chunks geleert.
                    if (pos == 0) {
                        val marke = marken.removeAt(0)
                        if (text[0].endsWith(" ")) text[0] = text[0].dropLast(1)
                        when (marke.position) {
                            Position.B -> text[0] = marke.mark + text[0]
                            Position.E -> {
                                ergebnis.add(text.removeAt(0) + marke.mark)
                                pos++
                            }
                            Position.A -> {
                                ergebnis.add(marke.mark)
                                pos++
                            }
                            Position.I -> {
                                if (text.size == 1) {
                                    text[0] = text[0] + marke.mark
                                } else {
                                    val erstes = text.removeAt(0)
                                    text[0] = erstes + marke.mark + text[0]
                                }
                            }
                        }
                    } else {
                        ergebnis.add(text.removeAt(0))
                        pos++
                    }
                }
            }
        }
        return ergebnis
    }
}
