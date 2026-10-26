# Entscheidungen

Protokoll, neueste oben. Status: offen → entschieden → umgesetzt oder verworfen.

| # | Datum | Thema | Status |
|---|---|---|---|
| 3 | 2026-10-25 | Sonde als eigene Kotlin-App, nicht als Flet-Erweiterung | Vorschlag Claude, Ben entscheidet |
| 2 | 2026-10-25 | Rechnung und Sensor bleiben in trainingsapp/hrv-core; hrv-watch ist nur Anzeige | Vorschlag Claude, Ben entscheidet |
| 1 | 2026-10-25 | Eigenes Repo `hrv-watch` statt Ordner in hrv-research oder trainingsapp | Vorschlag Claude, Ben entscheidet |

## 3 — Sonde als eigene Kotlin-App

**Frage:** Die fünf offenen Fragen zur Fit3 (zeigt sie Titel, wie oft, wie lang, bleibt die
Ansicht offen, was bei paralleler Musik) brauchen eine App, die eine `MediaSession` mit
wechselnden Metadaten hält. In trainingsapp einbauen oder getrennt?

**Befund:** trainingsapp spricht Android nur über Flet-Erweiterungen an (`flet_ble` um
`flutter_blue_plus`, `flet_ton` um audioplayers). Eine `MediaSession` bräuchte eine dritte
Erweiterung (`flet_medien`, Dart + Kotlin) und einen vollen `flet build`. Für die Antwort auf
„zeigt das Band Text?" ist das der falsche Hebel: Eine Kotlin-App mit Simulation und optionalem
H10 ist ein Nachmittag, läuft ohne Flutter-Toolchain und lässt sich am Band in Minuten testen.

**Vorschlag:** Sonde in Kotlin (`sonde/`), ausdrücklich Wegwerfcode. Erst wenn Weg 1 oder 2
trägt, kommt `flet_medien` für trainingsapp; die Sonde bleibt als Referenz liegen.

## 2 — Rechnung und Sensor bleiben in trainingsapp/hrv-core

**Frage:** Soll die Handy-Seite von hrv-watch den H10 selbst verbinden und RMSSD rechnen?

**Befund:** trainingsapp hat Sensoranbindung (`flet_ble`, Profile, Prüfstand), hrv-core die
Kennzahlen mit Golden Master, trainingsapp die Zonen und Limits (`core/limits.py`,
`core/hrv_training.py`). Eine zweite Kette in Kotlin wäre eine zweite Wahrheit, genau das, was
hrv-core vermeiden soll.

**Vorschlag:** hrv-watch liefert nur den Anzeigekanal (Erweiterung für trainingsapp, später
Uhren-App). Seiteninhalt bestimmt die trainingsapp. Ausnahme: die Sonde rechnet eine
Vorschau-RMSSD, damit man am Band etwas Plausibles sieht; markiert, nicht produktiv.

## 1 — Eigenes Repo

**Frage:** Wohin mit Sonde, Testprotokoll, späterer Erweiterung und Uhren-App?

**Befund:** hrv-research ist privat und für Literatur, Daten, Notebooks; eine Android-App dort
wäre fremd. trainingsapp ist öffentlich und trägt Erweiterungen in `erweiterungen/`, aber eine
Wear-OS-App und das Gerätewissen zur Fit3 gehören nicht in die Trainings-App. hrv-core ist
reine numpy-Bibliothek.

**Vorschlag:** `hrv-watch`, privat, bis klar ist, was trägt. `flet_medien` kann später als
Ordner nach `trainingsapp/erweiterungen/` umziehen oder von dort als Pfad eingebunden werden,
wie Ben es bei `flet_ble` hält. Befunde zur Fit3 ziehen zusammengefasst nach
`hrv-core/docs/geraete/`.
