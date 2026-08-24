// SPDX-License-Identifier: GPL-3.0-or-later
package de.tilly.kokoly.tts.pipeline

import android.content.Context
import org.json.JSONObject

/**
 * Das 114-Token-Vokabular des Modells (Zeichen → Token-Id), exportiert vom
 * Golden-Generator aus der Referenz — eine Quelle für Filter UND Tokenisierung.
 */
object Vokabular {

    fun lade(context: Context): Map<Char, Long> {
        val text = context.assets.open("vokabular.json")
            .bufferedReader().readText()
        val o = JSONObject(text)
        val karte = HashMap<Char, Long>(o.length())
        for (k in o.keys()) karte[k.single()] = o.getLong(k)
        return karte
    }
}
