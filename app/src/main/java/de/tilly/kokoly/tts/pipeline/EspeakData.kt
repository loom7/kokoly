// SPDX-License-Identifier: GPL-3.0-or-later
package de.tilly.kokoly.tts.pipeline

import android.content.Context
import java.io.File

/**
 * Kopiert espeak-ng-data aus den Assets nach filesDir — espeak liest nur vom
 * Dateisystem. Muster aus dem Nova-Projekt (EspeakData.ensure): eine
 * Versionsmarke macht die Kopie einmalig; ihr Wert ist der espeak-Pin, damit
 * ein espeak-Update die Daten automatisch erneuert.
 */
object EspeakData {

    private const val VERSION = "espeak-ng-1.52.0"

    fun ensure(context: Context): File {
        val ziel = File(context.filesDir, "espeak-ng-data")
        val marke = File(ziel, ".version")
        if (marke.takeIf { it.exists() }?.readText() == VERSION) return ziel

        ziel.deleteRecursively()
        kopiere(context, "espeak-ng-data", ziel)
        marke.writeText(VERSION)
        return ziel
    }

    private fun kopiere(context: Context, assetPfad: String, ziel: File) {
        val kinder = context.assets.list(assetPfad).orEmpty()
        if (kinder.isEmpty()) {
            ziel.parentFile?.mkdirs()
            context.assets.open(assetPfad).use { quelle ->
                ziel.outputStream().use { quelle.copyTo(it) }
            }
            return
        }
        ziel.mkdirs()
        for (kind in kinder) kopiere(context, "$assetPfad/$kind", File(ziel, kind))
    }
}
