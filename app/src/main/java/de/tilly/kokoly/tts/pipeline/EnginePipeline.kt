// SPDX-License-Identifier: GPL-3.0-or-later
package de.tilly.kokoly.tts.pipeline

import android.content.Context
import android.util.Log
import de.tilly.kokoly.tts.rules.de.Phonemregeln
import de.tilly.kokoly.tts.rules.de.Textregeln
import java.io.File

/**
 * Die eine Pipeline des Prozesses: espeak (resident) + Frontend + Kokoro.
 *
 * M1-Stand: nur Deutsch (kokoro-martin), Modell liegt im externen
 * App-Verzeichnis (adb push — der Downloader ist M2b). Lazy: espeak und
 * Vokabular beim ersten Zugriff, die ORT-Session beim ersten Satz.
 *
 * Energie-Grundsatz aus der Architektur: der (spätere) Leerlauf-Timer entlädt
 * nur die ORT-Session über [entladeModell] — espeak bleibt immer resident.
 */
object EnginePipeline {

    private const val TAG = "KokolyPipeline"

    @Volatile private var vokabular: Map<Char, Long>? = null
    private var frontend: PhonemeFrontend? = null
    private var kokoro: KokoroSynthesizer? = null
    private val sperre = Any()

    /** Trennt Sätze fürs Streaming: erst fertige Satzeinheit, dann Synthese. */
    private val SATZENDE = Regex("""(?<=[.!?…])\s+""")

    fun modellDatei(context: Context): File =
        File(context.getExternalFilesDir(null), "kokoro-martin.onnx")

    fun stimmDatei(context: Context): File =
        File(context.getExternalFilesDir(null), "martin-voice.f32")

    fun modellVorhanden(context: Context): Boolean =
        modellDatei(context).exists() && stimmDatei(context).exists()

    fun starte(context: Context) {
        synchronized(sperre) {
            if (vokabular != null) return
            EspeakNative.init(EspeakData.ensure(context).absolutePath)
            val v = Vokabular.lade(context)
            vokabular = v
            frontend = PhonemeFrontend(v.keys) { chunk, sprache ->
                EspeakNative.phonemisiere(chunk, sprache)
            }
        }
    }

    private fun ladeModell(context: Context): KokoroSynthesizer {
        synchronized(sperre) {
            kokoro?.let { return it }
            val t0 = System.nanoTime()
            val k = KokoroSynthesizer(modellDatei(context), stimmDatei(context), threads = 4)
            Log.i(TAG, "Kokoro-Session geladen in %.2f s".format((System.nanoTime() - t0) / 1e9))
            kokoro = k
            return k
        }
    }

    fun entladeModell() {
        synchronized(sperre) {
            kokoro?.schliesse()
            kokoro = null
        }
    }

    /**
     * Synthetisiert satzweise und liefert jedes Audio-Stück an [liefere];
     * bricht ab und gibt false zurück, sobald [liefere] false meldet
     * (Stop-Flag des Dienstes oder voller Kanal).
     */
    fun synthetisiere(
        context: Context,
        text: String,
        sprache: String,
        tempo: Float,
        liefere: (FloatArray) -> Boolean,
    ): Boolean {
        starte(context)
        val front = frontend!!
        val vokab = vokabular!!
        val k = ladeModell(context)

        val saetze = text.split(SATZENDE).filter { it.isNotBlank() }
        for (satz in saetze) {
            // Deutsche Regelstufe (M3): Textregeln VOR dem Phonemisierer,
            // Betonung + Wortlaute danach — Reihenfolge der Referenz. Für
            // andere Sprachen läuft nur die Phonemisierung (die Regelwerke
            // sind deutsch; „z.B." auszuschreiben wäre im Englischen falsch).
            val deutsch = sprache == "de"
            val (satzText, textMeldungen) =
                if (deutsch) Textregeln.berichtige(PhonemeFrontend.normalisiere(satz))
                    .let { it.text to it.meldungen }
                else satz to emptyList()
            textMeldungen.forEach { Log.i(TAG, it) }

            val ergebnis = front.verarbeite(satzText, sprache)
            if (ergebnis.verworfen.isNotEmpty()) {
                // Nie still: die Sichtbarkeitsregel aus der Windows-Referenz.
                Log.w(TAG, "Nicht im Vokabular, verworfen: ${ergebnis.verworfen} bei »$satz«")
            }
            if (ergebnis.phoneme.isBlank()) continue
            // M1-Grenze, ehrlich: ein EINZELsatz über dem Modellfenster wird
            // hart geteilt, ohne Pausenfeinsteuerung. Die volle Stückelung
            // (Pausen 0,22/0,35, continuous-Verhalten) ist M3-Arbeit.
            val phoneme =
                if (deutsch) Phonemregeln.berichtige(satzText, ergebnis.phoneme)
                    .also { r -> r.meldungen.forEach { Log.i(TAG, it) } }.phoneme
                else ergebnis.phoneme

            for (fenster in phoneme.chunked(KokoroSynthesizer.FENSTER - 2)) {
                val audio = k.synthetisiere(fenster, vokab, tempo)
                if (!liefere(audio)) return false
            }
        }
        return true
    }
}
