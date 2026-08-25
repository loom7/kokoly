// SPDX-License-Identifier: GPL-3.0-or-later
package de.tilly.kokoly.tts.model

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Das Modelllager — Ablage, Bezug und Prüfung der Gewichte (M2b, ADR-0005).
 *
 * **Ablageort:** `noBackupFilesDir/modelle` — Gewichte gehören nicht ins
 * Auto-Backup (Größe, Reproduzierbarkeit über models.json). Entwicklungsweg
 * bleibt: liegt eine Datei im externen App-Verzeichnis (adb push), gewinnt sie
 * — derselbe Name, kein Sonderpfad im Code dahinter.
 *
 * **Bezug:** models.json (im APK als Asset, identisch zur Repo-Wurzel — ein
 * JVM-Test hält beide gleich) nennt URL, Größe und SHA-256 je Datei. Der
 * Download läuft nur über ungetaktete Netze, unterstützt Fortsetzen (Range)
 * und prüft die Summe VOR dem Umbenennen — eine halbe oder falsche Datei
 * erreicht nie ihren endgültigen Namen.
 */
object ModellLager {

    private const val TAG = "KokolyLager"

    data class Eintrag(
        val name: String,
        val url: String,
        val sha256: String,
        val bytes: Long,
    )

    fun manifest(context: Context): List<Eintrag> {
        val text = context.assets.open("models.json").bufferedReader().readText()
        val wurzel = JSONObject(text).getJSONObject("modelle")
        val eintraege = mutableListOf<Eintrag>()
        for (modell in wurzel.keys()) {
            val knoten = wurzel.getJSONObject(modell)
            for (feld in listOf("dateien", "varianten")) {
                val liste = knoten.optJSONArray(feld) ?: continue
                for (i in 0 until liste.length()) {
                    val e = liste.getJSONObject(i)
                    if (!e.getString("url").startsWith("http")) continue
                    eintraege.add(Eintrag(
                        e.getString("name"), e.getString("url"),
                        e.getString("sha256"), e.getLong("bytes"),
                    ))
                }
            }
        }
        return eintraege
    }

    fun lagerOrdner(context: Context): File =
        File(context.noBackupFilesDir, "modelle").apply { mkdirs() }

    /** Auflösung: Entwicklungsweg (adb push, extern) gewinnt vor dem Lager. */
    fun datei(context: Context, name: String): File? {
        val dev = File(context.getExternalFilesDir(null), name)
        if (dev.exists()) return dev
        val lager = File(lagerOrdner(context), name)
        return if (lager.exists()) lager else null
    }

    fun istUngetaktet(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    /**
     * Lädt einen Manifest-Eintrag ins Lager. Blockierend — der Aufrufer sorgt
     * für den Thread. [fortschritt] bekommt (geladeneBytes, gesamtBytes).
     *
     * @return die fertige, summengeprüfte Datei.
     */
    fun lade(
        context: Context,
        eintrag: Eintrag,
        fortschritt: (Long, Long) -> Unit = { _, _ -> },
    ): File {
        val ziel = File(lagerOrdner(context), eintrag.name)
        if (ziel.exists()) return ziel
        check(istUngetaktet(context)) { "Kein ungetaktetes Netz — Download verweigert (ADR-0005)." }

        val teil = File(lagerOrdner(context), eintrag.name + ".teil")
        var ab = teil.length()
        if (ab >= eintrag.bytes) { teil.delete(); ab = 0 }

        val verbindung = URL(eintrag.url).openConnection() as HttpURLConnection
        verbindung.connectTimeout = 15_000
        verbindung.readTimeout = 30_000
        if (ab > 0) verbindung.setRequestProperty("Range", "bytes=$ab-")
        verbindung.instanceFollowRedirects = true

        val fortsetzung = verbindung.responseCode == 206
        if (!fortsetzung) { teil.delete(); ab = 0 }
        check(verbindung.responseCode in listOf(200, 206)) {
            "HTTP ${verbindung.responseCode} für ${eintrag.name}"
        }

        verbindung.inputStream.use { quelle ->
            java.io.FileOutputStream(teil, fortsetzung).use { senke ->
                val puffer = ByteArray(1 shl 16)
                var geladen = ab
                while (true) {
                    val n = quelle.read(puffer)
                    if (n < 0) break
                    senke.write(puffer, 0, n)
                    geladen += n
                    fortschritt(geladen, eintrag.bytes)
                }
            }
        }

        val ist = sha256(teil)
        check(ist.equals(eintrag.sha256, ignoreCase = true)) {
            teil.delete()
            "Prüfsumme falsch für ${eintrag.name}: $ist"
        }
        check(teil.renameTo(ziel)) { "Umbenennen fehlgeschlagen: ${eintrag.name}" }
        Log.i(TAG, "${eintrag.name} geladen und geprüft (${eintrag.bytes} B)")
        return ziel
    }

    fun sha256(datei: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        datei.inputStream().use { ein ->
            val puffer = ByteArray(1 shl 20)
            while (true) {
                val n = ein.read(puffer)
                if (n < 0) break
                md.update(puffer, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
