# hrv-watch

HRV-Werte am Handgelenk anzeigen. Das Handy hält die Verbindung zum Sensor (Polar H9/H10,
später andere) und rechnet; die Uhr zeigt nur an: Herzfrequenz, RMSSD, Intervalle,
Trainingszustand, durchblätterbar, nach Möglichkeit mit den Tasten der Uhr.

> Stand 26.10.2026: Startpaket, am Gerät noch nichts geprüft. Erster Schritt ist der Testplan
> mit der Sonde; danach entscheidet sich, was aus diesem Repo wird.

## Woher das kommt

- Befund zur Galaxy Fit3 und die Anzeige-Idee: `hrv-research/Geraete/Fit3/` (22.10.2026).
- Sensoranbindung, Zonen, Limits, Rechnung: **trainingsapp** (Flet, `flet_ble`, hrv-core).
- Kennzahlen: **hrv-core**. Hier wird nichts nachgerechnet, was dort steht.

## Die drei Wege zur Fit3, in dieser Reihenfolge

Die Fit3 (SM-R390) nimmt keine eigene App und kein freies Textfeld an. Vom Handy übernimmt sie
nur feste Inhaltstypen. Also wird einer davon zum Textkanal.

| Weg | Kanal | Verhalten am Band | Wann |
|---|---|---|---|
| **1** | Mediensteuerung (`MediaSession`): Titel, Interpret, evtl. Album | stumm, dauerhaft, **Tasten ◀ ▶ blättern Seiten, ⏯ startet/stoppt** | zuerst; Sonde testet es |
| **2** | Benachrichtigung mit fester ID | sicher vorhanden, vermutlich jedes Update ein Popup | Rückfall, auch gleichzeitig testbar |
| **3** | Gadgetbridge-Fork (PR #7052: Medien, Wetter, Benachrichtigung) | ohne Galaxy Wearable, Wetter als dritter Textkanal | nur wenn 1 und 2 scheitern |

Was am Band unbelegt ist und morgen entschieden wird: ob die Fit3 Titel/Interpret überhaupt
zeigt (Samsungs Doku nennt nur die Tasten), wie oft Updates ankommen, ob die Medienansicht
offen bleibt. Fragen und Protokollvorlage: [`docs/TESTPLAN_fit3.md`](docs/TESTPLAN_fit3.md).

## Aufbau

```
hrv-watch/
├── README.md
├── CLAUDE.md                 Regeln für Claude Code (Sprache, keine Patientendaten, Rechnung in hrv-core)
├── docs/
│   ├── ENTSCHEIDUNGEN.md     Protokoll, neueste oben
│   ├── TESTPLAN_fit3.md      Test am Gerät: Schritte, Fragen, Protokoll
│   └── geraete.md            Welche Uhr kann was: Fit3, Wear OS, Garmin, Bangle.js, PineTime, Apple
└── sonde/                    Android-Wegwerf-App (Kotlin): Medien- und Benachrichtigungskanal,
                              Simulation oder echter H10, Seiten blättern per Band-Tasten
```

Später dazu, je nach Befund:

- `flet_medien/` — Flet-Erweiterung (Dart + Kotlin) nach dem Muster von `trainingsapp/erweiterungen/flet_ble`,
  die trainingsapp die `MediaSession` gibt. Dann schreibt die trainingsapp die Seiten, mit
  hrv-core-Werten, Zonen und Limits, die sie schon hat. Die Sonde ist danach Geschichte.
- `wearos/` — eigene Uhren-App (Kotlin/Compose for Wear OS) für Galaxy Watch 4+ oder Pixel
  Watch, falls eine solche Uhr dazukommt. Erst dann stellt sich die Frage, ob die Uhr den H10
  selbst verbindet oder nur Anzeige des Handys ist (`docs/geraete.md`).

## Architektur, kurz

```
Polar H10 ──BLE──▶ Handy (trainingsapp: flet_ble → hrv-core → Seiten) ──MediaSession──▶ Galaxy Wearable ──▶ Fit3
                                                                        ◀── Play/Pause, ◀ ▶ ──
```

Eine „Seite" ist ein Tripel aus Titel, Zeile 2, Zeile 3 (Medien: Titel, Interpret, Album).
Die Uhr blättert mit ◀ ▶ durch die Seiten, ⏯ startet oder stoppt die Messung. Dieselben
Seiten gehen unverändert in eine Benachrichtigung oder, später, per Wearable Data Layer an
eine Wear-OS-App. Das Format der Seiten ist der einzige Vertrag zwischen Rechnung und Anzeige;
Zeilenlängen begrenzt das schwächste Gerät (Fit3, Zeichenzahl im Testplan ermitteln).

## Sonde bauen und morgen testen

Anleitung in [`sonde/README.md`](sonde/README.md): `./gradlew assembleDebug` im Terminal,
`adb install`, fertig. Android Studio ist nicht nötig, SDK und JDK 17 sind vom Flet-Build der
trainingsapp schon da. Danach den Testplan abarbeiten und das Protokoll ausfüllen.
