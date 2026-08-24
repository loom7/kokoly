// SPDX-License-Identifier: GPL-3.0-or-later
package de.tilly.kokoly.tts

import de.tilly.kokoly.tts.pipeline.PhonemeFrontend
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DER M0-Prüfstein, Hälfte 2: Golden-Stufe B auf der JVM.
 *
 * Die Stufe-A-Fixtures (rohe espeak-Ausgabe der Referenz-DLL) spielen hier
 * espeak; geprüft wird die GESAMTE Kotlin-Nachverarbeitung zeichengenau gegen
 * die Endfassung der Windows-Referenz — für jeden Satz, in allen 8 Sprachen.
 *
 * Die Beweiskette schließt mit dem Gerätetest EspeakParityTest: dort ist
 * arm64-espeak == Referenz-DLL (Stufe A), hier ist Kotlin(Stufe A) ==
 * Endfassung. Zusammen: Android-Kette == Windows-Kette, ohne dass ein
 * JVM-Test je natives espeak laden müsste.
 */
class FrontendGoldenTest {

    private val vokabular: Set<Char> = run {
        val text = javaClass.getResourceAsStream("/vokabular.json")!!
            .bufferedReader().readText()
        val o = JSONObject(text)
        o.keys().asSequence().map { it.single() }.toSet()
    }

    private fun golden(name: String): JSONObject {
        val text = javaClass.getResourceAsStream("/golden/$name.json")!!
            .bufferedReader().readText()
        return JSONObject(text)
    }

    private fun pruefeSprache(name: String) {
        val saetze = golden(name).getJSONArray("saetze")
        assertTrue(saetze.length() > 0)
        for (i in 0 until saetze.length()) {
            val satz = saetze.getJSONObject(i)
            val text = satz.getString("text")
            val segs = satz.getJSONArray("segmente")

            // Die Fixtures spielen espeak: Chunks müssen in Reihenfolge und
            // Wortlaut EXAKT denen der Referenzabtrennung entsprechen — damit
            // ist der Interpunktions-Port gleich mitgeprüft.
            var naechstes = 0
            val frontend = PhonemeFrontend(vokabular) { chunk, _ ->
                assertTrue("[$name] mehr Chunks als die Referenz bei: $text",
                    naechstes < segs.length())
                val erwartet = segs.getJSONObject(naechstes)
                assertEquals("[$name] Abtrennung weicht ab bei: $text",
                    erwartet.getString("chunk"), chunk)
                naechstes++
                erwartet.getString("roh")
            }

            val ergebnis = frontend.verarbeite(text, name)
            assertEquals("[$name] weniger Chunks als die Referenz bei: $text",
                segs.length(), naechstes)
            assertEquals("[$name] Endfassung weicht ab bei: $text",
                satz.getString("endfassung"), ergebnis.phoneme)

            val verworfenSoll = satz.getJSONArray("verworfen")
            assertEquals("[$name] Verlustmeldung weicht ab bei: $text",
                (0 until verworfenSoll.length()).map { verworfenSoll.getString(it) },
                ergebnis.verworfen.map { it.toString() })
        }
    }

    @Test fun deutsch() = pruefeSprache("de")
    @Test fun englischUs() = pruefeSprache("en-us")
    @Test fun englischGb() = pruefeSprache("en-gb")
    @Test fun spanisch() = pruefeSprache("es")
    @Test fun franzoesisch() = pruefeSprache("fr-fr")
    @Test fun italienisch() = pruefeSprache("it")
    @Test fun portugiesisch() = pruefeSprache("pt-br")
    @Test fun hindi() = pruefeSprache("hi")
}
