// SPDX-License-Identifier: GPL-3.0-or-later
package de.tilly.kokoly.tts.service

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech

/** Liefert den Beispielsatz der Systemeinstellungen in der verlangten Sprache. */
class GetSampleText : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val lang = intent.getStringExtra("language")
        val country = intent.getStringExtra("country")
        val sprache = Sprachen.nachLocale(lang, country) ?: Sprachen.ALLE.first()
        val ergebnis = Intent().putExtra(
            TextToSpeech.Engine.EXTRA_SAMPLE_TEXT, sprache.beispielsatz)
        setResult(TextToSpeech.Engine.CHECK_VOICE_DATA_PASS, ergebnis)
        finish()
    }
}
