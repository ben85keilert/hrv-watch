# Sonde: Wegwerf-App für den Fit3-Test

Eine kleine Android-App (Kotlin, ohne Flutter), die eine `MediaSession` mit wechselnden
Metadaten hält und dieselben Werte wahlweise als Benachrichtigung schickt. Werte kommen aus
einer Simulation oder vom Polar H10 (Standard-Herzfrequenzdienst 0x180D mit RR-Intervallen).
Zweck: die Fragen aus `../docs/TESTPLAN_fit3.md` beantworten. Danach wird sie nicht weiter
gepflegt.

## Bauen und installieren (Terminal, ohne Android Studio)

Das Gradle-Gerüst liegt im Repo (`settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`,
Wrapper). Gebraucht werden nur ein Android-SDK und ein JDK 17; wer die trainingsapp per
`flet build` baut, hat beides schon (bei Ben `~/Android/sdk` und `~/java/17.0.13+11`). Die
Pfade sind rechnerabhängig und bleiben außerhalb des Repos: `sdk.dir` in der nicht committeten
`local.properties` neben dieser README, `org.gradle.java.home` in `~/.gradle/gradle.properties`.
Letzteres ist nötig, weil Gradle 8.14 nicht auf Java 25 läuft.

```bash
cd sonde
./gradlew assembleDebug                          # erster Lauf lädt Abhängigkeiten, ~1 min
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat -s AndroidRuntime:E                   # nur falls die App abstürzt
```

`adb` liegt in `~/Android/sdk/platform-tools/`. Handy per USB mit USB-Debugging oder per
WLAN-Pairing (Entwickleroptionen → Drahtloses Debugging, `adb pair`, `adb connect`).

Beim ersten Start Berechtigungen erteilen: Benachrichtigungen, Geräte in der Nähe. Dann
Galaxy Wearable → Benachrichtigungen → „hrv-watch Sonde" erlauben.

PyCharm kann die `.kt`-Dateien nur als Text bearbeiten (kein Kotlin-Plugin); Fehler meldet
erst `./gradlew assembleDebug`. Stand 26.10.2026 baut der Code ohne Fehler und Warnungen.
Wer Android Studio hat, öffnet `sonde/` direkt als Projekt; nötig ist es nicht.

## Bedienung

| Knopf | Wirkung |
|---|---|
| Start (Simulation) | Vordergrunddienst startet, Werte laufen; HF pendelt 90–140 wie beim Intervalllauf (Periode ~60 s), RR passend |
| Stopp | alles aus, Session beendet |
| Medienkanal an/aus | `MediaSession` + Medien-Notification aktualisieren (Voreinstellung: an) |
| Benachrichtigung an/aus | zusätzlich Werte als normale Benachrichtigung mit fester ID (Voreinstellung: aus) |
| Takt 1 s / 2 s / 5 s | Aktualisierungsabstand |
| Nächste Seite | wie ▶ am Band |
| Stempel an/aus | Zeile 3 mit Stempel (Voreinstellung) oder konstant „hrv-watch“; ohne Stempel ändert sich der Text seltener, und unveränderter Text wird nicht neu gesendet (Test F4: Laufschrift) |
| Bild (Albumcover) an/aus | HF-Kurve der letzten 150 s als Albumcover in Metadaten und Medien-Notification (Voreinstellung: aus). Test F9: zeigt das Band Cover? |
| Stille abspielen an/aus | lautloser Audio-Loop, falls das Band Medien nur bei echter Wiedergabe zeigt |
| Polar H10 verbinden | sucht ein Gerät mit 0x180D, verbindet, abonniert 0x2A37; Simulation aus |

Die Textfläche darunter zeigt Zustand und Protokoll (letzte 40 Zeilen): Band-Tastendrücke
kommen als „Band: Play / Pause / weiter / zurück" an.

## Seiten

| Seite | Titel | Zeile 2 | Zeile 3 |
|---|---|---|---|
| 0 | `HF 72` | `RMSSD 43 ms` | Stempel `#Nr Sekunden Takt` |
| 1 | `RMSSD 43 ms` | `60 s, 58 RR` | Stempel |
| 2 | `RR 812 ms` | `n=1234` | Stempel |
| 3 | `············▅▆▆▅▄▃▃▄` (HF-Kurve 150 s, 20 Blockzeichen à 7,5 s, Skala 60–160) | `HF 108, 150 s` | Stempel |
| 4 | `Zustand` | `ruhig, HF 72, RMSSD 43 ms` (längste Zeile, für F4) | Stempel |

Die Stempelnummer zählt jede Aktualisierung; springt sie am Band, wurden Updates verschluckt.

Bandtasten: ▶ ◀ blättern. ⏯ schickt Pause bzw. Play je nach gemeldetem Zustand; die Sonde
meldet PLAYING, solange Werte laufen, sonst PAUSED (400 ms Entprellung). Jede Metadaten-Änderung
setzt am Band die Laufschrift zurück, deshalb sendet die Sonde nur, wenn sich der Text geändert
hat; das Cover höchstens alle 10 s.

## Was absichtlich fehlt

- Artefaktkorrektur, saubere RMSSD, Zonen nach Alter oder Schwellen: macht später trainingsapp
  mit hrv-core.
- Neuverbinden mit Backoff, Bonding, PMD: nicht nötig für den Test.
- Layout-XML, Icons, Themes: die Oberfläche ist programmatisch, Icons sind Systemicons.
