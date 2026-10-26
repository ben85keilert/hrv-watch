package de.hrvwatch.sonde

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path

/**
 * Verlauf der Herzfrequenz der letzten 150 s, einmal als Textzeile aus Blockzeichen (für eine
 * Medienzeile) und einmal als Bild (als Albumcover, falls das Band Cover anzeigt).
 * Skala 60–160 bpm. Nur für den Fit3-Test; die echte Kurve zeichnet später die trainingsapp.
 */
object Kurve {
    const val FENSTER_MS = 150_000L
    // Blockzeichen ▁▂▃▅▆▇█ zeigt die Fit3 als Kästchen (26.10.); zweiter Versuch mit ASCII-Stufen.
    private const val BLOECKE = "_.-'^"

    /** Ein Zeichen je Abschnitt (bei 20 Zeichen 7,5 s), Maximum im Abschnitt; ohne Wert Leerzeichen. */
    fun text(verlauf: List<Pair<Long, Int>>, jetzt: Long, zeichen: Int = 20): String {
        val abschnitt = FENSTER_MS / zeichen
        val anfang = jetzt - FENSTER_MS
        val sb = StringBuilder()
        for (i in 0 until zeichen) {
            val von = anfang + i * abschnitt
            val bis = von + abschnitt
            val max = verlauf.filter { it.first >= von && it.first < bis }.maxOfOrNull { it.second }
            sb.append(if (max == null) ' ' else BLOECKE[(max - 60).coerceIn(0, 100) * (BLOECKE.length - 1) / 100])
        }
        return sb.toString()
    }

    /** Quadratisches Bild: Rasterlinien bei 100/120/140, rote Kurve, aktuelle HF oben links. */
    fun bild(verlauf: List<Pair<Long, Int>>, jetzt: Long, nr: Int, groesse: Int = 400): Bitmap {
        val b = Bitmap.createBitmap(groesse, groesse, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        c.drawColor(Color.BLACK)
        val raster = Paint().apply { color = Color.DKGRAY; strokeWidth = 2f }
        for (hf in listOf(100, 120, 140)) {
            val y = yVon(hf, groesse)
            c.drawLine(0f, y, groesse.toFloat(), y, raster)
        }
        val linie = Paint().apply {
            color = Color.rgb(255, 80, 80)
            strokeWidth = groesse / 50f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        val anfang = jetzt - FENSTER_MS
        val pfad = Path()
        var erster = true
        for ((t, hf) in verlauf) {
            if (t < anfang) continue
            val x = (t - anfang).toFloat() / FENSTER_MS * groesse
            val y = yVon(hf, groesse)
            if (erster) {
                pfad.moveTo(x, y)
                erster = false
            } else {
                pfad.lineTo(x, y)
            }
        }
        c.drawPath(pfad, linie)
        val schrift = Paint().apply {
            color = Color.WHITE
            textSize = groesse / 7f
            isAntiAlias = true
            isFakeBoldText = true
        }
        verlauf.lastOrNull()?.let { c.drawText("HF ${it.second}", groesse * 0.05f, groesse * 0.2f, schrift) }
        // Bildnummer rechts oben: daran sieht man am Band, ob das Cover nachgeladen wird.
        c.drawText("#$nr", groesse * 0.65f, groesse * 0.2f, schrift)
        return b
    }

    private fun yVon(hf: Int, groesse: Int): Float =
        groesse * (1f - (hf - 60).coerceIn(0, 100) / 100f) * 0.9f + groesse * 0.05f
}
