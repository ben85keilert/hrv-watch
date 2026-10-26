# Welche Uhr kann was

Stand 26.10.2026; am Gerät geprüft ist nur die Fit3 (Zeile 1), der Rest ist Aktenlage. Spalte „Anzeige" meint: Kann das
Handy bestimmen, was auf der Uhr steht? Spalte „Sensor direkt": Kann die Uhr den H10 selbst
per BLE verbinden, ohne Handy?

| Gerät | Plattform | Eigene App | Anzeige vom Handy | Sensor direkt | Einschätzung |
|---|---|---|---|---|---|
| **Samsung Galaxy Fit3** (vorhanden) | proprietäres RTOS, nur Galaxy Wearable | nein, kein SDK | nur über Medien-, Benachrichtigungs-, Wetterkanal | nein | **Am Gerät geprüft 26.10.2026:** Medienkanal trägt für drei Textzeilen im Sekundentakt, ▶ und ⏯ funktionieren. Keine Kurve: Blockzeichen fehlen, Cover nur als Hintergrund hinter den Tasten und unzuverlässig geladen. Reicht für HF/RMSSD/Zustand als Text (`TESTPLAN_fit3.md`, ENTSCHEIDUNGEN #5) |
| Samsung Galaxy Watch 4 bis 8, Watch FE | Wear OS (ab 3) | ja, Kotlin/Compose, Watch Face Format | Wearable Data Layer, Komplikationen, Kacheln | ja, BLE-Central mit Standard-APIs | beste Fortsetzung im Samsung-Lager; gebraucht ab ca. 100 € |
| Google Pixel Watch 1 bis 4 | Wear OS | ja | wie oben | ja | gleichwertig, nur Google-Handys sind nicht Pflicht |
| Garmin (Forerunner, Venu, vívoactive) | Connect IQ | Datenfelder, Widgets, Apps (Monkey C) | nur über eigene Connect-IQ-App mit Handy-Kommunikation | Garmin verbindet HRM-Gurte selbst (ANT+/BLE), HRV im Training als Datenfeld möglich | zweiter Ökosystemwechsel; HRV-Logik dann in Monkey C, nicht hrv-core |
| Bangle.js 2 | Espruino (JavaScript), offen | ja, trivial, per Browser | Gadgetbridge oder eigene BLE-Strecke | ja, BLE-Central in JS | billig (ca. 90 €), hackbar, Display schwach, Akku gut |
| PineTime (InfiniTime) | FreeRTOS, offen, C++ | ja, Firmware-Fork | Gadgetbridge (Benachrichtigung, Musik, Wetter) | theoretisch, kein fertiger Code | nur für Firmware-Bastler |
| Apple Watch | watchOS | ja, Swift | ja | ja | eigener Stack, kein Android; hier nicht weiter |
| Polar-Uhren (Vantage, Pacer) | proprietär | nein | nein | H10 ja, aber Anzeige fest | zeigen H10-HF, keine freie Anzeige |

## Was das für den Plan heißt

1. Fit3 ist der Test, ob der Medienkanal trägt. Er kostet nichts, weil das Band da ist.
2. Kommt eine zweite Uhr, dann **Wear OS** (Galaxy Watch oder Pixel Watch). Dort wird aus
   hrv-watch eine echte Uhren-App. Zwei Bauweisen, Entscheidung erst dann:
   - **Anzeige-Client:** Handy rechnet (trainingsapp, hrv-core), Uhr zeigt Seiten über den
     Wearable Data Layer. Gleiche Seitenstruktur wie bei der Fit3. Wenig Code, eine Wahrheit.
   - **Eigenständig:** Uhr verbindet den H10 selbst und rechnet. Der H10 hält zwei
     BLE-Verbindungen, Handy und Uhr könnten parallel mitlesen. Dafür müssten Kennzahlen in
     Kotlin nachgebaut werden, gegen hrv-core-Golden-Master geprüft. Nur, wenn das Handy beim
     Training wirklich stören würde.
3. Weitere Sensoren (H9, Verity Sense, andere 0x180D-Gurte) kommen über trainingsapp herein
   (`docs/geraete_kompatibilitaet.md` dort); hrv-watch sieht nur Seiten und muss nichts wissen.

## Quellen

Fit3: `hrv-research/Geraete/Fit3/README.md` (S1 bis S13). Wear OS: Android-Doku zu Wearable
Data Layer, Complications und Bluetooth LE auf Wear OS. Connect IQ: Garmin Developer, Datenfeld-
Typen. Bangle.js: Espruino-Doku, BLE-Central. PineTime: InfiniTime-Repo. Nichts davon am Gerät
geprüft; Preise sind Erinnerungswerte vom Oktober 2026.
