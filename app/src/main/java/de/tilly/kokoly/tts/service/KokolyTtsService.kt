// SPDX-License-Identifier: GPL-3.0-or-later
package de.tilly.kokoly.tts.service

import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeechService

/**
 * Der TTS-Dienst — M0-Stub.
 *
 * In M0 wird die Pipeline OHNE diese Schicht bewiesen (Phonemgleichheit und
 * ich-Laut-Hörprobe laufen als nackte Gerätetests, WAV per adb pull). Erst M1
 * verdrahtet sie hierher. Bis dahin meldet der Stub ehrlich: nichts verfügbar —
 * er erscheint damit zwar in den Systemeinstellungen, bietet aber keine Sprache
 * an, statt kaputtes Audio zu liefern.
 */
class KokolyTtsService : TextToSpeechService() {

    override fun onIsLanguageAvailable(lang: String?, country: String?, variant: String?): Int =
        android.speech.tts.TextToSpeech.LANG_NOT_SUPPORTED

    override fun onGetLanguage(): Array<String> = arrayOf("deu", "DEU", "")

    override fun onLoadLanguage(lang: String?, country: String?, variant: String?): Int =
        android.speech.tts.TextToSpeech.LANG_NOT_SUPPORTED

    override fun onStop() = Unit

    override fun onSynthesizeText(request: SynthesisRequest, callback: SynthesisCallback) {
        // M0: keine Synthese über den Dienstweg. error() statt stummem done(),
        // damit ein versehentlicher Aufruf sichtbar scheitert.
        callback.error()
    }
}
