// SPDX-License-Identifier: GPL-3.0-or-later
package de.tilly.kokoly.tts.service

import android.media.AudioFormat
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.speech.tts.Voice
import android.util.Log
import de.tilly.kokoly.tts.pipeline.EnginePipeline
import de.tilly.kokoly.tts.pipeline.EspeakNative
import de.tilly.kokoly.tts.pipeline.KokoroSynthesizer
import de.tilly.kokoly.tts.pipeline.Tonhoehe

/**
 * Der TTS-Dienst — seit M4 mehrsprachig.
 *
 * Vertragspunkte (docs/recherche/feld1-tts-api.md): onSynthesizeText läuft
 * blockierend auf genau EINEM Synthese-Thread; onStop kommt von einem anderen
 * Thread — deshalb das volatile-Abbruchflag, geprüft je Audio-Block.
 * audioAvailable-Blöcke bleiben unter getMaxBufferSize(). Sprachcodes kommen
 * vom Framework als ISO-3 an („deu").
 *
 * Stimmwahl: setVoice() einer fremden App landet als request.voiceName; ohne
 * Stimme entscheidet die Sprach-Vorgabestimme. getSpeechRate() wird Tempo,
 * getPitch() wirkt seit dem 25.08.2026 (F2 revidiert) über tempokompensiertes
 * Umtasten — Verfahren und Grenze in Tonhoehe.kt.
 */
class KokolyTtsService : TextToSpeechService() {

    private companion object { const val TAG = "KokolyTts" }

    @Volatile private var gestoppt = false

    override fun onCreate() {
        super.onCreate()
        // espeak + Vokabular sind leicht und bleiben resident; die schwere
        // ORT-Session lädt erst der erste Satz (und stirbt mit onDestroy).
        runCatching { EnginePipeline.starte(this) }
            .onFailure { Log.e(TAG, "Pipeline-Start fehlgeschlagen", it) }
    }

    override fun onDestroy() {
        EnginePipeline.entladeModell()
        EspeakNative.terminate()
        super.onDestroy()
    }

    override fun onIsLanguageAvailable(lang: String?, country: String?, variant: String?): Int =
        VoiceRegistry.sprachverfuegbarkeit(this, lang, country)

    override fun onGetLanguage(): Array<String> = arrayOf("deu", "DEU", "")

    override fun onLoadLanguage(lang: String?, country: String?, variant: String?): Int {
        val rc = onIsLanguageAvailable(lang, country, variant)
        if (rc != TextToSpeech.LANG_NOT_SUPPORTED) {
            runCatching { EnginePipeline.starte(this) }
        }
        return rc
    }

    override fun onGetVoices(): MutableList<Voice> =
        VoiceRegistry.stimmen(this).toMutableList()

    override fun onIsValidVoiceName(name: String?): Int =
        if (VoiceRegistry.aufloesen(this, name) != null) TextToSpeech.SUCCESS
        else TextToSpeech.ERROR

    override fun onLoadVoice(name: String?): Int = onIsValidVoiceName(name)

    override fun onGetDefaultVoiceNameFor(lang: String?, country: String?, variant: String?): String? =
        VoiceRegistry.vorgabeStimme(this, lang, country)

    override fun onStop() {
        gestoppt = true
    }

    override fun onSynthesizeText(request: SynthesisRequest, callback: SynthesisCallback) {
        gestoppt = false
        val text = request.charSequenceText?.toString().orEmpty()
        if (text.isBlank()) { callback.done(); return }

        // Stimme der App, sonst Vorgabestimme der verlangten Sprache.
        val aufgeloest = VoiceRegistry.aufloesen(this, request.voiceName)
            ?: VoiceRegistry.vorgabeStimme(this, request.language, request.country)
                ?.let { VoiceRegistry.aufloesen(this, it) }
        if (aufgeloest == null) { callback.error(); return }
        val (sprache, stimme) = aufgeloest

        // 100 = Normalwert der API für Tempo UND Tonhöhe. Tonhöhe seit dem
        // Nutzer-Entscheid vom 25.08.2026 (revidiert F2): tempokompensiertes
        // Umtasten — das Modell synthetisiert um p langsamer, gelesen wird um
        // p schneller (Tonhoehe.kt, Formant-Vorbehalt dort).
        val tempo = (request.speechRate / 100f).coerceIn(0.5f, 2.0f)
        val tonFaktor = (request.pitch / 100f)
            .coerceIn(Tonhoehe.MIN_FAKTOR, Tonhoehe.MAX_FAKTOR)
        val modellTempo = (tempo / tonFaktor).coerceIn(0.5f, 2.0f)

        callback.start(KokoroSynthesizer.ABTASTRATE, AudioFormat.ENCODING_PCM_16BIT, 1)
        val maxBlock = callback.maxBufferSize

        runCatching {
            EnginePipeline.synthetisiere(this, text, sprache, stimme, modellTempo) { audio ->
                liefereAlsPcm16(Tonhoehe.umtasten(audio, tonFaktor), maxBlock, callback)
            }
        }.onFailure {
            Log.e(TAG, "Synthese fehlgeschlagen", it)
            callback.error()
            return
        }
        // Auch nach Stop schließt done() den Vorgang ordentlich ab — das
        // Framework hat den Abbruch selbst angestoßen und wartet nur noch.
        callback.done()
    }

    /** float32 → PCM16-Blöcke ≤ maxBufferSize; false bei Stop oder Kanalfehler. */
    private fun liefereAlsPcm16(
        audio: FloatArray,
        maxBlock: Int,
        callback: SynthesisCallback,
    ): Boolean {
        val bytes = ByteArray(audio.size * 2)
        for (i in audio.indices) {
            val s = (audio[i].coerceIn(-1f, 1f) * 32767f).toInt()
            bytes[2 * i] = (s and 0xFF).toByte()
            bytes[2 * i + 1] = ((s shr 8) and 0xFF).toByte()
        }
        var ab = 0
        while (ab < bytes.size) {
            if (gestoppt) return false
            val laenge = minOf(maxBlock, bytes.size - ab)
            if (callback.audioAvailable(bytes, ab, laenge) != TextToSpeech.SUCCESS) return false
            ab += laenge
        }
        return true
    }
}
