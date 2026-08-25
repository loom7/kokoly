// SPDX-License-Identifier: GPL-3.0-or-later
package de.tilly.kokoly.tts.pipeline

/**
 * Die JNI-Bindung an espeak-ng 1.52.0 (Pin: native/espeak-ng/PIN.md).
 *
 * espeak ist nicht threadsicher — jeder Zugriff läuft über [sperre]. Der
 * native Wrapper hält bewusst keine eigenen Locks (eine Wahrheit, ein Ort).
 *
 * Lebenszyklus (Querschnittsfestlegung in docs/architektur.md, berichtigt
 * 25.08.2026): einmal je Prozess initialisieren, resident bis zum Prozessende.
 * Der Dienst ruft [terminate] NIE — Dienst-onDestroy ist nicht Prozessende
 * (Engine-Wechsel zerstört und rebindet im selben Prozess; ein terminate
 * machte die Engine danach stumm). [terminate] bleibt für Testaufräumen;
 * [init] ist idempotent und heilt eine beendete Bindung.
 */
object EspeakNative {

    private val sperre = Any()
    private var bereit = false
    private var aktiveStimme: String? = null

    /** Sprachcode → espeak-Stimmkennung — dieselbe Tabelle wie der Golden-Generator. */
    val STIMMEN: Map<String, String> = mapOf(
        "de" to "gmw/de", "en-us" to "gmw/en-us", "en-gb" to "gmw/en",
        "es" to "roa/es", "fr-fr" to "roa/fr", "it" to "roa/it",
        "pt-br" to "roa/pt-br", "hi" to "inc/hi",
    )

    fun init(datenPfad: String) {
        synchronized(sperre) {
            if (bereit) return
            System.loadLibrary("kokoly_jni")
            val rate = nativeInit(datenPfad)
            check(rate > 0) { "espeak_Initialize fehlgeschlagen (rc=$rate) — Datenpfad: $datenPfad" }
            bereit = true
        }
    }

    /**
     * Phonemisiert einen Interpunktions-Chunk in der gegebenen Sprache.
     *
     * Rückgabe ist die ROHE espeak-Ausgabe (IPA, '_'-Separatoren, ggf.
     * Sprachwechselmarken) — die Nachverarbeitung gehört [PhonemeFrontend].
     */
    fun phonemisiere(chunk: String, sprache: String): String {
        val kennung = requireNotNull(STIMMEN[sprache]) { "Unbekannte Sprache: $sprache" }
        synchronized(sperre) {
            check(bereit) { "EspeakNative.init() wurde nicht gerufen" }
            if (aktiveStimme != kennung) {
                val rc = nativeSetVoice(kennung)
                check(rc == 0) { "espeak_SetVoiceByName($kennung) fehlgeschlagen (rc=$rc)" }
                aktiveStimme = kennung
            }
            return nativeTextToPhonemes(chunk)
        }
    }

    fun terminate() {
        synchronized(sperre) {
            if (!bereit) return
            nativeTerminate()
            bereit = false
            aktiveStimme = null
        }
    }

    @JvmStatic private external fun nativeInit(dataPath: String): Int
    @JvmStatic private external fun nativeSetVoice(voiceId: String): Int
    @JvmStatic private external fun nativeTextToPhonemes(text: String): String
    @JvmStatic private external fun nativeTerminate()
}
