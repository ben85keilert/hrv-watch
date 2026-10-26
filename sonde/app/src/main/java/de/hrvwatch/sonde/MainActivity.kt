package de.hrvwatch.sonde

import android.Manifest
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Bedienoberfläche der Sonde: eine Spalte Knöpfe, darunter Zustand und Protokoll des Dienstes.
 * Alles programmatisch, damit kein Layout-XML gepflegt werden muss.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var status: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val auffrischen = object : Runnable {
        override fun run() {
            status.text = AnzeigeDienst.protokoll()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val spalte = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        fun knopf(text: String, aktion: String, taktMs: Long? = null) {
            spalte.addView(Button(this).apply {
                this.text = text
                setOnClickListener { befehl(aktion, taktMs) }
            })
        }

        knopf("Start (Simulation)", AnzeigeDienst.START)
        knopf("Stopp", AnzeigeDienst.STOPP)
        knopf("Medienkanal an/aus", AnzeigeDienst.MEDIEN_UMSCHALTEN)
        knopf("Benachrichtigung an/aus", AnzeigeDienst.NOTIF_UMSCHALTEN)
        knopf("Takt 1 s", AnzeigeDienst.TAKT, 1000L)
        knopf("Takt 2 s", AnzeigeDienst.TAKT, 2000L)
        knopf("Takt 5 s", AnzeigeDienst.TAKT, 5000L)
        knopf("Nächste Seite", AnzeigeDienst.SEITE_WEITER)
        knopf("Stille abspielen an/aus", AnzeigeDienst.STILLE_UMSCHALTEN)
        knopf("Polar H10 verbinden", AnzeigeDienst.H10_VERBINDEN)

        status = TextView(this).apply {
            textSize = 12f
            typeface = Typeface.MONOSPACE
            setPadding(0, 24, 0, 0)
        }
        spalte.addView(status)

        setContentView(ScrollView(this).apply { addView(spalte) })
        berechtigungen()
    }

    override fun onResume() {
        super.onResume()
        handler.post(auffrischen)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(auffrischen)
    }

    /** Schickt einen Befehl an den Dienst; startet ihn bei Bedarf als Vordergrunddienst. */
    private fun befehl(aktion: String, taktMs: Long?) {
        val i = Intent(this, AnzeigeDienst::class.java).setAction(aktion)
        if (taktMs != null) i.putExtra(AnzeigeDienst.EXTRA_TAKT, taktMs)
        ContextCompat.startForegroundService(this, i)
    }

    /** Fragt alle Laufzeitrechte auf einmal ab; die Antwort wird nicht ausgewertet, das Protokoll zeigt Fehler. */
    private fun berechtigungen() {
        val noetig = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            noetig += Manifest.permission.BLUETOOTH_SCAN
            noetig += Manifest.permission.BLUETOOTH_CONNECT
        } else {
            noetig += Manifest.permission.ACCESS_FINE_LOCATION
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            noetig += Manifest.permission.POST_NOTIFICATIONS
        }
        ActivityCompat.requestPermissions(this, noetig.toTypedArray(), 1)
    }
}
