// SPDX-License-Identifier: GPL-3.0-or-later
package de.tilly.kokoly.tts.model

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.util.Log
import java.io.File

/**
 * Einmalige on-device-Wandlung .onnx → .ort (M2b, ADR-0005/0008).
 *
 * Das .ort-Format ist der voroptimierte Flatbuffer der Laufzeit: Die
 * Graphoptimierung läuft EINMAL hier statt bei jedem Session-Aufbau — im
 * Nova-Projekt halbierte das die Ladezeit. Die Wandlung ist der energie-
 * intensivste Einzelmoment der App und läuft deshalb genau einmal je
 * Modelldatei, direkt nach dem Download.
 */
object OrtWandlung {

    private const val TAG = "KokolyOrt"

    /** Liefert die .ort-Fassung, wandelt bei Bedarf einmalig. */
    fun sichere(onnx: File): File {
        val ort = File(onnx.parentFile, onnx.name.removeSuffix(".onnx") + ".ort")
        if (ort.exists()) return ort

        val t0 = System.nanoTime()
        val optionen = OrtSession.SessionOptions().apply {
            setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            addConfigEntry("session.save_model_format", "ORT")
            setOptimizedModelFilePath(ort.absolutePath)
        }
        // Die Wegwerf-Session existiert nur, damit die Laufzeit den optimierten
        // Graphen herausschreibt.
        OrtEnvironment.getEnvironment().createSession(onnx.absolutePath, optionen).close()
        Log.i(TAG, "%s → .ort in %.1f s (%d MB)".format(
            onnx.name, (System.nanoTime() - t0) / 1e9, ort.length() shr 20))
        return ort
    }
}
