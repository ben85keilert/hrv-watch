# Sonde: Wegwerf-App für den Fit3-Test

Eine kleine Android-App (Kotlin, ohne Flutter), die eine `MediaSession` mit wechselnden
Metadaten hält und dieselben Werte wahlweise als Benachrichtigung schickt. Werte kommen aus
einer Simulation oder vom Polar H10 (Standard-Herzfrequenzdienst 0x180D mit RR-Intervallen).
Zweck: die Fragen aus `../docs/TESTPLAN_fit3.md` beantworten. Danach wird sie nicht weiter
gepflegt.

> Der Code wurde ohne Android-Toolchain geschrieben und **nicht gebaut**. Rechne beim ersten
> `Run` mit ein, zwei Compilerhinweisen (fehlender Import, veraltete Signatur); die Logik ist
> absichtlich kurz gehalten, damit das in Minuten erledigt ist.

## Bauen (Android Studio, etwa 15 Minuten)

1. **Neues Projekt:** Android Studio → New Project → **Empty Views Activity** (nicht Compose).
   Name `Sonde`, **Package name `de.hrvwatch.sonde`** (muss genau so heißen), Sprache Kotlin,
   Minimum SDK **26**, Build configuration Kotlin DSL. Fertigstellen, Sync abwarten.
2. **Dateien ersetzen/kopieren:**
   - `app/src/main/AndroidManifest.xml` → durch die Datei hier ersetzen.
   - `app/src/main/java/de/hrvwatch/sonde/MainActivity.kt` → ersetzen; `AnzeigeDienst.kt`,
     `Seiten.kt`, `Rechnung.kt`, `H10Quelle.kt` daneben legen.
   - Die vom Assistenten erzeugte `res/layout/activity_main.xml` kann bleiben, sie wird nicht benutzt.
3. **Abhängigkeit** in `app/build.gradle.kts`, Block `dependencies { … }`, ergänzen:
   ```kotlin
   implementation("androidx.media:media:1.7.0")
   ```
   `androidx.core:core-ktx` und `androidx.appcompat:appcompat` sind im Template schon drin.
   Falls der Assistent `minSdk` nicht auf 26 gesetzt hat: in `defaultConfig` nachziehen.
4. Sync, dann **Run** auf dem Handy (USB-Debugging oder WLAN-Pairing). Beim ersten Start
   Berechtigungen erteilen: Benachrichtigungen, Geräte in der Nähe.
5. Galaxy Wearable → Benachrichtigungen → „hrv-watch Sonde" erlauben.

## Bedienung

| Knopf | Wirkung |
|---|---|
| Start (Simulation) | Vordergrunddienst startet, Werte laufen; HF schwingt 60–80, RR passend |
| Stopp | alles aus, Session beendet |
| Medienkanal an/aus | `MediaSession` + Medien-Notification aktualisieren (Voreinstellung: an) |
| Benachrichtigung an/aus | zusätzlich Werte als normale Benachrichtigung mit fester ID (Voreinstellung: aus) |
| Takt 1 s / 2 s / 5 s | Aktualisierungsabstand |
| Nächste Seite | wie ▶ am Band |
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
| 3 | `Zustand` | `ruhig, HF 72, RMSSD 43 ms` (längste Zeile, für F4) | Stempel |

Die Stempelnummer zählt jede Aktualisierung; springt sie am Band, wurden Updates verschluckt.

## Was absichtlich fehlt

- Artefaktkorrektur, saubere RMSSD, Zonen nach Alter oder Schwellen: macht später trainingsapp
  mit hrv-core.
- Neuverbinden mit Backoff, Bonding, PMD: nicht nötig für den Test.
- Layout-XML, Icons, Themes: die Oberfläche ist programmatisch, Icons sind Systemicons.
