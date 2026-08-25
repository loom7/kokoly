// SPDX-License-Identifier: GPL-3.0-or-later
package de.tilly.kokoly.tts.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import de.tilly.kokoly.tts.pipeline.EnginePipeline
import java.util.Locale

/**
 * Das Stimmenverzeichnis — M1: genau eine Stimme, de-DE-martin.
 *
 * Die Stimmnamen sind öffentliche API (ADR-0010): einmal veröffentlicht,
 * wandern sie nie wieder. Muster: `<sprache>-<REGION>-<stimmname>`.
 * M4 erweitert auf die v1.0-Stimmen und die An-/Abwahl in den Einstellungen.
 */
object VoiceRegistry {

    const val MARTIN = "de-DE-martin"

    fun stimmen(context: Context): List<Voice> {
        if (!EnginePipeline.modellVorhanden(context)) return emptyList()
        return listOf(
            Voice(
                MARTIN, Locale.GERMANY, Voice.QUALITY_HIGH,
                Voice.LATENCY_NORMAL, /* requiresNetwork = */ false, emptySet(),
            )
        )
    }

    /** ISO-3-Prüfung, wie das Framework fragt („deu"); ISO-2 defensiv dazu. */
    fun sprachverfuegbarkeit(context: Context, lang: String?, country: String?): Int {
        if (!EnginePipeline.modellVorhanden(context)) return TextToSpeech.LANG_NOT_SUPPORTED
        return when (lang?.lowercase()) {
            "deu", "de" -> when (country?.uppercase()) {
                "DEU", "DE" -> TextToSpeech.LANG_COUNTRY_AVAILABLE
                else -> TextToSpeech.LANG_AVAILABLE
            }
            else -> TextToSpeech.LANG_NOT_SUPPORTED
        }
    }
}
