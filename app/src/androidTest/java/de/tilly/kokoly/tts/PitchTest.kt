// SPDX-License-Identifier: GPL-3.0-or-later
package de.tilly.kokoly.tts

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.tilly.kokoly.tts.pipeline.EnginePipeline
import de.tilly.kokoly.tts.service.Sprachen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Tonhöhe über die Standard-API (Nutzerfund 25.08.: der Systemregler war tot).
 *
 * Prüft am Gerät: setPitch wirkt, und die DAUER bleibt dabei erhalten (das
 * Versprechen der Tempokompensation). Die Tonhöhen-VERSCHIEBUNG selbst wird
 * host-seitig per F0-Messung an den gezogenen WAVs belegt — hier zählt nur,
 * dass sich die Dateien deutlich unterscheiden und gleich lang sind.
 */
@RunWith(AndroidJUnit4::class)
class PitchTest {

    @Test
    fun tonhoeheWirktUndDauerBleibt() {
        val kontext = InstrumentationRegistry.getInstrumentation().targetContext
        assumeTrue(EnginePipeline.gruppeVorhanden(kontext, Sprachen.MARTIN))

        val start = CountDownLatch(1)
        lateinit var tts: TextToSpeech
        tts = TextToSpeech(kontext, { start.countDown() }, "de.tilly.kokoly.tts")
        assertTrue(start.await(30, TimeUnit.SECONDS))
        tts.setLanguage(Locale.GERMANY)
        // Fest 1,0 statt Systemvorgabe: der Nutzer-Tempo-Regler (z. B. 164 %)
        // drückte modellTempo sonst in die Klemmung und der Test hinge vom
        // Gerätezustand ab.
        tts.setSpeechRate(1.0f)

        val satz = "Die Tonhöhe dieser Stimme lässt sich jetzt verstellen."
        val laengen = mutableMapOf<String, Long>()
        for ((name, ton) in listOf("normal" to 1.0f, "hoch" to 1.4f, "tief" to 0.75f)) {
            tts.setPitch(ton)
            val ziel = File(kontext.getExternalFilesDir(null), "ton-$name.wav")
            ziel.delete()
            val fertig = CountDownLatch(1)
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) = Unit
                override fun onDone(id: String?) { fertig.countDown() }
                @Deprecated("Framework-Altpfad")
                override fun onError(id: String?) { fertig.countDown() }
                override fun onError(id: String?, code: Int) { fertig.countDown() }
            })
            tts.synthesizeToFile(satz, null, ziel, name)
            assertTrue("$name nicht fertig", fertig.await(120, TimeUnit.SECONDS))
            assertTrue("$name leer", ziel.length() > 50_000)
            laengen[name] = ziel.length()
        }
        tts.shutdown()

        // Dauer erhalten: WAV-Längen (PCM16, gleiche Rate) innerhalb ±12 %.
        val normal = laengen.getValue("normal").toDouble()
        for ((name, l) in laengen) {
            assertTrue("Dauer weicht ab bei $name: $l gegen $normal",
                l / normal in 0.88..1.12)
        }
        assertTrue("hoch == normal? Tonhöhe wirkungslos",
            laengen.getValue("hoch") != laengen.getValue("normal"))
    }
}
