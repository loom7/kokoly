// SPDX-License-Identifier: GPL-3.0-or-later
package de.tilly.kokoly.tts

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.tilly.kokoly.tts.pipeline.EnginePipeline
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
 * M1-Abnahme: die Engine über die ECHTE Framework-Schnittstelle, wie sie jede
 * fremde App benutzt — TextToSpeech-Klient, gezielt an diese Engine gebunden,
 * setLanguage(GERMAN), synthesizeToFile.
 *
 * Das prüft Binden, Sprachaushandlung (ISO-3), Voice-Vergabe und den
 * Streaming-Weg durch SynthesisCallback in einem Zug. Ziel Z1 des Plans.
 */
@RunWith(AndroidJUnit4::class)
class TtsClientTest {

    @Test
    fun frameworkKlientSynthetisiertDeutsch() {
        val kontext = InstrumentationRegistry.getInstrumentation().targetContext
        assumeTrue("Modell fehlt (adb push)", EnginePipeline.modellVorhanden(kontext))

        val start = CountDownLatch(1)
        var initErgebnis = TextToSpeech.ERROR
        lateinit var tts: TextToSpeech
        tts = TextToSpeech(kontext, { rc ->
            initErgebnis = rc
            start.countDown()
        }, "de.tilly.kokoly.tts")
        assertTrue("Engine nicht initialisiert", start.await(30, TimeUnit.SECONDS))
        assertEquals("Init fehlgeschlagen", TextToSpeech.SUCCESS, initErgebnis)

        val sprachErgebnis = tts.setLanguage(Locale.GERMANY)
        assertTrue(
            "setLanguage(GERMANY) lieferte $sprachErgebnis",
            sprachErgebnis == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
                sprachErgebnis == TextToSpeech.LANG_AVAILABLE,
        )
        assertEquals("de-DE-martin", tts.voice?.name)

        val ziel = File(kontext.getExternalFilesDir(null), "m1-framework.wav")
        ziel.delete()
        val fertig = CountDownLatch(1)
        var fehler: String? = null
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) = Unit
            override fun onDone(id: String?) { fertig.countDown() }
            @Deprecated("Framework-Altpfad")
            override fun onError(id: String?) { fehler = "onError"; fertig.countDown() }
            override fun onError(id: String?, code: Int) { fehler = "onError($code)"; fertig.countDown() }
        })

        val satz = "Guten Tag. Kokoly spricht jetzt über die Android-Schnittstelle, " +
            "wie jede andere Stimme des Systems."
        tts.synthesizeToFile(satz, null, ziel, "m1")
        assertTrue("Synthese nicht fertig geworden", fertig.await(120, TimeUnit.SECONDS))
        assertEquals(null, fehler)
        assertTrue("WAV fehlt oder leer: ${ziel.length()} B", ziel.length() > 100_000)

        tts.shutdown()
    }
}
