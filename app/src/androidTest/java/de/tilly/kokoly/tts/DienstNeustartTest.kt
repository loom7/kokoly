// SPDX-License-Identifier: GPL-3.0-or-later
package de.tilly.kokoly.tts

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.tilly.kokoly.tts.pipeline.EnginePipeline
import de.tilly.kokoly.tts.service.Sprachen
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Der Engine-Wechsel-Nachsteller (Nutzerfund 25.08.2026: Wiedergabe stumm,
 * nachdem in den Systemeinstellungen die Engine gewechselt und zurückgewechselt
 * wurde). Ablauf: Klient A spricht, shutdown → Dienst-onDestroy — der PROZESS
 * lebt aber weiter. Klient B bindet neu an DENSELBEN Prozess und muss wieder
 * sprechen können. Jeder frühere Test startete einen frischen Prozess und
 * konnte diesen Weg nicht sehen.
 */
@RunWith(AndroidJUnit4::class)
class DienstNeustartTest {

    private fun sprichInDatei(name: String): Long {
        val kontext = InstrumentationRegistry.getInstrumentation().targetContext
        val start = CountDownLatch(1)
        lateinit var tts: TextToSpeech
        tts = TextToSpeech(kontext, { start.countDown() }, "de.tilly.kokoly.tts")
        assertTrue("$name: Bindung nicht fertig", start.await(30, TimeUnit.SECONDS))
        tts.setLanguage(Locale.GERMANY)
        val ziel = File(kontext.getExternalFilesDir(null), "$name.wav")
        ziel.delete()
        val fertig = CountDownLatch(1)
        var fehlerCode = Int.MIN_VALUE
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) = Unit
            override fun onDone(id: String?) { fertig.countDown() }
            @Deprecated("Framework-Altpfad")
            override fun onError(id: String?) { fertig.countDown() }
            override fun onError(id: String?, code: Int) { fehlerCode = code; fertig.countDown() }
        })
        tts.synthesizeToFile("Guten Tag, hier spricht Martin.", null, ziel, name)
        assertTrue("$name: nicht fertig geworden", fertig.await(120, TimeUnit.SECONDS))
        tts.shutdown()
        assertTrue("$name: onError-Code $fehlerCode", fehlerCode == Int.MIN_VALUE)
        return ziel.length()
    }

    /** Stop mitten im langen Run (Stop-Pfad, Aufgabe 1.2): schnell und folgenlos. */
    @Test
    fun stopptMittenImLangenLaufUndSprichtDanach() {
        val kontext = InstrumentationRegistry.getInstrumentation().targetContext
        assumeTrue(EnginePipeline.gruppeVorhanden(kontext, Sprachen.MARTIN))
        val start = CountDownLatch(1)
        lateinit var tts: TextToSpeech
        tts = TextToSpeech(kontext, { start.countDown() }, "de.tilly.kokoly.tts")
        assertTrue(start.await(30, TimeUnit.SECONDS))
        tts.setLanguage(Locale.GERMANY)
        val lang = (1..8).joinToString(" ") {
            "Dieser sehr lange Satz Nummer $it dient allein dem Abbruchtest der Synthese."
        }
        tts.speak(lang, TextToSpeech.QUEUE_FLUSH, null, "langlauf")
        Thread.sleep(900) // mitten im ersten Modell-Run
        val t0 = System.nanoTime()
        tts.stop()
        val stopDauer = (System.nanoTime() - t0) / 1e6
        assertTrue("stop() blockierte %.0f ms".format(stopDauer), stopDauer < 1_500)
        Thread.sleep(500)
        tts.shutdown()
        // Danach muss die Engine unversehrt weitersprechen (frische Bindung).
        assertTrue("nach Stop leer", sprichInDatei("nach-stop") > 30_000)
    }

    /** shutdown() mitten im Run (Engine-Wechsel-Ernstfall): kein Absturz, danach lebendig. */
    @Test
    fun ueberlebtShutdownMittenImLauf() {
        val kontext = InstrumentationRegistry.getInstrumentation().targetContext
        assumeTrue(EnginePipeline.gruppeVorhanden(kontext, Sprachen.MARTIN))
        val start = CountDownLatch(1)
        lateinit var tts: TextToSpeech
        tts = TextToSpeech(kontext, { start.countDown() }, "de.tilly.kokoly.tts")
        assertTrue(start.await(30, TimeUnit.SECONDS))
        tts.setLanguage(Locale.GERMANY)
        val lang = (1..8).joinToString(" ") {
            "Dieser sehr lange Satz Nummer $it dient allein dem Abbruchtest der Synthese."
        }
        tts.speak(lang, TextToSpeech.QUEUE_FLUSH, null, "shutdownlauf")
        Thread.sleep(900)
        tts.shutdown() // unbind -> onDestroy -> Session schließt, Run läuft evtl. noch
        Thread.sleep(3_000)
        assertTrue("nach Shutdown leer/tot", sprichInDatei("nach-shutdown") > 30_000)
    }

    @Test
    fun sprichtNachUnbindRebindWeiter() {
        val kontext = InstrumentationRegistry.getInstrumentation().targetContext
        assumeTrue(EnginePipeline.gruppeVorhanden(kontext, Sprachen.MARTIN))

        val ersteLaenge = sprichInDatei("neustart-a")
        assertTrue("Klient A leer", ersteLaenge > 50_000)
        // shutdown → unbind → Dienst-onDestroy; der Prozess bleibt im Cache.
        Thread.sleep(4_000)
        val zweiteLaenge = sprichInDatei("neustart-b")
        assertTrue("Klient B leer/tot — Engine nach Rebind stumm", zweiteLaenge > 50_000)
    }
}
