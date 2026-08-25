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
 * Hörproben-Werkzeug für neue Regeln (docs/regelwerk.md, Schritt „Hörprobe“):
 * synthetisiert einen Satz über den ECHTEN Dienstweg — also samt Regelstufe,
 * die GateWavTest umgeht — in eine WAV zum Ziehen per adb pull.
 *
 * Aufruf mit eigenem Satz:
 *   am instrument -w -e class de.tilly.kokoly.tts.HoerprobeTest  *     -e text "Der Satz." -e datei hoer-regel  *     de.tilly.kokoly.tts.test/androidx.test.runner.AndroidJUnitRunner
 */
@RunWith(AndroidJUnit4::class)
class HoerprobeTest {

    @Test
    fun synthetisiereUeberDienst() {
        val kontext = InstrumentationRegistry.getInstrumentation().targetContext
        assumeTrue(EnginePipeline.gruppeVorhanden(kontext, Sprachen.MARTIN))
        val argumente = InstrumentationRegistry.getArguments()
        val text = argumente.getString("text")
            ?: "Die Photosynthese ist auch eine Synthese."
        val datei = argumente.getString("datei") ?: "hoer-probe"

        val start = CountDownLatch(1)
        lateinit var tts: TextToSpeech
        tts = TextToSpeech(kontext, { start.countDown() }, "de.tilly.kokoly.tts")
        assertTrue(start.await(30, TimeUnit.SECONDS))
        tts.setLanguage(Locale.GERMANY)
        // Fest 1,0/1,0: ohne Pinnen sickern die Systemregler (Tempo/Tonhöhe
        // der TTS-Einstellungen) in die Hörprobe — genau so entstand am
        // 25.08.2026 eine gestauchte Regel-Hörprobe, die nach verschluckten
        // Silben klang. Hörproben urteilen über REGELN, nie über Regler.
        tts.setSpeechRate((argumente.getString("rate") ?: "1.0").toFloat())
        tts.setPitch((argumente.getString("pitch") ?: "1.0").toFloat())

        val ziel = File(kontext.getExternalFilesDir(null), "$datei.wav")
        ziel.delete()
        val fertig = CountDownLatch(1)
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) = Unit
            override fun onDone(id: String?) { fertig.countDown() }
            @Deprecated("Framework-Altpfad")
            override fun onError(id: String?) { fertig.countDown() }
            override fun onError(id: String?, code: Int) { fertig.countDown() }
        })
        tts.synthesizeToFile(text, null, ziel, datei)
        assertTrue("Synthese nicht fertig", fertig.await(120, TimeUnit.SECONDS))
        tts.shutdown()
        assertTrue("WAV leer", ziel.length() > 50_000)
    }
}
