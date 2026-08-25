// SPDX-License-Identifier: GPL-3.0-or-later
package de.tilly.kokoly.tts.service

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech

/** Meldet dem Framework die Stimmdatenlage je Sprache (M4: alle sichtbaren). */
class CheckVoiceData : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sichtbar = VoiceRegistry.sichtbareSprachen(this).map { it.iso3 }
        val fehlend = Sprachen.ALLE.map { it.iso3 } - sichtbar.toSet()
        val ergebnis = Intent().apply {
            putStringArrayListExtra(TextToSpeech.Engine.EXTRA_AVAILABLE_VOICES, ArrayList(sichtbar))
            putStringArrayListExtra(TextToSpeech.Engine.EXTRA_UNAVAILABLE_VOICES, ArrayList(fehlend))
        }
        setResult(
            if (sichtbar.isNotEmpty()) TextToSpeech.Engine.CHECK_VOICE_DATA_PASS
            else TextToSpeech.Engine.CHECK_VOICE_DATA_FAIL,
            ergebnis,
        )
        finish()
    }
}
