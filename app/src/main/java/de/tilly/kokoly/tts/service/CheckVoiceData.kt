// SPDX-License-Identifier: GPL-3.0-or-later
package de.tilly.kokoly.tts.service

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import de.tilly.kokoly.tts.pipeline.EnginePipeline

/** Meldet dem Framework die verfügbaren Stimmdaten (M1: de-DE, wenn das Modell liegt). */
class CheckVoiceData : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val verfuegbar = EnginePipeline.modellVorhanden(this)
        val ergebnis = Intent().apply {
            putStringArrayListExtra(
                TextToSpeech.Engine.EXTRA_AVAILABLE_VOICES,
                if (verfuegbar) arrayListOf("deu-DEU") else arrayListOf(),
            )
            putStringArrayListExtra(
                TextToSpeech.Engine.EXTRA_UNAVAILABLE_VOICES,
                if (verfuegbar) arrayListOf() else arrayListOf("deu-DEU"),
            )
        }
        setResult(
            if (verfuegbar) TextToSpeech.Engine.CHECK_VOICE_DATA_PASS
            else TextToSpeech.Engine.CHECK_VOICE_DATA_FAIL,
            ergebnis,
        )
        finish()
    }
}
