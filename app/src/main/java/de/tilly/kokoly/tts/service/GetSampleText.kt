// SPDX-License-Identifier: GPL-3.0-or-later
package de.tilly.kokoly.tts.service

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech

/** Liefert den Beispielsatz der Systemeinstellungen. Sprachfächerung kommt in M4. */
class GetSampleText : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ergebnis = Intent().putExtra(
            TextToSpeech.Engine.EXTRA_SAMPLE_TEXT,
            "Der Termin ist am dritten August um halb zehn.",
        )
        setResult(TextToSpeech.Engine.CHECK_VOICE_DATA_PASS, ergebnis)
        finish()
    }
}
