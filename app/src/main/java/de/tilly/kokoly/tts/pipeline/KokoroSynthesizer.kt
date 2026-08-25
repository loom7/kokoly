// SPDX-License-Identifier: GPL-3.0-or-later
package de.tilly.kokoly.tts.pipeline

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.File
import java.nio.FloatBuffer
import java.nio.LongBuffer

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

        OnnxTensor.createTensor(umgebung, LongBuffer.wrap(eingabe),
            longArrayOf(1, eingabe.size.toLong())).use { tTokens ->
        OnnxTensor.createTensor(umgebung, FloatBuffer.wrap(stilzeile),
            longArrayOf(1, STILBREITE.toLong())).use { tStil ->
        OnnxTensor.createTensor(umgebung, FloatBuffer.wrap(floatArrayOf(tempo)),
            longArrayOf(1)).use { tTempo ->
            sitzung.run(mapOf(
                tokensEingabe to tTokens, "style" to tStil, "speed" to tTempo,
            )).use { ergebnis ->
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

    fun schliesse() = sitzung.close()
}
