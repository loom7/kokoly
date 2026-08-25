// SPDX-License-Identifier: GPL-3.0-or-later
package de.tilly.kokoly.tts

import de.tilly.kokoly.tts.pipeline.PhonemeFrontend
import de.tilly.kokoly.tts.rules.de.Phonemregeln
import de.tilly.kokoly.tts.rules.de.Textregeln
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Golden-Stufe C: das deutsche Regelwerk, zeichengenau gegen die Referenz.
 *
 * Je Korpussatz drei Prüfungen in einem Lauf:
 *  1. Kotlin-Textregeln == Referenz-Textregeln (Textebene),
 *  2. Frontend auf dem regelbereinigten Text == Referenz (Fixtures spielen espeak),
 *  3. Betonung + Wortlaute == Referenz-Endfassung (Phonemebene).
 *
 * Der Korpus enthält absichtlich die Gegenproben (2:1, Version 2.1.4,
 * „August Meier", „Die Regie der Regierung") — Nicht-Greifen ist genauso
 * golden-gesichert wie Greifen.
 */
class RegelnGoldenTest {

    private val vokabular: Set<Char> = run {
        val text = javaClass.getResourceAsStream("/vokabular.json")!!
            .bufferedReader().readText()
        JSONObject(text).keys().asSequence().map { it.single() }.toSet()
    }

    @Test
    fun deutschesRegelwerkIstReferenzgleich() {
        val text = javaClass.getResourceAsStream("/golden/de-regeln.json")!!
            .bufferedReader().readText()
        val saetze = JSONObject(text).getJSONArray("saetze")
        assertTrue(saetze.length() > 0)

        for (i in 0 until saetze.length()) {
            val satz = saetze.getJSONObject(i)
            val eingabe = satz.getString("text")

            // 1) Textregeln
            val nachText = Textregeln.berichtige(
                PhonemeFrontend.normalisiere(eingabe)).text
            assertEquals("Textregeln weichen ab bei: $eingabe",
                satz.getString("nach_textregeln"), nachText)

            // 2) Frontend auf dem regelbereinigten Text (Fixtures spielen espeak)
            val segs = satz.getJSONArray("segmente")
            var naechstes = 0
            val frontend = PhonemeFrontend(vokabular) { chunk, _ ->
                assertTrue("mehr Chunks als die Referenz bei: $eingabe",
                    naechstes < segs.length())
                val erwartet = segs.getJSONObject(naechstes)
                assertEquals("Abtrennung weicht ab bei: $eingabe",
                    erwartet.getString("chunk"), chunk)
                naechstes++
                erwartet.getString("roh")
            }
            val phoneme = frontend.verarbeite(nachText, "de").phoneme
            assertEquals("Frontend weicht ab bei: $eingabe",
                satz.getString("phoneme_ohne_regeln"), phoneme)

            // 3) Betonung + Wortlaute
            val endfassung = Phonemregeln.berichtige(nachText, phoneme).phoneme
            assertEquals("Regelstufe weicht ab bei: $eingabe",
                satz.getString("endfassung"), endfassung)
        }
    }
}
