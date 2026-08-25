// SPDX-License-Identifier: GPL-3.0-or-later
package de.tilly.kokoly.tts.pipeline

import android.content.Context
import android.util.Log
import de.tilly.kokoly.tts.model.ModellLager
import de.tilly.kokoly.tts.model.NpzStimmen
import de.tilly.kokoly.tts.model.OrtWandlung
import de.tilly.kokoly.tts.rules.de.Phonemregeln
import de.tilly.kokoly.tts.rules.de.Textregeln
import de.tilly.kokoly.tts.service.Sprachen

/**
 * Die eine Pipeline des Prozesses: espeak (resident) + Frontend + Kokoro.
 *
 * **Session-Politik (ADR-0012, entschieden mit den M2a-Zahlen):** genau EINE
 * residente ORT-Session; ein Sprachwechsel über Modellgruppen hinweg tauscht
 * sie (gemessene Ladezeit 0,8–1,0 s, mit .ort weniger). Zwei residente
 * Sessions kosteten dauerhaft ~700 MB PSS — der Tausch kostet eine Sekunde
 * beim seltenen Gruppenwechsel. Der Leerlauf-Timer (M5) entlädt nur die
 * Session, nie espeak.
 *
 * **Stimmvektoren:** lazy aus der npz-Bank (522 KB je Stimme), kleiner
 * LRU-Zwischenspeicher — die Bank selbst bleibt auf Platte.
 *
 * **Modellbezug (M2b):** ModellLager löst Dateien auf (Entwicklungsweg vor
 * Lager); .onnx wird beim ersten Laden einmalig nach .ort gewandelt.
 */
object EnginePipeline {

    private const val TAG = "KokolyPipeline"
    private const val STIMM_LRU = 3

    @Volatile private var vokabular: Map<Char, Long>? = null
    private var frontend: PhonemeFrontend? = null
    private val sperre = Any()

    private var session: KokoroSynthesizer? = null
    private var sessionGruppe: String? = null

    private val stimmCache = object : LinkedHashMap<String, FloatArray>(4, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, FloatArray>?) =
            size > STIMM_LRU
    }

    /** Trennt Sätze fürs Streaming: erst fertige Satzeinheit, dann Synthese. */
    private val SATZENDE = Regex("""(?<=[.!?…])\s+""")

    // ---------------------------------------------------------------- Bestand

    /** Erste vorhandene Modelldatei der Gruppe (Vorzugsreihenfolge aus Sprachen). */
    fun modellDatei(context: Context, gruppe: Sprachen.Modellgruppe) =
        gruppe.modelle.firstNotNullOfOrNull { ModellLager.datei(context, it) }

    fun stimmbank(context: Context, gruppe: Sprachen.Modellgruppe) =
        ModellLager.datei(context, gruppe.stimmbank)

    fun gruppeVorhanden(context: Context, gruppe: Sprachen.Modellgruppe): Boolean =
        modellDatei(context, gruppe) != null && stimmbank(context, gruppe) != null

    // ---------------------------------------------------------------- Aufbau

    fun starte(context: Context) {
        synchronized(sperre) {
            // espeak-init VOR der Vokabular-Abkürzung: nach einem terminate
            // (z. B. Testaufräumen) heilt der nächste Start die Bindung —
            // init selbst ist idempotent und im Normalfall ein Flag-Test.
            EspeakNative.init(EspeakData.ensure(context).absolutePath)
            if (vokabular != null) return
            val v = Vokabular.lade(context)
            vokabular = v
            frontend = PhonemeFrontend(v.keys) { chunk, sprache ->
                EspeakNative.phonemisiere(chunk, sprache)
            }
        }
    }

    private fun sessionFuer(context: Context, gruppe: Sprachen.Modellgruppe): KokoroSynthesizer {
        synchronized(sperre) {
            session?.let { if (sessionGruppe == gruppe.kennung) return it }
            // Gruppenwechsel: die alte Session fällt, bevor die neue lädt —
            // nie zwei 300-MB-Arenen gleichzeitig (ADR-0012).
            session?.schliesse()
            session = null

            val onnx = requireNotNull(modellDatei(context, gruppe)) {
                "Kein Modell der Gruppe ${gruppe.kennung} vorhanden"
            }
            // Entwicklungsdateien (extern) werden nicht gewandelt — das externe
            // Verzeichnis ist Wegwerf-Boden; die .ort-Wandlung gilt dem Lager.
            val datei = if (onnx.name.endsWith(".onnx") &&
                onnx.parentFile == ModellLager.lagerOrdner(context)) {
                OrtWandlung.sichere(onnx)
            } else onnx

            val t0 = System.nanoTime()
            val neu = KokoroSynthesizer(datei, threads = 4)
            Log.i(TAG, "Session ${gruppe.kennung} (${datei.name}) in %.2f s"
                .format((System.nanoTime() - t0) / 1e9))
            session = neu
            sessionGruppe = gruppe.kennung
            return neu
        }
    }

    private fun stimmVektor(
        context: Context,
        gruppe: Sprachen.Modellgruppe,
        stimme: String,
    ): FloatArray {
        val schluessel = "${gruppe.kennung}/$stimme"
        synchronized(stimmCache) {
            stimmCache[schluessel]?.let { return it }
        }
        val bank = requireNotNull(stimmbank(context, gruppe)) {
            "Stimmbank ${gruppe.stimmbank} fehlt"
        }
        val vektor = NpzStimmen(bank).vektor(stimme)
        synchronized(stimmCache) { stimmCache[schluessel] = vektor }
        return vektor
    }

    fun entladeModell() {
        synchronized(sperre) {
            session?.schliesse()
            session = null
            sessionGruppe = null
        }
    }

    /** Reicht onStop an den laufenden Modell-Run durch (setTerminate). */
    fun brichAb() {
        synchronized(sperre) { session?.brichAb() }
    }

    // ---------------------------------------------------------------- Synthese

    /**
     * Synthetisiert satzweise mit der gegebenen Stimme; false, sobald
     * [liefere] abbricht (Stop-Flag des Dienstes oder voller Kanal).
     */
    fun synthetisiere(
        context: Context,
        text: String,
        sprache: Sprachen.Sprache,
        stimme: String,
        tempo: Float,
        liefere: (FloatArray) -> Boolean,
    ): Boolean {
        starte(context)
        val front = frontend!!
        val vokab = vokabular!!
        val k = sessionFuer(context, sprache.gruppe)
        k.loescheAbbruch() // Stop des VORIGEN Auftrags gilt nicht mehr
        val vektor = stimmVektor(context, sprache.gruppe, stimme)

        val saetze = text.split(SATZENDE).filter { it.isNotBlank() }
        for (satz in saetze) {
            // Deutsche Regelstufe (M3): Textregeln VOR dem Phonemisierer,
            // Betonung + Wortlaute danach — nur für Deutsch, die Regelwerke
            // sind deutsch („z.B." auszuschreiben wäre im Englischen falsch).
            val deutsch = sprache.espeak == "de"
            val (satzText, textMeldungen) =
                if (deutsch) Textregeln.berichtige(PhonemeFrontend.normalisiere(satz))
                    .let { it.text to it.meldungen }
                else satz to emptyList()
            textMeldungen.forEach { Log.i(TAG, it) }

            val ergebnis = front.verarbeite(satzText, sprache.espeak)
            if (ergebnis.verworfen.isNotEmpty()) {
                // Nie still: die Sichtbarkeitsregel aus der Windows-Referenz.
                Log.w(TAG, "Nicht im Vokabular, verworfen: ${ergebnis.verworfen} bei »$satz«")
            }
            if (ergebnis.phoneme.isBlank()) continue

            val phoneme =
                if (deutsch) Phonemregeln.berichtige(satzText, ergebnis.phoneme)
                    .also { r -> r.meldungen.forEach { Log.i(TAG, it) } }.phoneme
                else ergebnis.phoneme

            // Ehrliche Grenze: ein EINZELsatz über dem Modellfenster wird hart
            // geteilt; die feine Stückelung (Pausen 0,22/0,35, continuous) ist
            // offener Pipeline-Punkt.
            for (fenster in phoneme.chunked(KokoroSynthesizer.FENSTER - 2)) {
                val audio = k.synthetisiere(fenster, vokab, vektor, tempo)
                if (!liefere(audio)) return false
            }
        }
        return true
    }
}
