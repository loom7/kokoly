// SPDX-License-Identifier: GPL-3.0-or-later
package de.tilly.kokoly.tts.service

import android.app.Activity
import android.os.Bundle
import android.speech.tts.TextToSpeech

/** M0-Stub: meldet „keine Stimmen" — gefüllt wird das in M4 aus der VoiceRegistry. */
class CheckVoiceData : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(TextToSpeech.Engine.CHECK_VOICE_DATA_FAIL)
        finish()
    }
}
