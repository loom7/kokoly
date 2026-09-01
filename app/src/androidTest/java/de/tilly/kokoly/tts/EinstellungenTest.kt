// SPDX-License-Identifier: GPL-3.0-or-later
package de.tilly.kokoly.tts

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.tilly.kokoly.tts.settings.Einstellungen
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Der Erstlauf-Merker: einmal erledigt, bleibt erledigt. */
@RunWith(AndroidJUnit4::class)
class EinstellungenTest {

    @Test
    fun willkommenMerkerHaeltGenauEinmal() {
        val kontext = InstrumentationRegistry.getInstrumentation().targetContext
        kontext.getSharedPreferences("kokoly", android.content.Context.MODE_PRIVATE)
            .edit().remove("willkommen_erledigt").commit()

        assertFalse("Frische Installation muss den Willkommensblock zeigen",
            Einstellungen.istWillkommenErledigt(kontext))
        Einstellungen.setzeWillkommenErledigt(kontext)
        assertTrue(Einstellungen.istWillkommenErledigt(kontext))
    }
}
