// SPDX-License-Identifier: GPL-3.0-or-later
package de.tilly.kokoly.tts.pipeline

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Schreibt float32-Audio als WAV (PCM16, mono) — für Prüfstein und Messläufe. */
object Wav {

    fun schreibe(datei: File, audio: FloatArray, rate: Int) {
        val pcm = ShortArray(audio.size) {
            (audio[it].coerceIn(-1f, 1f) * 32767f).toInt().toShort()
        }
        val datenBytes = pcm.size * 2
        val puffer = ByteBuffer.allocate(44 + datenBytes).order(ByteOrder.LITTLE_ENDIAN)
        puffer.put("RIFF".toByteArray()).putInt(36 + datenBytes).put("WAVE".toByteArray())
        puffer.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
        puffer.putInt(rate).putInt(rate * 2).putShort(2).putShort(16)
        puffer.put("data".toByteArray()).putInt(datenBytes)
        for (s in pcm) puffer.putShort(s)
        datei.writeBytes(puffer.array())
    }
}
