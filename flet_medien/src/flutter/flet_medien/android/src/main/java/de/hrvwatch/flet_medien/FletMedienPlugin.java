package de.hrvwatch.flet_medien;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

/**
 * Kanal {@code flet_medien/session}: eine {@link MediaSession} mit drei Textzeilen.
 *
 * <p>Android zeigt Titel, Interpret und Album einer aktiven Session in der
 * Mediensteuerung, Galaxy Wearable reicht sie an die Galaxy Fit3 weiter (Befund
 * hrv-watch/docs/TESTPLAN_fit3.md, 26.10.2026). Es läuft kein Ton; der Zustand
 * PLAYING genügt dem Band. Die Session lebt im App-Prozess, am Leben hält ihn der
 * Messdienst von flet_ble.
 *
 * <p>Methoden (Dart → Java): {@code starten} (titel, zeile2, zeile3, laeuft) → bool,
 * {@code zeigen} (titel, zeile2, zeile3) → bool, {@code zustand} (laeuft) → bool,
 * {@code beenden}, {@code aktiv} → bool. Tasten (Java → Dart): {@code taste}
 * (taste: play, pause, stop, weiter, zurueck).
 *
 * <p>Das Kotlin der Sonde ({@code hrv-watch/sonde}, {@code AnzeigeDienst}) ist die
 * Vorlage, hier ohne Dienst, Simulation und Rechnung; mit dem Framework
 * ({@code android.media.session}) statt androidx.media, damit die Erweiterung keine
 * Abhängigkeit braucht.
 */
public class FletMedienPlugin implements FlutterPlugin, MethodChannel.MethodCallHandler {
    private static final String TAG = "FletMedien";
    private static final String KANAL = "uhr";
    private static final int BENACHRICHTIGUNG_ID = 4714;
    private static final long TASTEN =
            PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_PLAY_PAUSE
                    | PlaybackState.ACTION_STOP | PlaybackState.ACTION_SKIP_TO_NEXT
                    | PlaybackState.ACTION_SKIP_TO_PREVIOUS;

    private MethodChannel kanal;
    private Context kontext;
    private MediaSession session;
    private boolean laeuft = true;
    private String titel = "", zeile2 = "", zeile3 = "";

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        kontext = binding.getApplicationContext();
        kanal = new MethodChannel(binding.getBinaryMessenger(), "flet_medien/session");
        kanal.setMethodCallHandler(this);
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        beenden();
        kanal.setMethodCallHandler(null);
        kanal = null;
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
        try {
            switch (call.method) {
                case "starten":
                    Boolean l = call.argument("laeuft");
                    laeuft = l == null || l;
                    zeilenMerken(call);
                    sessionAnlegen();
                    zeigen();
                    result.success(true);
                    break;
                case "zeigen":
                    if (session == null) {
                        result.success(false);
                        break;
                    }
                    zeilenMerken(call);
                    zeigen();
                    result.success(true);
                    break;
                case "zustand":
                    Boolean z = call.argument("laeuft");
                    laeuft = z == null || z;
                    if (session != null) {
                        session.setPlaybackState(zustand());
                        benachrichtigen();
                    }
                    result.success(session != null);
                    break;
                case "beenden":
                    beenden();
                    result.success(null);
                    break;
                case "aktiv":
                    result.success(session != null);
                    break;
                default:
                    result.notImplemented();
            }
        } catch (Exception e) {
            Log.w(TAG, "Fehler bei " + call.method, e);
            result.error("medien", e.toString(), null);
        }
    }

    private void zeilenMerken(MethodCall call) {
        String t = call.argument("titel");
        String z2 = call.argument("zeile2");
        String z3 = call.argument("zeile3");
        titel = t == null ? "" : t;
        zeile2 = z2 == null ? "" : z2;
        zeile3 = z3 == null ? "" : z3;
    }

    // -- Session ---------------------------------------------------------------------

    private void sessionAnlegen() {
        if (session != null) return;
        kanalAnlegen();
        session = new MediaSession(kontext, "hrv-watch");
        session.setCallback(new MediaSession.Callback() {
            @Override
            public void onPlay() {
                taste("play");
            }

            @Override
            public void onPause() {
                taste("pause");
            }

            @Override
            public void onStop() {
                taste("stop");
            }

            @Override
            public void onSkipToNext() {
                taste("weiter");
            }

            @Override
            public void onSkipToPrevious() {
                taste("zurueck");
            }
        });
        session.setPlaybackState(zustand());
        session.setActive(true);
    }

    private void beenden() {
        if (session != null) {
            session.setActive(false);
            session.release();
            session = null;
        }
        NotificationManager nm = (NotificationManager) kontext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(BENACHRICHTIGUNG_ID);
    }

    /** Die Taste kommt auf dem Hauptthread an (die Session wurde dort angelegt). */
    private void taste(String name) {
        if (kanal == null) return;
        Map<String, Object> m = new HashMap<>();
        m.put("taste", name);
        kanal.invokeMethod("taste", m);
    }

    private PlaybackState zustand() {
        return new PlaybackState.Builder()
                .setActions(TASTEN)
                .setState(laeuft ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED,
                        PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1f)
                .build();
    }

    private void zeigen() {
        session.setMetadata(new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, titel)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, zeile2)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, zeile3)
                .build());
        session.setPlaybackState(zustand());
        benachrichtigen();
    }

    // -- Benachrichtigung -------------------------------------------------------------

    private void kanalAnlegen() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = (NotificationManager) kontext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null || nm.getNotificationChannel(KANAL) != null) return;
        NotificationChannel k = new NotificationChannel(KANAL, "Uhr (Mediensteuerung)",
                NotificationManager.IMPORTANCE_LOW);
        k.setDescription("Werte der Messung für die Uhr am Handgelenk");
        k.setSound(null, null);
        k.enableVibration(false);
        nm.createNotificationChannel(k);
    }

    private void benachrichtigen() {
        NotificationManager nm = (NotificationManager) kontext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null || session == null) return;
        Notification.Builder b = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(kontext, KANAL)
                : new Notification.Builder(kontext);
        b.setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle(titel)
                .setContentText(zeile2)
                .setSubText(zeile3)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setStyle(new Notification.MediaStyle().setMediaSession(session.getSessionToken()));
        Intent start = kontext.getPackageManager().getLaunchIntentForPackage(kontext.getPackageName());
        if (start != null) {
            b.setContentIntent(PendingIntent.getActivity(kontext, 0, start,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        }
        try {
            nm.notify(BENACHRICHTIGUNG_ID, b.build());
        } catch (SecurityException e) {
            // Android 13+ ohne POST_NOTIFICATIONS: die Session gibt es trotzdem.
            Log.w(TAG, "Benachrichtigung nicht erlaubt", e);
        }
    }
}
