// SPDX-License-Identifier: GPL-3.0-or-later
package de.tilly.kokoly.tts.pipeline

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.File
import java.nio.FloatBuffer
import java.nio.LongBuffer

/**
 * Die nackte Kokoro-Inferenz — M0-Fassung für den Prüfstein.
 *
 * Eingabeaufbau exakt wie die Referenz (kokoro_onnx.Kokoro._infer):
 * Tokens [[0, …, 0]] int64, Stilzeile voice[min(n, 510) - 1] als [1, 256],
 * speed float32[1]. Ausgabe: float32-Audio, 24 kHz mono.
 *
 * Bewusst NICHT hier: Stückelung über 510 Token, Pausensteuerung, continuous
 * (alles M1+, nach den Mustern der Windows-Referenz). Der Prüfstein spricht
 * Einzelsätze.
 */
class KokoroSynthesizer(
    modellDatei: File,
    stimmDatei: File,
    threads: Int = 4,
    /** XNNPACK-Execution-Provider statt CPU-EP — Messgröße der M2a-Matrix. */
    xnnpack: Boolean = false,
) {

    companion object {
        const val ABTASTRATE = 24000
        const val FENSTER = 510
        const val STILBREITE = 256
    }

    private val umgebung = OrtEnvironment.getEnvironment()
    private val sitzung: OrtSession
    private val stimme: FloatArray
    private val tokensEingabe: String

    init {
        val optionen = OrtSession.SessionOptions().apply {
            if (xnnpack) {
                // XNNPACK hat seinen eigenen Threadpool; die ORT-Threads bleiben
                // dann auf 1, sonst drehen zwei Pools gegeneinander (Messfalle
                // aus der Recherche, Kontrollpunkt in 6.4 des Plans).
                addXnnpack(mapOf("intra_op_num_threads" to threads.toString()))
                setIntraOpNumThreads(1)
            } else {
                setIntraOpNumThreads(threads)
            }
        }
        sitzung = umgebung.createSession(modellDatei.absolutePath, optionen)
        stimme = stimmDatei.readBytes().let { bytes ->
            check(bytes.size == FENSTER * STILBREITE * 4) {
                "Stimmdatei hat ${bytes.size} Bytes, erwartet ${FENSTER * STILBREITE * 4}"
            }
            val puffer = java.nio.ByteBuffer.wrap(bytes)
                .order(java.nio.ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
            FloatArray(puffer.remaining()).also { puffer.get(it) }
        }
        // Der Tokens-Eingang heißt je nach Export "tokens" oder "input_ids" —
        // wie die Referenz wird er als der Nicht-style/speed-Eingang erkannt.
        tokensEingabe = sitzung.inputNames.first { it != "style" && it != "speed" }
    }

    fun synthetisiere(phoneme: String, vokabular: Map<Char, Long>, tempo: Float = 1.0f): FloatArray {
        val tokens = phoneme.mapNotNull { vokabular[it] }
        require(tokens.isNotEmpty()) { "kein Phonem im Vokabular: $phoneme" }
        require(tokens.size <= FENSTER - 2) { "${tokens.size} Token > Fenster — Stückelung ist M1" }

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
