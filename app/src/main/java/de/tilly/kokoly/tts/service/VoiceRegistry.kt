// SPDX-License-Identifier: GPL-3.0-or-later
package de.tilly.kokoly.tts.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import de.tilly.kokoly.tts.pipeline.EnginePipeline
import de.tilly.kokoly.tts.settings.Einstellungen

/**
 * Das Stimmenverzeichnis — die Sicht des Frameworks auf [Sprachen] (M4).
 *
 * Sichtbar ist eine Stimme genau dann, wenn ihre Sprache AKTIV ist
 * (Einstellungen, Ziel Z4) und die Modellgruppe VORHANDEN (Download bzw.
 * Entwicklungsweg). Abgewählte Sprachen existieren für fremde Apps nicht —
 * weder in onGetVoices noch in onIsLanguageAvailable.
 */
object VoiceRegistry {

    fun sichtbareSprachen(context: Context): List<Sprachen.Sprache> =
        Sprachen.ALLE.filter {
            Einstellungen.istAktiv(context, it.espeak) &&
                EnginePipeline.gruppeVorhanden(context, it.gruppe)
        }

    fun stimmen(context: Context): List<Voice> =
        sichtbareSprachen(context).flatMap { sprache ->
            sprache.stimmen.map { stimme ->
                Voice(
                    Sprachen.stimmName(sprache, stimme), sprache.locale,
                    Voice.QUALITY_HIGH, Voice.LATENCY_NORMAL,
                    /* requiresNetwork = */ false, emptySet(),
                )
            }
        }

    fun sprachverfuegbarkeit(context: Context, lang: String?, country: String?): Int {
        val sprache = Sprachen.nachLocale(lang, country) ?: return TextToSpeech.LANG_NOT_SUPPORTED
        if (sprache !in sichtbareSprachen(context)) return TextToSpeech.LANG_NOT_SUPPORTED
        val landPasst = !country.isNullOrBlank() &&
            (sprache.locale.isO3Country.equals(country, true) ||
                sprache.locale.country.equals(country, true))
        return if (landPasst) TextToSpeech.LANG_COUNTRY_AVAILABLE
        else TextToSpeech.LANG_AVAILABLE
    }

    fun vorgabeStimme(context: Context, lang: String?, country: String?): String? {
        val sprache = Sprachen.nachLocale(lang, country) ?: return null
        if (sprache !in sichtbareSprachen(context)) return null
        return Sprachen.stimmName(sprache, sprache.stimmen.first())
    }

    /** Auflösung eines Stimmnamens — nur sichtbare Stimmen gelten. */
    fun aufloesen(context: Context, stimmName: String?): Pair<Sprachen.Sprache, String>? {
        if (stimmName.isNullOrBlank()) return null
        val (sprache, stimme) = Sprachen.aufloesen(stimmName) ?: return null
        return if (sprache in sichtbareSprachen(context)) sprache to stimme else null
    }
}
