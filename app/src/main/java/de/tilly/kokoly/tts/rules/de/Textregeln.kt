// SPDX-License-Identifier: GPL-3.0-or-later
package de.tilly.kokoly.tts.rules.de

/**
 * Textregeln — Aufbereitung VOR dem Phonemisierer. 1:1-Port von
 * `TTS Test/textregeln.py` (eingefrorene Referenz, ADR-0013); jede Regel dort
 * mit Messbeleg. Golden-geprüft über golden/de-regeln.json.
 *
 * Was hier bewusst FEHLT: eine Zahl-in-Worte-Maschinerie. espeak liest Zahlen
 * selbst richtig (20/20 Proben der Referenz, inkl. „null Komma eins") — eine
 * zweite Schicht darüber würde nur Fehler einführen.
 *
 * Regex-Portierung: Pythons `\w`/`\b` sind Unicode; Javas `\w` ist ASCII.
 * Deshalb überall ausgeschriebene Klassen `[\p{L}\p{N}_]` bzw. Lookarounds —
 * die Goldens halten die Gleichheit fest.
 */
object Textregeln {

    private const val W = """[\p{L}\p{N}_]"""

    private val MONATE = listOf(
        "Januar", "Februar", "März", "April", "Mai", "Juni",
        "Juli", "August", "September", "Oktober", "November", "Dezember",
    )

    private val ORDINAL = mapOf(
        1 to "erst", 2 to "zweit", 3 to "dritt", 4 to "viert", 5 to "fünft",
        6 to "sechst", 7 to "siebt", 8 to "acht", 9 to "neunt", 10 to "zehnt",
        11 to "elft", 12 to "zwölft", 13 to "dreizehnt", 14 to "vierzehnt",
        15 to "fünfzehnt", 16 to "sechzehnt", 17 to "siebzehnt", 18 to "achtzehnt",
        19 to "neunzehnt", 20 to "zwanzigst", 21 to "einundzwanzigst",
        22 to "zweiundzwanzigst", 23 to "dreiundzwanzigst", 24 to "vierundzwanzigst",
        25 to "fünfundzwanzigst", 26 to "sechsundzwanzigst", 27 to "siebenundzwanzigst",
        28 to "achtundzwanzigst", 29 to "neunundzwanzigst", 30 to "dreißigst",
        31 to "einunddreißigst",
    )

    /**
     * Wörter, nach denen die Ordinalzahl auf -en geht. „den" ist der wichtigste
     * Eintrag: die häufigste deutsche Datumsformel — sein Fehlen erzeugte in
     * der Referenz „den dritter Mai".
     */
    private val DATIV_AKKUSATIV = setOf(
        "am", "vom", "zum", "seit", "bis", "ab", "dem", "einem", "jeden",
        "des", "eines", "beim", "nach", "vor",
        "den", "einen", "diesen", "jenen",
    )

    private val NOMINATIV = setOf("der", "die", "dieser", "jeder", "jener")

    private data class Ersetzung(val muster: Regex, val ersatz: String)

    private fun ab(kuerzel: String, lang: String, ignoreCase: Boolean = false) = Ersetzung(
        Regex("(?<!$W)$kuerzel", if (ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet()),
        lang,
    )

    /** Abkürzungen mit Punkt: buchstabiert + 250 ms Pausenloch — beides gemessen. */
    private val ABKUERZUNGEN: List<Ersetzung> = listOf(
        ab("""z\.\s?B\.""", "zum Beispiel", true),
        ab("""u\.\s?a\.""", "unter anderem", true),
        ab("""d\.\s?h\.""", "das heißt", true),
        ab("""u\.\s?U\.""", "unter Umständen"),
        ab("""v\.\s?a\.""", "vor allem"),
        ab("""bzw\.""", "beziehungsweise", true),
        ab("""ggf\.""", "gegebenenfalls", true),
        ab("""evtl\.""", "eventuell", true),
        ab("""inkl\.""", "inklusive", true),
        ab("""zzgl\.""", "zuzüglich", true),
        ab("""usw\.""", "und so weiter", true),
        ab("""etc\.""", "et cetera", true),
        ab("""vgl\.""", "vergleiche", true),
        ab("""ca\.""", "zirka", true),
        ab("""sog\.""", "sogenannt", true),
        ab("""Nr\.""", "Nummer"),
        ab("""Dr\.""", "Doktor"),
        ab("""Prof\.""", "Professor"),
        ab("""Tel\.""", "Telefon"),
        ab("""MwSt\.""", "Mehrwertsteuer"),
        Ersetzung(Regex("""(?<!$W)St\.(?=\s+[A-ZÄÖÜ])"""), "Sankt"),
        Ersetzung(Regex("""(?<=[a-zäöüß])str\.(?!$W)"""), "straße"),
        Ersetzung(Regex("""(?<!$W)Str\.(?!$W)"""), "Straße"),
    ) + listOf(
        // Monats- und Wochentagskürzel — Verwaltungsalltag („Bis Ende Nov.").
        "Jan" to "Januar", "Feb" to "Februar", "Mrz" to "März", "Apr" to "April",
        "Jun" to "Juni", "Jul" to "Juli", "Aug" to "August", "Sept" to "September",
        "Sep" to "September", "Okt" to "Oktober", "Nov" to "November", "Dez" to "Dezember",
        "Mo" to "Montag", "Di" to "Dienstag", "Mi" to "Mittwoch", "Do" to "Donnerstag",
        "Fr" to "Freitag", "Sa" to "Samstag", "So" to "Sonntag",
    ).map { (kurz, lang) -> Ersetzung(Regex("""(?<!$W)$kurz\.(?!$W)"""), lang) }

    private val EINHEITEN = mapOf(
        "km/h" to "Kilometer pro Stunde", "kWh" to "Kilowattstunden",
        "m²" to "Quadratmeter", "m³" to "Kubikmeter",
        "°C" to "Grad Celsius", "°F" to "Grad Fahrenheit",
        "kg" to "Kilogramm", "km" to "Kilometer", "cm" to "Zentimeter",
        "mm" to "Millimeter", "ml" to "Milliliter", "mg" to "Milligramm",
        "kB" to "Kilobyte", "MB" to "Megabyte", "GB" to "Gigabyte",
        "TB" to "Terabyte", "kW" to "Kilowatt", "Hz" to "Hertz",
        "m" to "Meter", "l" to "Liter", "g" to "Gramm", "t" to "Tonnen",
    )

    /** Zahl (ganz ODER mit Dezimalkomma) vor der Einheit — „1,5 kg" gehört dazu. */
    private val EINHEIT = Regex(
        """(?<![$W.])(\d+(?:[.,]\d+)?)\s?(""" +
            EINHEITEN.keys.sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) } +
            """)(?!$W)"""
    )

    private val ORDINAL_MONAT = Regex(
        """(?<![\d.])(\d{1,2})\.\s+(""" + MONATE.joinToString("|") + """)(?!$W)"""
    )

    /** Nachfolgendes „Uhr" wird mitgefasst — sonst „acht Uhr neun Uhr". */
    private val UHRZEIT = Regex("""(?<!\d)(\d{1,2}):(\d{2})(?::(\d{2}))?(?!\d)(\s*Uhr)?""")

    private val TAUSENDER = Regex("""(?<!\d)(\d{1,3})\.(\d{3})(?!\d)""")

    private val SATZZEICHEN = listOf(
        "..." to "…",   // 930 ms → 300 ms
        "–" to ",",     // verschwindet bei espeak sonst spurlos
        "‒" to ",",
        "—" to ",",
    )

    /** Die Ordinalendung folgt dem Wort direkt davor — das eine Stück Kasus, das eine Regex trägt. */
    private fun endung(text: String, ab: Int): String {
        var ende = ab
        while (ende > 0 && text[ende - 1].isWhitespace()) ende--
        var anfang = ende
        while (anfang > 0 && text[anfang - 1].isLetter()) anfang--
        return when (text.substring(anfang, ende).lowercase()) {
            in DATIV_AKKUSATIV -> "en"
            in NOMINATIV -> "e"
            else -> "er"
        }
    }

    data class Ergebnis(val text: String, val meldungen: List<String>)

    fun berichtige(eingabe: String): Ergebnis {
        var text = eingabe
        val meldungen = mutableListOf<String>()

        // Eine Abkürzung am Satzende verschluckt ihren Punkt („im Dez." →
        // „im Dezember" ohne Schluss) — das Schlusszeichen wird gerettet.
        val schluss = text.trimEnd().takeLast(1)
        val endeteMitPunkt = schluss.isNotEmpty() && schluss[0] in ".!?…"

        for ((muster, ersatz) in ABKUERZUNGEN) {
            val vorher = text
            text = muster.replace(text, ersatz)
            if (text != vorher) meldungen.add("Abkürzung → „$ersatz“")
        }

        var getroffen = false
        text = ORDINAL_MONAT.replace(text) { m ->
            val tag = m.groupValues[1].toInt()
            if (tag !in 1..31) return@replace m.value
            getroffen = true
            ORDINAL.getValue(tag) + endung(text, m.range.first) + " " + m.groupValues[2]
        }
        if (getroffen) meldungen.add("Datum ausgeschrieben")

        getroffen = false
        text = UHRZEIT.replace(text) { m ->
            val st = m.groupValues[1].toInt()
            val mi = m.groupValues[2].toInt()
            if (st !in 0..23 || mi !in 0..59) return@replace m.value
            getroffen = true
            if (mi == 0) "$st Uhr" else "$st Uhr $mi"
        }
        if (getroffen) meldungen.add("Uhrzeit ausgeschrieben")

        getroffen = false
        text = TAUSENDER.replace(text) { m ->
            getroffen = true
            m.groupValues[1] + m.groupValues[2]
        }
        if (getroffen) meldungen.add("Tausenderpunkt entfernt")

        getroffen = false
        text = EINHEIT.replace(text) { m ->
            getroffen = true
            m.groupValues[1] + " " + EINHEITEN.getValue(m.groupValues[2])
        }
        if (getroffen) meldungen.add("Einheit ausgeschrieben")

        for ((alt, neu) in SATZZEICHEN) {
            if (alt in text) {
                text = text.replace(alt, neu)
                meldungen.add("„$alt“ → „$neu“")
            }
        }

        if (endeteMitPunkt && text.trimEnd().takeLast(1).let { it.isEmpty() || it[0] !in ".!?…" }) {
            text = text.trimEnd() + schluss
        }
        return Ergebnis(text, meldungen)
    }
}
