# flet-medien

Flet-Erweiterung (Flet ≥ 1.0.3) für die trainingsapp: **drei Textzeilen als Mediensteuerung**
(`MediaSession`, Android). Android zeigt Titel, Interpret und Album einer aktiven Session in
der Mediensteuerung, Galaxy Wearable reicht sie an die Galaxy Fit3 weiter. Die Zeilen sind
frei: HF, RMSSD, Intervall-Kennzahlen. Die Tasten des Bands (▶ ◀ ⏯) kommen nach Python.

Befund und Grenzen: `../docs/TESTPLAN_fit3.md` (26.10.2026), Entscheidung: `../docs/ENTSCHEIDUNGEN.md` #6.
Vorlage war die Sonde (`../sonde`, `AnzeigeDienst.kt`), hier ohne Dienst, Simulation und Rechnung.

- Flutter-Seite: `src/flutter/flet_medien` (Service `FletMedienService`, Android-Plugin
  `FletMedienPlugin.java`, nur Android-Framework, keine androidx.media-Abhängigkeit)
- Python-Seite: `src/flet_medien` (`FletMedien`, ein `ft.Service`)

```python
from flet_medien import FletMedien, verfuegbar

if verfuegbar(page):                              # nur in der gebauten Android-App
    def taste(e): print(e.taste)                  # play | pause | stop | weiter | zurueck
    uhr = FletMedien(on_taste=taste)              # in main(page) anlegen, Referenz behalten!
    page.services.append(uhr)
    await uhr.starten("HF 122 ↑", "RMSSD 17 ms", "Aufnahme 12 min")
    await uhr.zeigen("Gipfel 141", "Tal 95, vor 0:45", "Aufnahme 12 min")  # nur bei Änderung
    await uhr.zustand(False)                      # PAUSED: das Band zeigt ▶ statt ⏸
    await uhr.warnen("Gurt prüfen", "23 % Artefakte")  # Benachrichtigung, Band vibriert
    await uhr.beenden()
```

## Was das Band tut (Fit3, 26.10.2026)

- Zeigt die drei Zeilen und aktualisiert im Sekundentakt; die Ansicht bleibt offen.
- **Jede Metadaten-Änderung setzt die Laufschrift zurück.** Deshalb `zeigen` nur bei
  geändertem Text rufen und Zeilen kurz halten (`trainingsapp.core.uhr_seiten`, 20 Zeichen).
- Meldet die Session immer PLAYING, schickt das Band bei ⏯ nur `pause`. Mit ehrlichem
  `zustand(laeuft)` wechselt es zwischen `play` und `pause`.
- `zurueck` kam am Fit3 nie an; `weiter` zuverlässig.
- Kurven gehen nicht: Blockzeichen fehlen in der Schrift, ein Albumcover liegt hinter den
  Tasten und wird nur beim Öffnen der Ansicht geladen. Darum keine Bild-Schnittstelle hier.

## Einbindung in die trainingsapp

Wie hrv-core als Schwesterordner: `[tool.uv.sources] flet-medien = { path = "../hrv-watch/flet_medien", editable = true }`
und `[tool.flet.dev_packages] flet-medien = "../hrv-watch/flet_medien"`. CI und APK-Workflow
checken hrv-watch am Commit aus `hrv-watch.ref` neben die trainingsapp aus.

Stolperfallen (wie bei flet_ble):

- **Referenz halten.** Flet entfernt Dienste, auf die in Python niemand mehr zeigt.
- **Nur in der gebauten App.** `flet run` kennt den Dart-Teil nicht; `verfuegbar(page)` prüft das.
- **Benachrichtigung.** Ohne `POST_NOTIFICATIONS` (Android 13+) bleibt die Medien-Benachrichtigung
  unsichtbar; die Session gibt es trotzdem. Die trainingsapp fragt das Recht beim Messdienst ab.
