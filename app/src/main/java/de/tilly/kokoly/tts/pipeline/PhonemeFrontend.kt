// SPDX-License-Identifier: GPL-3.0-or-later
package de.tilly.kokoly.tts.pipeline

import java.text.Normalizer

/**
 * Die Nachverarbeitung zwischen espeak und Modell — der 1:1-Port der
 * Windows-Kette (Spezifikation und Beweis: scripts/golden/generieren.py,
 * dort für jeden Korpussatz byte-identisch zur Referenz nachgewiesen).
 *
 * Schritte: NFC+Leerraum → Interpunktion abtrennen → je Chunk espeak (roh) →
 * '_' raus, Marken \(.+?\) raus → Interpunktion einsetzen → ʏ→y →
 * Vokabularfilter (Verlust wird GEMELDET, nie still).
 */
class PhonemeFrontend(
    private val vokabular: Set<Char>,
    private val espeak: (chunk: String, sprache: String) -> String,
) {

    data class Ergebnis(val phoneme: String, val verworfen: List<Char>)

    /** espeaks Sprachwechselmarken — Muster und Ort exakt wie die Referenz. */
    private val flags = Regex("""\(.+?\)""")

    fun verarbeite(text: String, sprache: String): Ergebnis {
        val norm = normalisiere(text)
        val (chunks, marken) = Interpunktion.preserve(norm)

        val nach = chunks.map { chunk ->
            var s = espeak(chunk, sprache)
            s = s.replace("_", "").replace("\n", " ")
            s = s.replace(Regex("""\s+"""), " ").trim()
            flags.replace(s, "")
        }

        val wieder = Interpunktion.restore(nach, marken)
        var ganz = wieder.filter { it.isNotEmpty() }.joinToString(" ").trim()
        ganz = ganz.replace(Regex("""\s+"""), " ")
        ganz = ganz.replace('ʏ', 'y')

        val behalten = StringBuilder()
        val verworfen = sortedSetOf<Char>()
        for (z in ganz) {
            when {
                z in vokabular -> behalten.append(z)
                z.isWhitespace() -> behalten.append(z)
                else -> verworfen.add(z)
            }
        }
        return Ergebnis(behalten.toString().trim(), verworfen.toList())
    }

    companion object {
        fun normalisiere(text: String): String =
            Normalizer.normalize(text, Normalizer.Form.NFC)
                .split(Regex("""\s+""")).filter { it.isNotEmpty() }.joinToString(" ")
    }
}
