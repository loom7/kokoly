// SPDX-License-Identifier: GPL-3.0-or-later
package de.tilly.kokoly.tts.rules.de

/**
 * Phonemregeln — Eingriffe zwischen Phonemisierer und Modell. 1:1-Port von
 * `TTS Test/betonung.py` und `TTS Test/wortlaute.py` (eingefrorene Referenz,
 * ADR-0013), golden-geprüft über golden/de-regeln.json.
 *
 * **Warum die Entscheidung am TEXT fällt:** Auf der Phonemebene ist der
 * Zusammenhang verloren — Monat und Vorname sind beide `ˈaʊɡʊst`. Ob eine
 * Regel greifen darf, prüft ihr Kontextmuster am Text vor der Fundstelle.
 *
 * **Die Zählprobe als Sicherung:** Findet sich das Phonemmuster nicht genau so
 * oft wie das Wort im Text, wird die Regel ÜBERSPRUNGEN („Die Regie der
 * Regierung": `reːʒˈiː` steckt auch in `reːɡˈiːrʊŋ` — nicht zuzuordnen, also
 * nichts tun). Ein Eingriff an der falschen Stelle wäre schlimmer als der
 * bekannte Fehler.
 *
 * **Betonungszeichen wirken nur unmittelbar VOR dem Vokal** (gemessen: ΔK je
 * Regel in der Referenz; `aʊˈɡʊst` wäre wirkungslos, `aʊɡˈʊst` wirkt).
 */
object Phonemregeln {

    private const val W = """[\p{L}\p{N}_]"""

    data class Regel(
        val wort: String,
        val falsch: String,
        val richtig: String,
        val kontext: Regex?,
        val erklaerung: String,
    ) {
        fun giltFuer(text: String, position: Int): Boolean {
            val muster = kontext ?: return true
            val davor = text.substring(maxOf(0, position - 40), position)
            return muster.containsMatchIn(davor)
        }
    }

    // ------------------------------------------------------------- Betonung

    private const val SIGNALWOERTER = "im|seit|bis|ab|vom|zum|nach|am|ende|anfang|mitte"
    private const val ORDINAL = "$W+(?:ten|sten)"
    private const val ZIFFERNDATUM = """\d{1,2}\."""

    /** Nur mit Monats-Signal davor gilt „August" als Monat, nie als Vorname. */
    private val MONATSKONTEXT = Regex(
        """(?:(?<!$W)(?:$SIGNALWOERTER)(?!$W)|(?<!$W)$ORDINAL(?!$W)|$ZIFFERNDATUM)\s+$""",
        RegexOption.IGNORE_CASE,
    )

    private fun fest(wort: String, falsch: String, richtig: String, dK: String, art: String) =
        Regel(wort, falsch, richtig, null, "$art, ΔK $dK")

    val BETONUNG: List<Regel> = listOf(
        Regel(
            "August", "ˈaʊɡʊst", "aʊɡˈʊst", MONATSKONTEXT,
            "Monat Augúst statt Vorname Áugust",
        ),
        // Keine Homographen — Betonung ist unabhängig vom Satz falsch.
        fest("Jahrzehnt", "jˈɑːɾtseːnt", "jɑːɾtsˈeːnt", "+0,63", "Zeitwort"),
        fest("Osnabrück", "ˈɔsnabɾˌyk", "ɔsnabɾˈyk", "+0,56", "Ortsname"),
        fest("Büro", "bˈyːroː", "byːrˈoː", "+0,44", "Fremdwort"),
        fest("Hotel", "hˈoːtəl", "hoːtˈəl", "+0,34", "Fremdwort"),
        fest("Kaffeemaschine", "kˌafeːmaʃˈiːnə", "kˈafeːmaʃˌiːnə", "+0,34", "Kompositum"),
        fest("Studentenwohnheim", "ʃtˈuːdəntˌɛnvoːnhˌaɪm", "ʃtuːdˈəntɛnvoːnhaɪm", "+0,30", "Kompositum"),
        fest("Lebensversicherung", "lˌeːbənsfɛɾzˈɪçərˌʊŋ", "lˈeːbənsfɛɾzɪçərʊŋ", "+0,30", "Kompositum"),
        fest("Motoren", "mˈoːtoːrən", "moːtˈoːrən", "+0,25", "Fremdwort"),
        fest("missverstehen", "mˈɪsfɜʃtˌeːən", "mɪsfɜʃtˈeːən", "+0,22", "Präfixverb"),
        fest("widersprechen", "vˈiːdɜʃpɾˌɛçən", "viːdɜʃpɾˈɛçən", "+0,20", "Präfixverb"),
    )

    // ------------------------------------------------------------- Wortlaute

    private fun wort(wort: String, falsch: String, richtig: String, erklaerung: String) =
        Regel(wort, falsch, richtig, null, erklaerung)

    /**
     * espeaks Wörterbuchlücken — segmentale Fehler, alle im Satzrahmen belegt.
     * Reihenfolge tragend: „Martins" steht VOR „Martin", weil `mˈaɾtɪn` ein
     * Anfangsstück von `mˈaɾtɪns` ist und die Zählprobe sonst zu Recht aussteigt.
     */
    val WORTLAUTE: List<Regel> = listOf(
        // Photosynthese VOR Synthese: das Synthese-Muster ist ein Teilstück
        // des Photosynthese-Musters — in dieser Reihenfolge heilen beide auch
        // im selben Satz (die Zählprobe sähe sonst zwei Phonemtreffer bei
        // einem Worttreffer und stiege zu Recht aus).
        wort("Photosynthese", "fˌoːtoːzˈyntəsə", "fˌoːtoːzyntˈeːzə",
            "Schwa statt eː, Betonung auf der Vorsilbe; ΔK-Methode wie Synthese"),
        wort("Synthese", "zˈyntəsə", "zyntˈeːzə",
            "„ZÜN-te-se“: Schwa statt eː, s statt z, Betonung vorn — ΔK +0,20 (Nutzerfund 25.08.2026)"),
        wort("Erbse", "ɛɾbzˈeː", "ˈɛɾpsə", "espeak erfindet „erb-ZEH“"),
        wort("Erbsen", "ˈɛɾpzən", "ˈɛɾpsən", "stimmhaftes z nach p ist unmöglich"),
        wort("Sauce", "zˈaʊkə", "zˈoːsə", "espeak liest buchstabengetreu"),
        wort("Journal", "juːɾnˈɑːl", "ʒʊɾnˈɑːl", "j statt ʒ"),
        wort("Regie", "reːɡˈiː", "reːʒˈiː", "hartes ɡ statt ʒ"),
        wort("Etage", "ˈeːtɑːɡə", "eːtˈɑːʒə", "ɡ statt ʒ, dazu falsche Betonung"),
        wort("Orange", "ˈoːraŋə", "orˈɑ̃ːʒə", "ʒ fehlt ganz, dazu falsche Betonung"),
        wort("Frauchen", "frˈaʊxən", "frˈaʊçən", "-chen hat immer ç, nie x"),
        wort("Martins", "mˈaɾtɪns", "mˈaɾtiːns", "im Deutschen langes i"),
        wort("Martin", "mˈaɾtɪn", "mˈaɾtiːn", "im Deutschen langes i"),
        wort("Tuch", "tˈʊx", "tˈuːx", "kurzes statt langes u"),
        wort("Tuchladen", "tˈʊxlɑːdən", "tˈuːxlɑːdən", "kurzes statt langes u"),
        wort("Handtuch", "hˈanttˌʊx", "hˈanttˌuːx", "kurzes statt langes u"),
        wort("Taschentuch", "tˈaʃəntˌʊx", "tˈaʃəntˌuːx", "kurzes statt langes u"),
        wort("Tischtuch", "tˈɪʃtʊx", "tˈɪʃtuːx", "kurzes statt langes u"),
    )

    // ------------------------------------------------------------- Anwendung

    data class Ergebnis(val phoneme: String, val meldungen: List<String>)

    /** Wendet einen Regelsatz an — der Algorithmus aus betonung.berichtige. */
    fun wende(text: String, phonemeEin: String, regeln: List<Regel>): Ergebnis {
        var phoneme = phonemeEin
        val meldungen = mutableListOf<String>()

        for (regel in regeln) {
            val wortMuster = Regex(
                "(?<!$W)" + Regex.escape(regel.wort) + "(?!$W)",
                RegexOption.IGNORE_CASE,
            )
            val vorkommen = wortMuster.findAll(text)
                .map { regel.giltFuer(text, it.range.first) }.toList()
            if (vorkommen.none { it }) continue

            val stellen = Regex(Regex.escape(regel.falsch))
                .findAll(phoneme).map { it.range.first }.toList()
            if (stellen.size != vorkommen.size) {
                meldungen.add(
                    "${regel.wort}: ${vorkommen.size}× im Text, ${stellen.size}× in den " +
                        "Phonemen — nicht zuzuordnen, unverändert gelassen"
                )
                continue
            }

            // Von hinten ersetzen, damit die früheren Stellen gültig bleiben.
            var geaendert = 0
            for (i in stellen.indices.reversed()) {
                if (!vorkommen[i]) continue
                val stelle = stellen[i]
                phoneme = phoneme.substring(0, stelle) + regel.richtig +
                    phoneme.substring(stelle + regel.falsch.length)
                geaendert++
            }
            if (geaendert > 0) {
                meldungen.add("${regel.wort} $geaendert× berichtigt (${regel.erklaerung})")
            }
        }
        return Ergebnis(phoneme, meldungen)
    }

    /** Die volle Regelstufe: Betonung, dann Wortlaute — Reihenfolge der Referenz. */
    fun berichtige(text: String, phoneme: String): Ergebnis {
        val b = wende(text, phoneme, BETONUNG)
        val w = wende(text, b.phoneme, WORTLAUTE)
        return Ergebnis(w.phoneme, b.meldungen + w.meldungen)
    }
}
