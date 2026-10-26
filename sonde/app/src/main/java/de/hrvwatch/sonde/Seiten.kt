package de.hrvwatch.sonde

/** Eine Anzeigeseite: drei Zeilen, bei Medien Titel / Interpret / Album. */
data class Anzeige(val titel: String, val zeile2: String, val zeile3: String)

/**
 * Baut die Seiten aus dem aktuellen Zustand. Die Seitenstruktur ist der einzige Vertrag
 * zwischen Rechnung und Anzeige; später schreibt die trainingsapp dieselben Tripel.
 */
object Seiten {
    const val ANZAHL = 6

    fun seite(
        index: Int,
        hf: Int?,
        r: Rechnung,
        nr: Int,
        sekunden: Long,
        taktMs: Long,
        stempelAn: Boolean,
        kurve: String,
    ): Anzeige {
        // Ohne Stempel bleibt Zeile 3 konstant; Test F4: setzt jede Aktualisierung die Laufschrift zurück?
        val stempel = if (stempelAn) "#$nr ${sekunden}s ${taktMs / 1000}s-Takt" else "hrv-watch"
        val rmssd = format(r.rmssd())
        return when (index) {
            0 -> Anzeige("HF ${hf ?: "--"}", "RMSSD $rmssd ms", stempel)
            1 -> Anzeige("RMSSD $rmssd ms", "60 s, ${r.anzahlImFenster()} RR", stempel)
            2 -> Anzeige("RR ${r.letztes?.toInt() ?: "--"} ms", "n=${r.anzahlGesamt}", stempel)
            3 -> Anzeige(kurve, "HF ${hf ?: "--"}, 150 s", stempel)
            // Konstanter Text: mit Stempel aus ändert sich nur das Cover (Test F9b).
            4 -> Anzeige("Kurve als Bild", "Hintergrund?", stempel)
            else -> Anzeige("Zustand", "${zustand(hf)}, HF ${hf ?: "--"}, RMSSD $rmssd ms", stempel)
        }
    }

    /** Grobe Einordnung nur für die Sonde. Zonen und Limits kommen später aus der trainingsapp. */
    private fun zustand(hf: Int?): String = when {
        hf == null -> "kein Signal"
        hf < 100 -> "ruhig"
        hf < 140 -> "moderat"
        else -> "intensiv"
    }

    private fun format(x: Double?): String = if (x == null) "--" else "%.0f".format(x)
}
