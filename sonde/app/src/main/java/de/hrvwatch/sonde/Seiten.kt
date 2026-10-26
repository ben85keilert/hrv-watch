package de.hrvwatch.sonde

/** Eine Anzeigeseite: drei Zeilen, bei Medien Titel / Interpret / Album. */
data class Anzeige(val titel: String, val zeile2: String, val zeile3: String)

/**
 * Baut die Seiten aus dem aktuellen Zustand. Die Seitenstruktur ist der einzige Vertrag
 * zwischen Rechnung und Anzeige; später schreibt die trainingsapp dieselben Tripel.
 */
object Seiten {
    const val ANZAHL = 4

    fun seite(index: Int, hf: Int?, r: Rechnung, nr: Int, sekunden: Long, taktMs: Long): Anzeige {
        val stempel = "#$nr ${sekunden}s ${taktMs / 1000}s-Takt"
        val rmssd = format(r.rmssd())
        return when (index) {
            0 -> Anzeige("HF ${hf ?: "--"}", "RMSSD $rmssd ms", stempel)
            1 -> Anzeige("RMSSD $rmssd ms", "60 s, ${r.anzahlImFenster()} RR", stempel)
            2 -> Anzeige("RR ${r.letztes?.toInt() ?: "--"} ms", "n=${r.anzahlGesamt}", stempel)
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
