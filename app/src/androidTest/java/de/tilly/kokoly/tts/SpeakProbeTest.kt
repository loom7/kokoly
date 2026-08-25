// SPDX-License-Identifier: GPL-3.0-or-later
package de.tilly.kokoly.tts

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Sprechprobe über den Lautsprecherpfad — wie der Wiedergabe-Knopf der
 * Systemeinstellungen, mit den Reglerwerten des Nutzerfunds vom 25.08.2026
 * als Vorgabe (überschreibbar per -e pitch/-e rate/-e text).
 */
@RunWith(AndroidJUnit4::class)
class SpeakProbeTest {
    @Test
    fun sprichMitSystemwerten() {
        val kontext = InstrumentationRegistry.getInstrumentation().targetContext
        val argumente = InstrumentationRegistry.getArguments()
        val pitch = (argumente.getString("pitch") ?: "0.83").toFloat()
        val rate = (argumente.getString("rate") ?: "1.64").toFloat()
        val text = argumente.getString("text")
            ?: "Guten Tag, hier spricht Martin. Der Termin ist am dritten August um halb zehn."

        val start = CountDownLatch(1)
        lateinit var tts: TextToSpeech
        tts = TextToSpeech(kontext, { start.countDown() }, "de.tilly.kokoly.tts")
        assertTrue(start.await(30, TimeUnit.SECONDS))
        tts.setLanguage(Locale.GERMANY)
        tts.setPitch(pitch)
        tts.setSpeechRate(rate)

        val fertig = CountDownLatch(1)
        var fehler = -1
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) = Unit
            override fun onDone(id: String?) { fertig.countDown() }
            @Deprecated("Framework-Altpfad")
            override fun onError(id: String?) { fertig.countDown() }
            override fun onError(id: String?, code: Int) { fehler = code; fertig.countDown() }
        })
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "probe")
        assertTrue("speak nicht fertig geworden", fertig.await(120, TimeUnit.SECONDS))
        tts.shutdown()
        assertTrue("onError-Code $fehler", fehler == -1)
    }
}
