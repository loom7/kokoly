// SPDX-License-Identifier: GPL-3.0-or-later
package de.tilly.kokoly.tts.settings

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

/** M0-Platzhalter. Die echte Einstellungs-Activity (Sprachen an/ab, Modell-Download) ist M2b/M4. */
class SettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply {
            text = "Kokoly — im Aufbau (M0). Einstellungen folgen mit Meilenstein M4."
            setPadding(48, 48, 48, 48)
        })
    }
}
