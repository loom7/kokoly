// SPDX-License-Identifier: GPL-3.0-or-later
package de.tilly.kokoly.tts

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.tilly.kokoly.tts.pipeline.EspeakData
import de.tilly.kokoly.tts.pipeline.EspeakNative
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

/**
 * DER M0-Prüfstein, Hälfte 1: Golden-Stufe A am Gerät.
 *
 * Vergleicht die rohe espeak-Ausgabe des arm64-Builds (Tag 1.52.0, eigener
 * NDK-Build) ZEICHENGENAU gegen die Stufe-A-Goldens der Windows-Referenz-DLL
 * (identischer Tag, identischer Aufruf). Besteht dieser Test, ist bewiesen:
 * unser espeak spricht auf Android dieselben Phoneme wie die Referenz — die
 * Fehlerklasse des historischen ich-Laut-Fehlschlags ist damit ausgeschlossen,
 * bevor die erste Audio-Sekunde erzeugt wird.
 *
 * M0 bündelt nur de+en-Daten; geprüft werden de-Goldens (enthalten die
 * Sprachwechselfälle nach en). Die übrigen Sprachen folgen mit 4.5.
 */
@RunWith(AndroidJUnit4::class)
class EspeakParityTest {

    companion object {
        @JvmStatic
        @BeforeClass
        fun espeakStarten() {
            val ziel = InstrumentationRegistry.getInstrumentation().targetContext
            EspeakNative.init(EspeakData.ensure(ziel).absolutePath)
        }
    }

    private fun golden(name: String): JSONObject {
        val test = InstrumentationRegistry.getInstrumentation().context
        val text = test.assets.open("golden/$name.json").bufferedReader().readText()
        return JSONObject(text)
    }

    @Test
    fun deutscheRohphonemeSindZeichengleich() {
        val daten = golden("de")
        val saetze = daten.getJSONArray("saetze")
        val abweichungen = StringBuilder()
        var segmente = 0
        for (i in 0 until saetze.length()) {
            val satz = saetze.getJSONObject(i)
            val segs = satz.getJSONArray("segmente")
            for (j in 0 until segs.length()) {
                val chunk = segs.getJSONObject(j).getString("chunk")
                val soll = segs.getJSONObject(j).getString("roh")
                val ist = EspeakNative.phonemisiere(chunk, "de")
                segmente++
                if (ist != soll) {
                    abweichungen.append("Satz: ").append(satz.getString("text"))
                        .append("\n  Chunk: ").append(chunk)
                        .append("\n  Soll : ").append(soll)
                        .append("\n  Ist  : ").append(ist).append("\n")
                }
            }
        }
        assertTrue("$segmente Segmente geprüft, Abweichungen:\n$abweichungen",
            abweichungen.isEmpty())
        assertTrue("kein einziges Segment geprüft?", segmente > 0)
    }

    @Test
    fun stimmwechselIstStabil() {
        // Nach einem Wechsel de→en→de muss de wieder exakt Referenz liefern —
        // die aktiveStimme-Buchführung darf nicht driften.
        val daten = golden("de").getJSONArray("saetze").getJSONObject(0)
        val chunk = daten.getJSONArray("segmente").getJSONObject(0).getString("chunk")
        val soll = daten.getJSONArray("segmente").getJSONObject(0).getString("roh")
        val erst = EspeakNative.phonemisiere(chunk, "de")
        EspeakNative.phonemisiere("hello world", "en-gb")
        val danach = EspeakNative.phonemisiere(chunk, "de")
        assertEquals(soll, erst)
        assertEquals(soll, danach)
    }
}
