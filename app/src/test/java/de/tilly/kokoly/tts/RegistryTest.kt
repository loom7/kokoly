// SPDX-License-Identifier: GPL-3.0-or-later
package de.tilly.kokoly.tts

import de.tilly.kokoly.tts.service.Sprachen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Reine Logiktests des Sprachverzeichnisses + Wachposten für models.json. */
class RegistryTest {

    @Test
    fun stimmnamenSindWohlgeformtUndEindeutig() {
        val alle = Sprachen.ALLE.flatMap { s -> s.stimmen.map { Sprachen.stimmName(s, it) } }
        assertEquals("42 Stimmen erwartet (41 v1.0 + martin)", 42, alle.size)
        assertEquals("Stimmnamen müssen eindeutig sein", alle.size, alle.toSet().size)
        alle.forEach { assertTrue(it, Regex("""[a-z]{2}-[A-Z]{2}-\w+""").matches(it)) }
    }

    @Test
    fun aufloesungIstUmkehrfunktion() {
        for (s in Sprachen.ALLE) for (st in s.stimmen) {
            val (s2, st2) = Sprachen.aufloesen(Sprachen.stimmName(s, st))!!
            assertEquals(s.espeak, s2.espeak)
            assertEquals(st, st2)
        }
        assertNull(Sprachen.aufloesen("xx-YY-nichts"))
        assertNull(Sprachen.aufloesen("de-DE-af_heart")) // fremde Stimme, falsche Sprache
    }

    @Test
    fun localeAufloesungIso3UndIso2() {
        assertEquals("de", Sprachen.nachLocale("deu", "DEU")!!.espeak)
        assertEquals("de", Sprachen.nachLocale("de", "DE")!!.espeak)
        assertEquals("en-us", Sprachen.nachLocale("eng", "USA")!!.espeak)
        assertEquals("en-gb", Sprachen.nachLocale("eng", "GBR")!!.espeak)
        assertEquals("en-us", Sprachen.nachLocale("eng", null)!!.espeak) // erste gewinnt
        assertEquals("pt-br", Sprachen.nachLocale("por", "BRA")!!.espeak)
        assertNull(Sprachen.nachLocale("jpn", null)) // Stufe 2
    }

    @Test
    fun modelsJsonAssetGleichtDerWurzel() {
        // Das Manifest liegt doppelt (Wurzel für Build/Doku, Asset für die
        // App). Dieser Wachposten hält beide Schriften identisch.
        val wurzel = File("../models.json").readText()
        val asset = File("src/main/assets/models.json").readText()
        assertEquals("models.json driftet: Wurzel und Asset abgleichen!", wurzel, asset)
    }
}
