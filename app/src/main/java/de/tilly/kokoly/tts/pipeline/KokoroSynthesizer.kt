// SPDX-License-Identifier: GPL-3.0-or-later
package de.tilly.kokoly.tts.pipeline

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.File
import java.nio.FloatBuffer
import java.nio.LongBuffer
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Die Kokoro-Inferenz über ONNX Runtime.
 *
 * Eingabeaufbau exakt wie die Referenz (kokoro_onnx.Kokoro._infer):
 * Tokens [[0, …, 0]] int64, Stilzeile voice[min(n, 510) - 1] als [1, 256],
 * speed float32[1]. Ausgabe: float32-Audio, 24 kHz mono.
 *
 * Seit M4 stimmenlos gebaut: der Stilvektor kommt je Aufruf herein
 * (EnginePipeline hält den LRU-Zwischenspeicher) — eine Session bedient damit
 * alle Stimmen ihrer Modellgruppe. Lädt .onnx wie .ort gleichermaßen.
 */
class KokoroSynthesizer(
    modellDatei: File,
    threads: Int = 4,
    /** XNNPACK-EP — nur noch für Messläufe; im Betrieb CPU-EP (ADR-0015). */
    xnnpack: Boolean = false,
) {

    companion object {
        const val ABTASTRATE = 24000
        const val FENSTER = 510
        const val STILBREITE = 256
    }

    private val umgebung = OrtEnvironment.getEnvironment()
    private val sitzung: OrtSession
    private val tokensEingabe: String

    // Lauf/Schließen-Synchronisation: OrtSession.close() hat KEINE eigene
    // Absicherung gegen einen laufenden run() (1.28.0 geprüft) — ein close
    // vom Main-Thread (onDestroy bei Engine-Wechsel, künftig Leerlauf-Timer)
    // mitten im nativen Run wäre Use-after-free. Läufe halten den Lesepart,
    // schliesse() den Schreibpart (fair, damit der Schließer nicht verhungert);
    // setTerminate lässt den laufenden Run in Millisekunden aussteigen.
    private val laufSperre = ReentrantReadWriteLock(true)
    private val laufOptionen = OrtSession.RunOptions()
    @Volatile private var geschlossen = false

    init {
        val optionen = OrtSession.SessionOptions().apply {
            if (xnnpack) {
                // XNNPACK bringt den eigenen Threadpool mit; die ORT-Threads
                // bleiben dann auf 1, sonst drehen zwei Pools gegeneinander.
                addXnnpack(mapOf("intra_op_num_threads" to threads.toString()))
                setIntraOpNumThreads(1)
            } else {
                setIntraOpNumThreads(threads)
            }
        }
        sitzung = umgebung.createSession(modellDatei.absolutePath, optionen)
        // Der Tokens-Eingang heißt je nach Export "tokens" oder "input_ids" —
        // wie die Referenz wird er als der Nicht-style/speed-Eingang erkannt.
        tokensEingabe = sitzung.inputNames.first { it != "style" && it != "speed" }
    }

    fun synthetisiere(
        phoneme: String,
        vokabular: Map<Char, Long>,
        stimme: FloatArray,
        tempo: Float = 1.0f,
    ): FloatArray {
        val tokens = phoneme.mapNotNull { vokabular[it] }
        // Regel 2 gilt auch hier: Phonemregeln laufen NACH dem Frontend-Filter,
        // ihre Ersatzlaute erreichen den Synthesizer ungefiltert — ein Zeichen
        // außerhalb des Vokabulars fiele sonst STILL unter den Tisch.
        if (tokens.size < phoneme.length) {
            val verloren = phoneme.filter { it !in vokabular }
            if (verloren.isNotBlank()) {
                android.util.Log.w("KokolySynth",
                    "Nicht im Vokabular, verworfen: »$verloren« in »$phoneme«")
            }
        }
        require(tokens.isNotEmpty()) { "kein Phonem im Vokabular: $phoneme" }
        require(tokens.size <= FENSTER - 2) { "${tokens.size} Token > Modellfenster" }
        require(stimme.size == FENSTER * STILBREITE) {
            "Stilvektor hat ${stimme.size} Werte, erwartet ${FENSTER * STILBREITE}"
        }

        val eingabe = LongArray(tokens.size + 2)
        tokens.forEachIndexed { i, t -> eingabe[i + 1] = t }

        val stilzeile = FloatArray(STILBREITE)
        val zeile = minOf(tokens.size, FENSTER) - 1
        System.arraycopy(stimme, zeile * STILBREITE, stilzeile, 0, STILBREITE)

        laufSperre.read {
            check(!geschlossen) { "Session bereits geschlossen" }
            OnnxTensor.createTensor(umgebung, LongBuffer.wrap(eingabe),
                longArrayOf(1, eingabe.size.toLong())).use { tTokens ->
            OnnxTensor.createTensor(umgebung, FloatBuffer.wrap(stilzeile),
                longArrayOf(1, STILBREITE.toLong())).use { tStil ->
            OnnxTensor.createTensor(umgebung, FloatBuffer.wrap(floatArrayOf(tempo)),
                longArrayOf(1)).use { tTempo ->
                sitzung.run(mapOf(
                    tokensEingabe to tTokens, "style" to tStil, "speed" to tTempo,
                ), laufOptionen).use { ergebnis ->
                    @Suppress("UNCHECKED_CAST")
                    val audio = ergebnis[0].value
                    return when (audio) {
                        is Array<*> -> (audio as Array<FloatArray>)[0]
                        is FloatArray -> audio
                        else -> error("Unerwarteter Ausgabetyp: ${audio!!::class}")
                    }
                }
            }}}
        }
    }

    /** Bricht den laufenden Modell-Run ab (onStop; Aufgabe 1.2 des Plans). */
    fun brichAb() {
        runCatching { laufOptionen.setTerminate(true) }
    }

    /** Hebt einen früheren Abbruch auf — einmal je Sprechauftrag. */
    fun loescheAbbruch() {
        runCatching { laufOptionen.setTerminate(false) }
    }

    fun schliesse() {
        brichAb() // ein laufender Run steigt in Millisekunden aus
        laufSperre.write {
            if (geschlossen) return
            geschlossen = true
            laufOptionen.close()
            sitzung.close()
        }
    }
}
