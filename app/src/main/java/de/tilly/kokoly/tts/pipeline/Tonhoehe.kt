// SPDX-License-Identifier: GPL-3.0-or-later
package de.tilly.kokoly.tts.pipeline

/**
 * Tonhöhe über tempokompensiertes Umtasten (Stufe-1-Verfahren).
 *
 * Der Kokoro-Graph hat keinen Tonhöheneingang. Das Verfahren hier nutzt den
 * speed-Eingang des Modells als Gegenstück zum Umtasten: für Faktor p wird um
 * p LANGSAMER synthetisiert (Dauer ×p) und anschließend um p schneller
 * gelesen (Dauer ÷p, Tonhöhe ×p) — netto bleibt die Dauer, die Tonhöhe wandert.
 *
 * **Ehrliche Grenze:** Umtasten verschiebt auch die Formanten — bei kräftigen
 * Werten klingt der Sprecher „kleiner“ bzw. „größer“ (Micky-Maus-Effekt), bei
 * moderaten Reglerstellungen ist es unauffällig. Die formanterhaltende
 * TD-PSOLA-Fassung (wie die Windows-Referenz über Praat, hier als
 * Eigenimplementierung) bleibt als Stufe-2-Verbesserung im Plan.
 */
object Tonhoehe {

    const val MIN_FAKTOR = 0.5f
    const val MAX_FAKTOR = 2.0f

    /** Liest [audio] um [faktor] schneller (linear interpoliert): Tonhöhe ×faktor. */
    fun umtasten(audio: FloatArray, faktor: Float): FloatArray {
        if (faktor in 0.999f..1.001f || audio.isEmpty()) return audio
        val laenge = (audio.size / faktor).toInt().coerceAtLeast(1)
        val ergebnis = FloatArray(laenge)
        for (i in 0 until laenge) {
            val quelle = i * faktor
            val links = quelle.toInt().coerceAtMost(audio.size - 1)
            val rechts = (links + 1).coerceAtMost(audio.size - 1)
            val anteil = quelle - links
            ergebnis[i] = audio[links] * (1 - anteil) + audio[rechts] * anteil
        }
        return ergebnis
    }
}
