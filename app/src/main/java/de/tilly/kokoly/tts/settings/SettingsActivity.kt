// SPDX-License-Identifier: GPL-3.0-or-later
package de.tilly.kokoly.tts.settings

import android.app.Activity
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import de.tilly.kokoly.tts.model.ModellLager
import de.tilly.kokoly.tts.pipeline.EnginePipeline
import de.tilly.kokoly.tts.service.Sprachen
import kotlin.concurrent.thread

/**
 * Die Einstellungen (M2b + M4): Modellbezug und Sprach-An/Abwahl.
 *
 * Bewusst schlichte, programmatische Oberfläche — die Engine wird über die
 * Systemeinstellungen erreicht, hier zählt Funktion: Modelle laden (nur
 * ungetaktet, mit Prüfsumme), Sprachen an- und abwählen (Ziel Z4).
 */
class SettingsActivity : Activity() {

    private lateinit var wurzel: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wurzel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }
        setContentView(ScrollView(this).apply {
            addView(wurzel)
            // Edge-to-Edge (Pflicht ab targetSdk 35): ohne Inset-Polster läge
            // die Überschrift unter der Statusleiste und die letzte Zeile
            // unter der Navigationsleiste.
            clipToPadding = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                setOnApplyWindowInsetsListener { v, insets ->
                    val rand = insets.getInsets(
                        WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                    v.setPadding(rand.left, rand.top, rand.right, rand.bottom)
                    WindowInsets.CONSUMED
                }
            }
        })
        baue()
    }

    private fun baue() {
        wurzel.removeAllViews()
        ueberschrift("Kokoly")

        // Erstlauf (Nutzerwunsch 31.08.2026): Sprachüberblick + die Frage nach
        // dem Deutsch-Download auf demselben Bild; alles Weitere geht ohnehin
        // hier in den Einstellungen.
        if (!Einstellungen.istWillkommenErledigt(this)) {
            willkommen()
        }

        ueberschrift("Modelle")
        val fehlend = ModellLager.manifest(this).filter {
            ModellLager.datei(this, it.name) == null
        }
        if (fehlend.isEmpty()) {
            zeile("Alle Modelldateien vorhanden.")
        } else {
            zeile("${fehlend.size} Datei(en) fehlen (" +
                "${fehlend.sumOf { it.bytes } / 1_048_576} MiB). Download nur über " +
                "ungetaktetes Netz (WLAN).")
            downloadKnopf("Alle herunterladen", fehlend)
        }

        ueberschrift("Sprachen")
        zeile("Abgewählte Sprachen verschwinden für alle Apps aus der Stimmliste.")
        // Haken-Farben explizit an die Textfarben des Themas binden: die
        // DeviceDefault-Vorgabetönung war im Dunkelmodus unsichtbar
        // (Nutzerfund 25.08.2026).
        val hakenFarben = ColorStateList(
            arrayOf(
                intArrayOf(-android.R.attr.state_enabled),
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(),
            ),
            intArrayOf(
                themenFarbe(android.R.attr.textColorHint),
                themenFarbe(android.R.attr.textColorPrimary),
                themenFarbe(android.R.attr.textColorSecondary),
            ),
        )
        for (sprache in Sprachen.ALLE) {
            val vorhanden = EnginePipeline.gruppeVorhanden(this, sprache.gruppe)
            wurzel.addView(CheckBox(this).apply {
                text = "${sprache.locale.displayName}" +
                    if (vorhanden) "" else "  (Modell fehlt)"
                isChecked = Einstellungen.istAktiv(this@SettingsActivity, sprache.espeak)
                isEnabled = vorhanden
                buttonTintList = hakenFarben
                setOnCheckedChangeListener { _, an ->
                    Einstellungen.setzeAktiv(this@SettingsActivity, sprache.espeak, an)
                }
            })
        }

        ueberschrift("Über")
        // Das GPL-Quellcodeangebot: die App nennt ihre Quelle selbst
        // (feld4-Lizenzcheckliste; Repo öffentlich seit F8).
        zeile("Kokoly — Kokoro-TTS für Android. GPL-3.0-or-later; Quelltext und " +
            "Lizenzhinweise: github.com/loom7/kokoly. Modelle: Kokoro-82M (Apache-2.0).")
    }

    /** Der Erstlauf-Block: unterstützte Sprachen + Deutsch-Download-Frage. */
    private fun willkommen() {
        ueberschrift("Willkommen")
        zeile("Kokoly spricht ${Sprachen.ALLE.size} Sprachen — vollständig " +
            "offline, als Sprachausgabe fürs ganze System:")
        for (sprache in Sprachen.ALLE) {
            val zusatz = if (sprache.gruppe == Sprachen.MARTIN) "Stimme „Martin“"
                else "${sprache.stimmen.size} Stimmen"
            zeile("•  ${sprache.locale.displayName} ($zusatz)")
        }

        val deutschDateien = Sprachen.MARTIN.modelle + Sprachen.MARTIN.stimmbank
        val deutschFehlt = ModellLager.manifest(this).filter {
            it.name in deutschDateien && ModellLager.datei(this, it.name) == null
        }
        if (deutschFehlt.isEmpty()) {
            zeile("Die deutsche Sprache ist bereits vorhanden.")
        } else {
            zeile("Die Sprachdaten sind nicht in der App enthalten. Soll die " +
                "deutsche Sprache jetzt geladen werden (" +
                "${deutschFehlt.sumOf { it.bytes } / 1_048_576} MiB, nur über " +
                "WLAN)? Alle anderen Sprachen lassen sich jederzeit unter " +
                "„Modelle“ nachladen.")
            downloadKnopf("Deutsch herunterladen", deutschFehlt) {
                Einstellungen.setzeWillkommenErledigt(this)
            }
        }
        wurzel.addView(Button(this).apply {
            text = if (deutschFehlt.isEmpty()) "Los geht’s" else "Später"
            setOnClickListener {
                Einstellungen.setzeWillkommenErledigt(this@SettingsActivity)
                baue()
            }
        })
    }

    /**
     * Download-Baustein: Knopf + Fortschrittsbalken + Statuszeile für eine
     * Dateiliste; nach Erfolg [beiErfolg], dann Neuaufbau der Seite.
     */
    private fun downloadKnopf(
        beschriftung: String,
        eintraege: List<ModellLager.Eintrag>,
        beiErfolg: () -> Unit = {},
    ) {
        val balken = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
            .apply { max = 1000; visibility = View.GONE }
        val stand = TextView(this)
        wurzel.addView(Button(this).apply {
            text = beschriftung
            setOnClickListener {
                isEnabled = false
                balken.visibility = View.VISIBLE
                thread {
                    for (eintrag in eintraege) {
                        runCatching {
                            ModellLager.lade(this@SettingsActivity, eintrag) { ist, soll ->
                                runOnUiThread {
                                    stand.text = "${eintrag.name}: ${ist / 1_048_576} MiB"
                                    balken.progress = (ist * 1000 / soll).toInt()
                                }
                            }
                        }.onFailure { f ->
                            runOnUiThread {
                                stand.text = "Fehler: ${f.message}"
                                isEnabled = true
                            }
                            return@thread
                        }
                    }
                    runOnUiThread { beiErfolg(); baue() }
                }
            }
        })
        wurzel.addView(balken)
        wurzel.addView(stand)
    }

    /** Löst ein Farbattribut des aktiven Themas auf (Selector → Vorgabefarbe). */
    private fun themenFarbe(attr: Int): Int {
        val wert = TypedValue()
        theme.resolveAttribute(attr, wert, true)
        return if (wert.resourceId != 0) getColor(wert.resourceId) else wert.data
    }

    private fun ueberschrift(text: String) = wurzel.addView(TextView(this).apply {
        this.text = text
        textSize = 20f
        setPadding(0, 32, 0, 8)
    })

    private fun zeile(text: String) = wurzel.addView(TextView(this).apply {
        this.text = text
        setPadding(0, 4, 0, 4)
    })
}
