package de.hrvwatch.sonde

import kotlin.math.sqrt

/**
 * Vorschau-RMSSD über ein gleitendes Fenster von 60 s Herzschlagzeit. Keine
 * Artefaktkorrektur, keine Nähte, nicht hrv-core. Nur damit am Band etwas Plausibles steht.
 * Die echte Rechnung macht hrv-core in der trainingsapp.
 */
class Rechnung(private val fensterMs: Double = 60_000.0) {

    private val puffer = ArrayDeque<Double>()
    private var summe = 0.0

    var anzahlGesamt = 0
        private set
    var letztes: Double? = null
        private set

    @Synchronized
    fun neu(rrMs: Double) {
        puffer.addLast(rrMs)
        summe += rrMs
        anzahlGesamt++
        letztes = rrMs
        while (puffer.size > 2 && summe - puffer.first() > fensterMs) {
            summe -= puffer.removeFirst()
        }
    }

    @Synchronized
    fun rmssd(): Double? {
        if (puffer.size < 10) return null
        var quadrate = 0.0
        var n = 0
        val it = puffer.iterator()
        var vorher = it.next()
        while (it.hasNext()) {
            val x = it.next()
            quadrate += (x - vorher) * (x - vorher)
            n++
            vorher = x
        }
        return sqrt(quadrate / n)
    }

    @Synchronized
    fun anzahlImFenster(): Int = puffer.size

    @Synchronized
    fun leeren() {
        puffer.clear()
        summe = 0.0
        letztes = null
    }
}
