package de.hrvwatch.sonde

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media.app.NotificationCompat.MediaStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sin

/**
 * Vordergrunddienst der Sonde. Hält die MediaSession (Weg 1), schreibt die aktuelle Seite in
 * Metadaten und Medien-Notification, wahlweise zusätzlich in eine normale Benachrichtigung
 * mit fester ID (Weg 2). Werte aus Simulation oder H10Quelle.
 *
 * Tasten vom Band kommen als MediaSession-Callbacks: ◀ ▶ blättern, ⏯ startet/stoppt die Werte.
 * Der Wiedergabezustand bleibt dabei PLAYING, damit das Band die Steuerung nicht ausblendet.
 */
@SuppressLint("MissingPermission")
class AnzeigeDienst : Service() {

    companion object {
        const val START = "de.hrvwatch.sonde.START"
        const val STOPP = "de.hrvwatch.sonde.STOPP"
        const val MEDIEN_UMSCHALTEN = "de.hrvwatch.sonde.MEDIEN"
        const val NOTIF_UMSCHALTEN = "de.hrvwatch.sonde.NOTIF"
        const val TAKT = "de.hrvwatch.sonde.TAKT"
        const val SEITE_WEITER = "de.hrvwatch.sonde.SEITE_WEITER"
        const val STILLE_UMSCHALTEN = "de.hrvwatch.sonde.STILLE"
        const val H10_VERBINDEN = "de.hrvwatch.sonde.H10"
        const val EXTRA_TAKT = "takt_ms"

        private const val KANAL_MEDIEN = "medien"
        private const val KANAL_WERTE = "werte"
        private const val ID_MEDIEN = 1
        private const val ID_WERTE = 2

        private val zeilen = ArrayDeque<String>()
        @Volatile private var zustandstext = "Dienst nicht gestartet"
        private val uhr = SimpleDateFormat("HH:mm:ss", Locale.GERMANY)

        @Synchronized
        fun log(text: String) {
            zeilen.addFirst("${uhr.format(Date())} $text")
            while (zeilen.size > 40) zeilen.removeLast()
        }

        @Synchronized
        fun protokoll(): String = zustandstext + "\n\n" + zeilen.joinToString("\n")
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var session: MediaSessionCompat
    private val rechnung = Rechnung()
    private var quelle: H10Quelle? = null
    private var stille: AudioTrack? = null

    private var laeuft = false
    private var medienAn = true
    private var notifAn = false
    private var taktMs = 1000L
    private var seite = 0
    private var nr = 0
    private var startZeit = 0L
    private var hf: Int? = null
    private var simZeit = 0.0

    private val takt = object : Runnable {
        override fun run() {
            aktualisiere()
            handler.postDelayed(this, taktMs)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        kanaele()
        session = MediaSessionCompat(this, "hrv-watch").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    log("Band: Play")
                    laufen(true)
                }

                override fun onPause() {
                    log("Band: Pause (Zustand bleibt PLAYING)")
                    laufen(false)
                }

                override fun onStop() {
                    log("Band: Stop (ignoriert)")
                }

                override fun onSkipToNext() {
                    log("Band: weiter")
                    seite = (seite + 1) % Seiten.ANZAHL
                    aktualisiere()
                }

                override fun onSkipToPrevious() {
                    log("Band: zurück")
                    seite = (seite + Seiten.ANZAHL - 1) % Seiten.ANZAHL
                    aktualisiere()
                }
            })
            setPlaybackState(spielt())
            isActive = true
        }
        log("Dienst erzeugt, MediaSession aktiv")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Immer zuerst in den Vordergrund, sonst bricht Android den Start nach 5 s ab.
        startForeground(ID_MEDIEN, medienNotification(aktuelleSeite()))

        when (intent?.action) {
            START -> {
                startZeit = SystemClock.elapsedRealtime()
                nr = 0
                if (quelle == null) rechnung.leeren()
                laufen(true)
            }
            STOPP -> {
                laufen(false)
                stilleAus()
                quelle?.trennen()
                quelle = null
                NotificationManagerCompat.from(this).cancel(ID_WERTE)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                zustandstext = "Dienst gestoppt"
                return START_NOT_STICKY
            }
            MEDIEN_UMSCHALTEN -> {
                medienAn = !medienAn
                log("Medienkanal ${if (medienAn) "an" else "aus"}")
            }
            NOTIF_UMSCHALTEN -> {
                notifAn = !notifAn
                log("Benachrichtigung ${if (notifAn) "an" else "aus"}")
                if (!notifAn) NotificationManagerCompat.from(this).cancel(ID_WERTE)
            }
            TAKT -> {
                taktMs = intent?.getLongExtra(EXTRA_TAKT, 1000L) ?: 1000L
                log("Takt ${taktMs / 1000} s")
                if (laeuft) {
                    handler.removeCallbacks(takt)
                    handler.post(takt)
                }
            }
            SEITE_WEITER -> {
                seite = (seite + 1) % Seiten.ANZAHL
                aktualisiere()
            }
            STILLE_UMSCHALTEN -> if (stille == null) stilleAn() else stilleAus()
            H10_VERBINDEN -> {
                if (quelle == null) {
                    rechnung.leeren()
                    hf = null
                    quelle = H10Quelle(
                        this,
                        log = { text -> handler.post { log(text) } },
                        messwert = { hfNeu, rr ->
                            handler.post {
                                hf = hfNeu
                                rr.forEach { rechnung.neu(it) }
                            }
                        },
                    )
                    quelle?.verbinden()
                } else {
                    log("H10 ist schon verbunden oder wird gesucht")
                }
            }
        }
        zustand()
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(takt)
        stilleAus()
        quelle?.trennen()
        session.isActive = false
        session.release()
        log("Dienst beendet")
        super.onDestroy()
    }

    // ---- Ablauf -------------------------------------------------------------------------

    private fun laufen(an: Boolean) {
        if (an == laeuft) return
        laeuft = an
        handler.removeCallbacks(takt)
        if (an) handler.post(takt)
        zustand()
    }

    private fun aktualisiere() {
        nr++
        if (quelle == null) simuliere()
        val s = aktuelleSeite()
        if (medienAn) {
            session.setMetadata(
                MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, s.titel)
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, s.zeile2)
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, s.zeile3)
                    .build(),
            )
            session.setPlaybackState(spielt())
            NotificationManagerCompat.from(this).notify(ID_MEDIEN, medienNotification(s))
        }
        if (notifAn) {
            NotificationManagerCompat.from(this).notify(ID_WERTE, werteNotification(s))
        }
        zustand()
    }

    private fun aktuelleSeite(): Anzeige {
        val sekunden = if (startZeit == 0L) 0L else (SystemClock.elapsedRealtime() - startZeit) / 1000
        return Seiten.seite(seite, hf, rechnung, nr, sekunden, taktMs)
    }

    /** HF schwingt langsam zwischen 60 und 80, RR passend mit etwas Rauschen. */
    private fun simuliere() {
        simZeit += taktMs / 1000.0
        val hfSim = 70.0 + 10.0 * sin(simZeit / 20.0)
        hf = hfSim.toInt()
        var vergangen = 0.0
        while (vergangen < taktMs) {
            val rr = 60000.0 / hfSim + (Math.random() - 0.5) * 40.0
            rechnung.neu(rr)
            vergangen += rr
        }
    }

    private fun zustand() {
        zustandstext = buildString {
            append(if (laeuft) "läuft" else "steht")
            append(", Quelle ").append(if (quelle == null) "Simulation" else "H10")
            append(", Medien ").append(if (medienAn) "an" else "aus")
            append(", Benachrichtigung ").append(if (notifAn) "an" else "aus")
            append(", Stille ").append(if (stille == null) "aus" else "an")
            append(", Takt ").append(taktMs / 1000).append(" s, Seite ").append(seite)
            append('\n')
            val s = aktuelleSeite()
            append(s.titel).append(" | ").append(s.zeile2).append(" | ").append(s.zeile3)
        }
    }

    private fun spielt(): PlaybackStateCompat = PlaybackStateCompat.Builder()
        .setActions(
            PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_STOP or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS,
        )
        .setState(PlaybackStateCompat.STATE_PLAYING, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1f)
        .build()

    // ---- Stille: lautloser Loop, falls das Band Medien nur bei echter Wiedergabe zeigt ----

    private fun stilleAn() {
        val rate = 8000
        val puffer = ByteArray(rate * 2) // 1 s 16-Bit-Mono, alles 0 = Stille
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(rate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(puffer.size)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(puffer, 0, puffer.size)
        track.setLoopPoints(0, rate, -1)
        track.play()
        stille = track
        log("Stille läuft")
    }

    private fun stilleAus() {
        stille?.let {
            try {
                it.stop()
            } catch (_: Exception) {
            }
            it.release()
            log("Stille aus")
        }
        stille = null
    }

    // ---- Benachrichtigungen --------------------------------------------------------------

    private fun kanaele() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(KANAL_MEDIEN, "Medienkanal (Sonde)", NotificationManager.IMPORTANCE_LOW),
        )
        nm.createNotificationChannel(
            NotificationChannel(KANAL_WERTE, "Werte als Benachrichtigung (Sonde)", NotificationManager.IMPORTANCE_DEFAULT),
        )
    }

    private fun oeffnen(): PendingIntent = PendingIntent.getActivity(
        this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
    )

    private fun medienNotification(s: Anzeige): Notification =
        NotificationCompat.Builder(this, KANAL_MEDIEN)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(s.titel)
            .setContentText(s.zeile2)
            .setSubText(s.zeile3)
            .setContentIntent(oeffnen())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setStyle(MediaStyle().setMediaSession(session.sessionToken))
            .build()

    private fun werteNotification(s: Anzeige): Notification =
        NotificationCompat.Builder(this, KANAL_WERTE)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle(s.titel)
            .setContentText(s.zeile2)
            .setSubText(s.zeile3)
            .setContentIntent(oeffnen())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
}
