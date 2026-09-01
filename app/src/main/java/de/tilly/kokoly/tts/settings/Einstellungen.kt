// SPDX-License-Identifier: GPL-3.0-or-later
package de.tilly.kokoly.tts.settings

import android.content.Context

/**
 * Die Nutzereinstellungen — M4: Sprachen aktiv an- und abwählbar (Ziel Z4).
 *
 * Abgewählte Sprachen verschwinden aus onGetVoices, onIsLanguageAvailable und
 * CheckVoiceData (RHVoice-Muster) — für fremde Apps existieren sie dann nicht.
 * Vorgabe: alle Sprachen an; wer weniger will, wählt ab.
 */
object Einstellungen {

    private const val NAME = "kokoly"
    private const val SCHLUESSEL_ABGEWAEHLT = "abgewaehlte_sprachen"
    private const val SCHLUESSEL_WILLKOMMEN = "willkommen_erledigt"

    private fun prefs(context: Context) =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    /** Gespeichert werden die ABgewählten — neue Sprachen sind damit automatisch an. */
    fun istAktiv(context: Context, espeakCode: String): Boolean =
        espeakCode !in prefs(context).getStringSet(SCHLUESSEL_ABGEWAEHLT, emptySet())!!

    fun setzeAktiv(context: Context, espeakCode: String, aktiv: Boolean) {
        val alt = prefs(context).getStringSet(SCHLUESSEL_ABGEWAEHLT, emptySet())!!
        val neu = if (aktiv) alt - espeakCode else alt + espeakCode
        prefs(context).edit().putStringSet(SCHLUESSEL_ABGEWAEHLT, neu).apply()
    }

    /** Erstlauf: Willkommensblock (Sprachüberblick + Deutsch-Download-Frage). */
    fun istWillkommenErledigt(context: Context): Boolean =
        prefs(context).getBoolean(SCHLUESSEL_WILLKOMMEN, false)

    fun setzeWillkommenErledigt(context: Context) {
        prefs(context).edit().putBoolean(SCHLUESSEL_WILLKOMMEN, true).apply()
    }
}
