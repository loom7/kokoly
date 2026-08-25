// SPDX-License-Identifier: GPL-3.0-or-later
package de.tilly.kokoly.tts

import android.content.res.Configuration
import android.util.TypedValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tag/Nacht-Verdrahtung der Einstellungen (Nutzerfund 25.08.2026: die Seite
 * blieb im Dark Mode hell). Löst Theme.Kokoly in beiden uiMode-Konfigurationen
 * auf und prüft android:isLightTheme — braucht keinen Bildschirm, läuft also
 * auch am gesperrten Gerät.
 */
@RunWith(AndroidJUnit4::class)
class ThemaTest {

    private fun istHell(nachtmodus: Int): Boolean {
        val basis = InstrumentationRegistry.getInstrumentation().targetContext
        val konfiguration = Configuration(basis.resources.configuration).apply {
            uiMode = nachtmodus or (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv())
        }
        val kontext = basis.createConfigurationContext(konfiguration)
        val thema = kontext.resources.newTheme()
        thema.applyStyle(R.style.Theme_Kokoly, true)
        val wert = TypedValue()
        thema.resolveAttribute(android.R.attr.isLightTheme, wert, true)
        return wert.data != 0
    }

    @Test
    fun tagHellNachtDunkel() {
        assertEquals(true, istHell(Configuration.UI_MODE_NIGHT_NO))
        assertEquals(false, istHell(Configuration.UI_MODE_NIGHT_YES))
    }
}
