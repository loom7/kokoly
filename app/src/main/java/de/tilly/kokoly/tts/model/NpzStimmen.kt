// SPDX-License-Identifier: GPL-3.0-or-later
package de.tilly.kokoly.tts.model

import java.io.DataInputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipFile

/**
 * Liest Kokoro-Stimmbänke direkt aus dem npz-Format (`voices-v1.0.bin` und
 * `voices-martin.npz` sind beide npz: ein Zip aus npy-Einträgen).
 *
 * Warum selbst lesen statt Hilfsdateien: Die Stimmvektoren kommen so aus
 * derselben Datei, die auch die Referenz benutzt — kein Export-Zwischenschritt,
 * der Fehler einführen oder veralten kann, und ein Download weniger.
 *
 * Gelesen wird NUR das nötige Format: npy Version 1.x, dtype `<f4`,
 * C-Reihenfolge, Form (510, 1, 256). Alles andere ist ein Fehler, kein
 * Ratefall — die Stimmbank ist Teil des gepinnten Modellstands.
 */
class NpzStimmen(private val datei: File) {

    companion object {
        const val ZEILEN = 510
        const val BREITE = 256
        private val MAGIC = byteArrayOf(0x93.toByte(), 'N'.code.toByte(),
            'U'.code.toByte(), 'M'.code.toByte(), 'P'.code.toByte(), 'Y'.code.toByte())
    }

    /** Die Namen aller Stimmen in der Bank. */
    fun stimmen(): List<String> = ZipFile(datei).use { zip ->
        zip.entries().asSequence()
            .map { it.name.removeSuffix(".npy") }
            .sorted().toList()
    }

    /**
     * Lädt den Stilvektor einer Stimme als flaches FloatArray (510 × 256).
     * Aufrufer cachen selbst (522 KB je Stimme) — die Bank bleibt auf Platte.
     */
    fun vektor(stimme: String): FloatArray = ZipFile(datei).use { zip ->
        val eintrag = zip.getEntry("$stimme.npy")
            ?: error("Stimme $stimme nicht in ${datei.name} (vorhanden: ${stimmen().take(5)}…)")
        DataInputStream(zip.getInputStream(eintrag).buffered()).use { ein ->
            val magic = ByteArray(6).also { ein.readFully(it) }
            check(magic.contentEquals(MAGIC)) { "kein npy-Eintrag: $stimme" }
            val major = ein.readUnsignedByte()
            ein.readUnsignedByte() // minor
            val kopfLaenge = if (major == 1) {
                ein.readUnsignedByte() or (ein.readUnsignedByte() shl 8)
            } else {
                ein.readUnsignedByte() or (ein.readUnsignedByte() shl 8) or
                    (ein.readUnsignedByte() shl 16) or (ein.readUnsignedByte() shl 24)
            }
            val kopf = ByteArray(kopfLaenge).also { ein.readFully(it) }
                .toString(Charsets.ISO_8859_1)
            check("'<f4'" in kopf) { "unerwarteter dtype in $stimme: $kopf" }
            check("'fortran_order': False" in kopf) { "Fortran-Reihenfolge in $stimme" }
            check("($ZEILEN, 1, $BREITE)" in kopf) { "unerwartete Form in $stimme: $kopf" }

            val bytes = ByteArray(ZEILEN * BREITE * 4).also { ein.readFully(it) }
            val puffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
            FloatArray(puffer.remaining()).also { puffer.get(it) }
        }
    }
}
