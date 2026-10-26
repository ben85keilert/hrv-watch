# CLAUDE.md

Hinweise für Claude Code in diesem Repository.

## Was dieses Repo ist

Anzeige von HRV-Werten am Handgelenk. Das Handy (trainingsapp) hält den Sensor und rechnet mit
hrv-core; hier entsteht, was die Werte auf eine Uhr bringt: zuerst die Samsung Galaxy Fit3 über
die Mediensteuerung, später eigene Uhren-Apps. Nachbarrepos: `../trainingsapp` (Flet-App,
`erweiterungen/flet_ble`), `../hrv-core` (Kennzahlen, Gerätewissen in `docs/geraete/`),
`../hrv-research` (privat, Befunde und Literatur).

## Sprache

Code und Bezeichner englisch, wenn es die verbreitete Schreibweise ist (Android-APIs, Kotlin,
Dart); eigene Klassen und Funktionen dürfen deutsch heißen, wie in trainingsapp. Kommentare,
Docstrings, Dokumentation, Antworten und Commit-Messages deutsch.

## Regeln

1. **Keine Patientendaten.** Hier gibt es keinen Datenordner. Tests laufen mit Simulation oder
   Bens eigenem Sensor. Nie Messdateien, Namen oder Freitexte aus anderen Repos hierher kopieren.
2. **Rechnung in hrv-core.** Kennzahlen (RMSSD, Zonen, Limits) rechnet hrv-core bzw. die
   trainingsapp. Die Sonde enthält eine Vorschau-RMSSD ohne Artefaktkorrektur, ausdrücklich als
   Wegwerfcode gekennzeichnet; sie wandert nicht in produktiven Code.
3. **Gerätewissen** (was die Fit3 anzeigt, wie oft, wie lang) nach jedem Test in
   `docs/TESTPLAN_fit3.md` protokollieren; die Zusammenfassung zieht nach `hrv-core/docs/geraete/`.
4. **Entscheidungen** in `docs/ENTSCHEIDUNGEN.md`, neueste oben: Frage, Befund, Entscheidung,
   Folge. Nichts stillschweigend in trainingsapp oder hrv-core ändern.
5. **Branches.** Gearbeitet wird auf `main`; Ben arbeitet allein.
6. **Keine Gewichte, keine Keystores, keine `local.properties`** committen. Android-Buildordner
   (`build/`, `.gradle/`, `.idea/`) bleiben draußen (`.gitignore`).

## Befehle

```bash
# Sonde bauen und aufs Handy (sonde/README.md); kein Android Studio nötig
cd sonde && ./gradlew assembleDebug && ~/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
# flet_medien: wird von ../trainingsapp eingebunden (pyproject.toml dort); Tests laufen dort:
#   cd ../trainingsapp && uv run pytest tests/test_uhr_seiten.py tests/test_uhr_kanal.py
```
