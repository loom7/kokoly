// SPDX-License-Identifier: GPL-3.0-or-later
package de.tilly.kokoly.tts

import android.os.Debug
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.tilly.kokoly.tts.pipeline.EspeakData
import de.tilly.kokoly.tts.pipeline.EspeakNative
import de.tilly.kokoly.tts.pipeline.KokoroSynthesizer
import de.tilly.kokoly.tts.pipeline.PhonemeFrontend
import de.tilly.kokoly.tts.pipeline.Vokabular
import de.tilly.kokoly.tts.pipeline.Wav
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Locale

/**
 * M2a-Messrahmen: EINE Konfiguration je Aufruf, gesteuert über
 * Instrumentation-Argumente — der Host fährt damit die Matrix ab:
 *
 *   am instrument … -e modell kokoro-v1.0.fp16.onnx -e threads 2 -e ep xnnpack
 *
 * Gemessen: Session-Ladezeit, RTF je Satz (kalt = 1. Satz nach Laden, warm =
 * Mittel der Folgesätze), PSS vor/nach Laden/nach Synthese (Debug.MemoryInfo).
 * Ergebnis als eine JSON-Zeile ins externe Verzeichnis (mess-<name>.json) und
 * ins Log. Je Lauf entsteht zusätzlich ein Hör-WAV des ersten Satzes.
 *
 * Bewusst KEINE mAh-Messung hier: die braucht lange ungestörte Strecken und
 * läuft als eigener M5-Punkt, nie am Kabel.
 */
@RunWith(AndroidJUnit4::class)
class MessMatrixTest {

    private companion object { const val TAG = "KokolyMess" }

    private val saetzeDe = listOf(
        "Der Prüfstand misst die Geschwindigkeit der Synthese auf diesem Gerät.",
        "Ein zweiter Satz zeigt das eingeschwungene Verhalten nach dem ersten Lauf.",
        "Der dritte Satz bestätigt, ob die Werte stabil bleiben oder driften.",
        "Zum Abschluss noch ein längerer Satz, der die Rechenlast etwas erhöht und damit näher an echtem Vorlesen liegt.",
    )
    private val saetzeEn = listOf(
        "The test bench measures synthesis speed on this device.",
        "A second sentence shows the warmed up behaviour after the first run.",
        "The third sentence confirms whether the numbers stay stable or drift.",
        "Finally a longer sentence that raises the load a little and comes closer to real reading aloud.",
    )

    private fun pssKb(): Long {
        val mi = Debug.MemoryInfo()
        Debug.getMemoryInfo(mi)
        return mi.totalPss.toLong()
    }

    @Test
    fun messeEineKonfiguration() {
        val args = InstrumentationRegistry.getArguments()
        val modellName = args.getString("modell") ?: "kokoro-martin.onnx"
        val threads = (args.getString("threads") ?: "4").toInt()
        val ep = args.getString("ep") ?: "cpu"

        val ziel = InstrumentationRegistry.getInstrumentation().targetContext
        val extern = ziel.getExternalFilesDir(null)!!
        val modell = File(extern, modellName)
        assumeTrue("Modell $modellName fehlt (adb push)", modell.exists())

        // martin: eigener Stilvektor + Deutsch; v1.0: af_heart + Englisch.
        val martin = modellName.startsWith("kokoro-martin")
        val stimme = File(extern, if (martin) "martin-voice.f32" else "af_heart-voice.f32")
        assumeTrue("Stimmvektor fehlt", stimme.exists())
        val sprache = if (martin) "de" else "en-us"
        val saetze = if (martin) saetzeDe else saetzeEn

        EspeakNative.init(EspeakData.ensure(ziel).absolutePath)
        val vokabular = Vokabular.lade(ziel)
        val frontend = PhonemeFrontend(vokabular.keys) { chunk, spr ->
            EspeakNative.phonemisiere(chunk, spr)
        }
        val phoneme = saetze.map { frontend.verarbeite(it, sprache).phoneme }

        val pssVorher = pssKb()
        var t0 = System.nanoTime()
        val kokoro = KokoroSynthesizer(modell, stimme, threads, xnnpack = ep == "xnnpack")
        val ladezeitS = (System.nanoTime() - t0) / 1e9
        val pssGeladen = pssKb()

        val rtf = mutableListOf<Double>()
        var hoerprobe: FloatArray? = null
        for ((i, ph) in phoneme.withIndex()) {
            t0 = System.nanoTime()
            val audio = kokoro.synthetisiere(ph, vokabular)
            val rechenzeit = (System.nanoTime() - t0) / 1e9
            rtf.add(rechenzeit / (audio.size.toDouble() / KokoroSynthesizer.ABTASTRATE))
            if (i == 0) hoerprobe = audio
        }
        val pssSynthese = pssKb()

        val name = "${modellName.removeSuffix(".onnx")}-${ep}-${threads}t"
        hoerprobe?.let { Wav.schreibe(File(extern, "hoer-$name.wav"), it, KokoroSynthesizer.ABTASTRATE) }

        val zeile = """{"konfiguration":"$name","ladezeit_s":%.2f,""".format(Locale.ROOT, ladezeitS) +
            """"rtf_kalt":%.3f,"rtf_warm":%.3f,"rtf_alle":[%s],""".format(
                Locale.ROOT, rtf[0], rtf.drop(1).average(),
                rtf.joinToString(",") { "%.3f".format(Locale.ROOT, it) }) +
            """"pss_vorher_mb":%d,"pss_geladen_mb":%d,"pss_synthese_mb":%d}"""
                .format(Locale.ROOT, pssVorher / 1024, pssGeladen / 1024, pssSynthese / 1024)
        File(extern, "mess-$name.json").writeText(zeile)
        Log.i(TAG, zeile)

        kokoro.schliesse()
    }
}
