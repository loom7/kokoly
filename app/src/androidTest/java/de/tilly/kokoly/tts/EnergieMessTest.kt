// SPDX-License-Identifier: GPL-3.0-or-later
package de.tilly.kokoly.tts

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.tilly.kokoly.tts.pipeline.EspeakData
import de.tilly.kokoly.tts.pipeline.EspeakNative
import de.tilly.kokoly.tts.pipeline.KokoroSynthesizer
import de.tilly.kokoly.tts.pipeline.PhonemeFrontend
import de.tilly.kokoly.tts.pipeline.Vokabular
import de.tilly.kokoly.tts.rules.de.Phonemregeln
import de.tilly.kokoly.tts.rules.de.Textregeln
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale

/**
 * M5: mAh je 1000 Zeichen, je Modellvariante und Threadzahl.
 *
 * Messkultur (CLAUDE.md): NIE am Kabel — der Test bricht hart ab, wenn das
 * Gerät lädt. Ladung über BATTERY_PROPERTY_CHARGE_COUNTER (µAh; am SM-F971B
 * in ~4,2-mAh-Schritten quantisiert — die Läufe sind deshalb lang genug für
 * ein Vielfaches der Schrittweite). Der Prüfstand hält einen partiellen
 * Wakelock (dokumentierter Harness-Anteil); die Engine selbst hält keinen.
 *
 * Aufruf je Konfiguration:
 *   am instrument -w -e class de.tilly.kokoly.tts.EnergieMessTest \
 *     -e modell kokoro-martin.onnx -e threads 4 -e zeichen 8000 -e name fp32-4t \
 *     de.tilly.kokoly.tts.test/androidx.test.runner.AndroidJUnitRunner
 * Ergebnis: energie-<name>.json im externen App-Verzeichnis.
 */
@RunWith(AndroidJUnit4::class)
class EnergieMessTest {

    // Fester Lesetext (~530 Zeichen) — realistisch gemischt: Regeln, Zahlen,
    // Datum, Komposita. Für ALLE Konfigurationen identisch wiederholt.
    private val ABSATZ =
        "Guten Tag, hier spricht Martin. Der Termin ist am dritten August um " +
        "halb zehn. Die Sprachsynthese läuft auf diesem Gerät, und die " +
        "Photosynthese ist auch eine Synthese. Ich möchte nicht, dass mich " +
        "vielleicht doch jemand richtig versteht. Das Frauchen ruft, wir " +
        "rauchen nicht. Die Erbsen sind gekocht, die Sauce ist fertig. " +
        "Nach dem 3. August wird es kühler, z. B. abends um 19:30 Uhr. " +
        "Die Lebensversicherung kostet 1.250 Euro im Jahrzehnt. " +
        "Der Motor der Kaffeemaschine widerspricht dem Studentenwohnheim. "

    @Test
    fun messeEnergieJeTausendZeichen() {
        val kontext = InstrumentationRegistry.getInstrumentation().targetContext
        val argumente = InstrumentationRegistry.getArguments()
        val modellName = argumente.getString("modell") ?: "kokoro-martin.onnx"
        val threads = (argumente.getString("threads") ?: "4").toInt()
        val zielZeichen = (argumente.getString("zeichen") ?: "8000").toInt()
        val name = argumente.getString("name") ?: "unbenannt"
        // Echtzeit-Taktung: nach jedem Satz warten, bis die Wanduhr der
        // Audiolänge entspricht — simuliert 30 Minuten VORLESEN (Synthese-
        // Schübe mit Leerlauf dazwischen) statt Dauervolllast.
        val echtzeit = (argumente.getString("echtzeit") ?: "false").toBoolean()

        val extern = kontext.getExternalFilesDir(null)!!
        val modell = File(extern, modellName)
        val stimme = File(extern, "martin-voice.f32")
        assumeTrue("Modell fehlt: $modellName", modell.exists() && stimme.exists())

        val batterie = kontext.getSystemService(BatteryManager::class.java)
        // NIE am Kabel: Laden verfälscht den Ladungszähler grundlegend.
        assertTrue("GERÄT LÄDT — Messung verweigert", !batterie.isCharging)

        val strom = kontext.getSystemService(PowerManager::class.java)
        val wachhalter = strom.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK, "kokoly:energiemessung")
        wachhalter.acquire(45 * 60 * 1000L)
        try {
            // Aufbau VOR der Messspanne — gemessen wird nur die Synthese.
            EspeakNative.init(EspeakData.ensure(kontext).absolutePath)
            val vokabular = Vokabular.lade(kontext)
            val frontend = PhonemeFrontend(vokabular.keys) { chunk, sprache ->
                EspeakNative.phonemisiere(chunk, sprache)
            }
            val vektor = stimme.readBytes().let { b ->
                val fb = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
                FloatArray(fb.remaining()).also { fb.get(it) }
            }
            val kokoro = KokoroSynthesizer(modell, threads = threads)

            val saetze = ABSATZ.trim().split(Regex("""(?<=[.!?])\s+"""))
            var zeichen = 0
            var audioSamples = 0L
            val ccStart = batterie.getLongProperty(
                BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
            val tempStart = temperatur(kontext)
            val t0 = System.nanoTime()

            while (zeichen < zielZeichen) {
                for (satz in saetze) {
                    if (zeichen >= zielZeichen) break
                    val (text, _) = Textregeln.berichtige(
                        PhonemeFrontend.normalisiere(satz)).let { it.text to it.meldungen }
                    val roh = frontend.verarbeite(text, "de")
                    val phoneme = Phonemregeln.berichtige(text, roh.phoneme).phoneme
                    if (phoneme.isBlank()) continue
                    for (fenster in phoneme.chunked(KokoroSynthesizer.FENSTER - 2)) {
                        audioSamples += kokoro.synthetisiere(fenster, vokabular, vektor).size
                    }
                    zeichen += satz.length
                    if (echtzeit) {
                        val sollWand = audioSamples * 1000L / KokoroSynthesizer.ABTASTRATE
                        val istWand = (System.nanoTime() - t0) / 1_000_000
                        if (sollWand > istWand) Thread.sleep(sollWand - istWand)
                    }
                }
            }

            val rechenSekunden = (System.nanoTime() - t0) / 1e9
            val ccEnde = batterie.getLongProperty(
                BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
            val tempEnde = temperatur(kontext)
            kokoro.schliesse()

            val audioSekunden = audioSamples.toDouble() / KokoroSynthesizer.ABTASTRATE
            val mAh = (ccStart - ccEnde) / 1000.0
            val json = String.format(Locale.ROOT,
                """{"name":"%s","modell":"%s","threads":%d,"zeichen":%d,""" +
                """"audioSekunden":%.1f,"rechenSekunden":%.1f,"rtf":%.3f,""" +
                """"ccStartMikroAh":%d,"ccEndeMikroAh":%d,"mAh":%.1f,""" +
                """"mAhJe1000Zeichen":%.2f,"tempStartZehntelC":%d,""" +
                """"tempEndeZehntelC":%d}""",
                name, modellName, threads, zeichen, audioSekunden, rechenSekunden,
                rechenSekunden / audioSekunden, ccStart, ccEnde, mAh,
                mAh * 1000.0 / zeichen, tempStart, tempEnde)
            File(extern, "energie-$name.json").writeText(json)
            Log.i("KokolyEnergie", json)
            assertTrue("Ladungszähler lief rückwärts?", ccStart >= ccEnde)
        } finally {
            wachhalter.release()
        }
    }

    private fun temperatur(kontext: android.content.Context): Int {
        val intent: Intent? = kontext.registerReceiver(
            null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
    }
}
