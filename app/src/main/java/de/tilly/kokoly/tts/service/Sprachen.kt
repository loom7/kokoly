// SPDX-License-Identifier: GPL-3.0-or-later
package de.tilly.kokoly.tts.service

import java.util.Locale

/**
 * Das Sprachverzeichnis — die eine Wahrheit über Sprachen, Stimmen und ihre
 * Modellzugehörigkeit (M4).
 *
 * Stimmnamen sind öffentliche API (ADR-0010): `<sprache>-<REGION>-<stimmname>`,
 * einmal veröffentlicht, wandern sie nie. Die Stimmlisten entsprechen der
 * v1.0-Stimmbank (41 Stimmen über 7 Sprachen) plus dem deutschen Fine-Tune;
 * ja/zh folgen in Stufe 2 (ADR-0003).
 */
object Sprachen {

    /** Welche Modelldateien eine Sprachgruppe braucht — in Vorzugsreihenfolge. */
    data class Modellgruppe(
        val kennung: String,
        /** Modellkandidaten; die erste vorhandene Datei gewinnt. */
        val modelle: List<String>,
        val stimmbank: String,
    )

    // Deutsch: fp32 gesetzt, die Mischfassung fp16dec ist Kandidat — der
    // Entscheid fällt mit der M5-Energiemessung (Nutzerwunsch, 25.08.2026).
    val MARTIN = Modellgruppe(
        "martin",
        listOf("kokoro-martin.onnx"),
        "voices-martin.npz",
    )

    // v1.0: fp16 ist der gemessene Standard (ADR-0015); fp32 bleibt als
    // Rückfallkandidat für Entwicklungsgeräte, auf denen nur er liegt.
    val V10 = Modellgruppe(
        "v10",
        listOf("kokoro-v1.0.fp16.onnx", "kokoro-v1.0.onnx"),
        "voices-v1.0.bin",
    )

    data class Sprache(
        /** espeak-Sprachcode — identisch zur Windows-Referenz und den Goldens. */
        val espeak: String,
        val locale: Locale,
        /** ISO3-Paar für CheckVoiceData („deu-DEU"). */
        val iso3: String,
        val gruppe: Modellgruppe,
        /** Stimmen der v1.0-Bank bzw. des Fine-Tunes; die erste ist die Vorgabe. */
        val stimmen: List<String>,
        val beispielsatz: String,
    )

    val ALLE: List<Sprache> = listOf(
        Sprache("de", Locale.GERMANY, "deu-DEU", MARTIN, listOf("martin"),
            "Guten Tag, hier spricht Martin. Der Termin ist am dritten August um halb zehn."),
        Sprache("en-us", Locale.US, "eng-USA", V10, listOf(
            "af_heart", "af_alloy", "af_aoede", "af_bella", "af_jessica", "af_kore",
            "af_nicole", "af_nova", "af_river", "af_sarah", "af_sky",
            "am_adam", "am_echo", "am_eric", "am_fenrir", "am_liam",
            "am_michael", "am_onyx", "am_puck", "am_santa"),
            "Hello, this is Kokoly speaking through the Android interface."),
        Sprache("en-gb", Locale.UK, "eng-GBR", V10, listOf(
            "bf_emma", "bf_alice", "bf_isabella", "bf_lily",
            "bm_daniel", "bm_fable", "bm_george", "bm_lewis"),
            "Hello, this is Kokoly speaking through the Android interface."),
        Sprache("es", Locale.forLanguageTag("es-ES"), "spa-ESP", V10,
            listOf("ef_dora", "em_alex", "em_santa"),
            "Hola, soy Kokoly hablando a través de la interfaz de Android."),
        Sprache("fr-fr", Locale.FRANCE, "fra-FRA", V10, listOf("ff_siwis"),
            "Bonjour, ici Kokoly qui parle par l'interface Android."),
        Sprache("it", Locale.ITALY, "ita-ITA", V10, listOf("if_sara", "im_nicola"),
            "Ciao, sono Kokoly e parlo attraverso l'interfaccia Android."),
        Sprache("pt-br", Locale.forLanguageTag("pt-BR"), "por-BRA", V10,
            listOf("pf_dora", "pm_alex", "pm_santa"),
            "Olá, aqui é Kokoly falando pela interface do Android."),
        Sprache("hi", Locale.forLanguageTag("hi-IN"), "hin-IND", V10,
            listOf("hf_alpha", "hf_beta", "hm_omega", "hm_psi"),
            "नमस्ते, मैं कोकोली हूँ।"),
    )

    /** Der öffentliche Stimmname: de-DE-martin, en-US-af_heart … (ADR-0010). */
    fun stimmName(sprache: Sprache, stimme: String): String =
        "${sprache.locale.language}-${sprache.locale.country}-$stimme"

    /** Auflösung eines Stimmnamens zurück auf (Sprache, Bank-Stimme). */
    fun aufloesen(stimmName: String): Pair<Sprache, String>? {
        val teile = stimmName.split("-", limit = 3)
        if (teile.size != 3) return null
        val sprache = ALLE.firstOrNull {
            it.locale.language == teile[0] && it.locale.country == teile[1]
        } ?: return null
        return if (teile[2] in sprache.stimmen) sprache to teile[2] else null
    }

    /**
     * Sprachauflösung wie das Framework fragt: ISO3 („deu"), defensiv auch
     * ISO2. Ohne Land gewinnt die erste passende Sprache der Liste.
     */
    fun nachLocale(lang: String?, country: String?): Sprache? {
        if (lang.isNullOrBlank()) return null
        val l = lang.lowercase()
        val c = country?.uppercase().orEmpty()
        val kandidaten = ALLE.filter {
            it.locale.isO3Language.equals(l, true) || it.locale.language.equals(l, true)
        }
        if (kandidaten.isEmpty()) return null
        return kandidaten.firstOrNull {
            c.isNotEmpty() && (it.locale.isO3Country.equals(c, true) ||
                it.locale.country.equals(c, true))
        } ?: kandidaten.first()
    }
}
