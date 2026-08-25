// SPDX-License-Identifier: GPL-3.0-or-later
package de.tilly.kokoly.tts

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.tilly.kokoly.tts.pipeline.EspeakData
import de.tilly.kokoly.tts.pipeline.EspeakNative
import de.tilly.kokoly.tts.pipeline.KokoroSynthesizer
import de.tilly.kokoly.tts.pipeline.PhonemeFrontend
import de.tilly.kokoly.tts.pipeline.Vokabular
import de.tilly.kokoly.tts.pipeline.Wav
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * DER M0-Prüfstein, Hälfte 3: die ganze Kette am Gerät, hörbar.
 *
 * Text → Frontend (espeak arm64 + Nachverarbeitung) → Kokoro-ONNX → WAV.
 * Die Phonemketten sind durch EspeakParityTest + FrontendGoldenTest bereits
 * als referenzgleich bewiesen; hier entsteht das Audio für die
 * ich-Laut-Hörprobe (A/B gegen die Windows-WAVs, adb pull).
 *
 * Modell und Stimme liegen im externen App-Verzeichnis (adb push, M0-Weg —
 * der Downloader ist M2b). Fehlen sie, wird der Test ÜBERSPRUNGEN, nicht rot.
 */
@RunWith(AndroidJUnit4::class)
class GateWavTest {

    private val gateSaetze = listOf(
        "gate1-ichlaut" to "Ich möchte nicht, dass mich vielleicht doch jemand richtig versteht.",
        "gate2-frauchen" to "Das Frauchen ruft, wir rauchen nicht.",
        "gate3-martin" to "Guten Tag, hier spricht Martin. Der Termin ist am dritten August um halb zehn.",
    )

    @Test
    fun erzeugeGateWavs() {
        val ziel = InstrumentationRegistry.getInstrumentation().targetContext
        val extern = ziel.getExternalFilesDir(null)!!
        val modell = File(extern, "kokoro-martin.onnx")
        val stimme = File(extern, "martin-voice.f32")
        assumeTrue("Modell fehlt — adb push, siehe KDoc", modell.exists() && stimme.exists())

        val vektor = stimme.readBytes().let { b ->
            val fb = java.nio.ByteBuffer.wrap(b)
                .order(java.nio.ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
            FloatArray(fb.remaining()).also { fb.get(it) }
        }
        EspeakNative.init(EspeakData.ensure(ziel).absolutePath)
        val vokabular = Vokabular.lade(ziel)
        val frontend = PhonemeFrontend(vokabular.keys) { chunk, sprache ->
            EspeakNative.phonemisiere(chunk, sprache)
        }

        var t0 = System.nanoTime()
        val kokoro = KokoroSynthesizer(modell, threads = 4)
        val ladezeit = (System.nanoTime() - t0) / 1e9
        Log.i("KokolyGate", "Session geladen in %.2f s".format(ladezeit))

        for ((name, satz) in gateSaetze) {
            val ergebnis = frontend.verarbeite(satz, "de")
            assertTrue("Verlust bei $name: ${ergebnis.verworfen}",
                ergebnis.verworfen.isEmpty())
            t0 = System.nanoTime()
            val audio = kokoro.synthetisiere(ergebnis.phoneme, vokabular, vektor)
            val rechenzeit = (System.nanoTime() - t0) / 1e9
            val dauer = audio.size.toDouble() / KokoroSynthesizer.ABTASTRATE
            Log.i("KokolyGate", "%s: Phoneme »%s« — %.2f s Audio in %.2f s (RTF %.3f)"
                .format(name, ergebnis.phoneme, dauer, rechenzeit, rechenzeit / dauer))
            Wav.schreibe(File(extern, "$name.wav"), audio, KokoroSynthesizer.ABTASTRATE)
            assertTrue("Audio verdächtig kurz bei $name", dauer > 1.0)
        }
        kokoro.schliesse()
    }
}
