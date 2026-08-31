// SPDX-License-Identifier: GPL-3.0-or-later
package de.tilly.kokoly.tts

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.tilly.kokoly.tts.pipeline.EnginePipeline
import de.tilly.kokoly.tts.service.Sprachen
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Der Leerlauf-Timer (M5-Festlegung): nach [EnginePipeline.leerlaufMs] ohne
 * Synthese fällt die ORT-Session; espeak bleibt resident, und der nächste
 * Auftrag lädt die Session einfach neu (gemessene 0,8–1,0 s).
 */
@RunWith(AndroidJUnit4::class)
class LeerlaufTest {

    @Test
    fun sessionFaelltNachLeerlaufUndKommtWieder() {
        val kontext = InstrumentationRegistry.getInstrumentation().targetContext
        assumeTrue(EnginePipeline.gruppeVorhanden(kontext, Sprachen.MARTIN))
        val de = Sprachen.ALLE.first { it.espeak == "de" }
        val vorgabe = EnginePipeline.leerlaufMs
        try {
            EnginePipeline.leerlaufMs = 4_000

            var samples = 0
            assertTrue(EnginePipeline.synthetisiere(kontext, "Guten Tag.", de, "martin", 1.0f) {
                samples += it.size; true
            })
            assertTrue("kein Audio", samples > 10_000)
            assertTrue("Session fehlt direkt nach Synthese", EnginePipeline.sessionGeladen())

            Thread.sleep(6_500)
            assertFalse("Session überlebte den Leerlauf", EnginePipeline.sessionGeladen())

            samples = 0
            assertTrue(EnginePipeline.synthetisiere(kontext, "Und wieder da.", de, "martin", 1.0f) {
                samples += it.size; true
            })
            assertTrue("Neuladen lieferte kein Audio", samples > 10_000)
            assertTrue(EnginePipeline.sessionGeladen())
        } finally {
            EnginePipeline.leerlaufMs = vorgabe
        }
    }
}
