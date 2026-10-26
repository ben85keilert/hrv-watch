# Entscheidungen

Protokoll, neueste oben. Status: offen → entschieden → umgesetzt oder verworfen.

| # | Datum | Thema | Status |
|---|---|---|---|
| 7 | 2026-10-26 | Seiten am Band: welche Werte, Auswahl in den Einstellungen, ⏯ je Seite, Gurt-Warnung | umgesetzt (trainingsapp 0.3.22, am Gerät offen) |
| 6 | 2026-10-26 | Uhranbindung: Werte aus der trainingsapp, Kanäle statt Geräte, Uhren-Apps in hrv-watch | umgesetzt (a–d, 26.10., am Gerät bestätigt: Seiten, ▶, ⏯-Marke); Wear OS offen |
| 5 | 2026-10-26 | Fit3 bleibt Textanzeige; Kurve braucht eine Wear-OS-Uhr | Vorschlag Claude, Ben entscheidet |
| 4 | 2026-10-26 | Sonde aus dem Terminal bauen (Gradle-Gerüst im Repo), kein Android Studio | umgesetzt |
| 3 | 2026-10-25 | Sonde als eigene Kotlin-App, nicht als Flet-Erweiterung | Vorschlag Claude, Ben entscheidet |
| 2 | 2026-10-25 | Rechnung und Sensor bleiben in trainingsapp/hrv-core; hrv-watch ist nur Anzeige | Vorschlag Claude, Ben entscheidet |
| 1 | 2026-10-25 | Eigenes Repo `hrv-watch` statt Ordner in hrv-research oder trainingsapp | Vorschlag Claude, Ben entscheidet |

## 7 — Seiten am Band: welche Werte, Auswahl, ⏯ je Seite

**Frage (Ben, 26.10.):** Welche Werte lohnen sich am Handgelenk für die Trainingssteuerung,
und wie wählt man sie? HF ist gesetzt. RMSSD schwankt akut stark und taugt eher für die
Auswertung; SDNN braucht lange Abschnitte. HF-Bereiche sind noch nicht entschieden (Maximal-HF
gemessen oder Formel, Ruhe-HF eingetragen), also vorerst Rohwerte. Später HRR30/HRR60 aus
hrv-research.

**Bens Vorschlag:** 1 HF mit RR darunter · 2 Atemfrequenz und RMSSD (Atemübungen) · 3 DFA α1 ·
4 HF-Gipfel und -Tal · 5 HRR30 / HRR60. ⏯ je Seite: auf 1–3 Marke, auf 4 Gipfel und Tal
zurücksetzen, auf 5 den Beginn der Erholung setzen.

**Claudes Anmerkungen:**

- ⏯ je Seite ist technisch einfach: der Kanal kennt die Seite und verteilt die Taste. Es ist
  auch der einzige Weg zu mehr Funktionen, denn das Band liefert zuverlässig nur ▶ und ⏯. Damit
  die Taste nicht auf der falschen Seite wirkt, nennt Zeile 3 auf jeder Seite, was ⏯ tut
  („⏯ Marke“, „⏯ Neu“, „⏯ Start“), und der Effekt ist sofort am Band sichtbar.
- Seite 4: Gipfel und Tal **seit dem letzten ⏯** (bzw. seit Verbinden), nicht mehr nur die
  letzten 150 s; dazu „vor m:ss“ des Gipfels. Die HF-Reihe reicht so weit wie der Live-Puffer
  (Einstellung Live-Verlauf, Vorgabe 30 min).
- Seite 5: ⏯ setzt t0 = Ende der Belastung; HRR30 = HF(t0) − HF(t0+30 s), HRR60 analog, HF
  jeweils als 5-s-Mittel. Bis dahin zählt die Seite hoch („HRR30 in 12 s“). Derselbe Druck
  setzt eine Notiz „HRR“ in die Aufnahme, damit die Auswertung den Punkt kennt. Fenster und
  Mittelung gleichen wir mit hrv-research ab, bevor der Wert in die Auswertung wandert.
- Seite 2: Atemfrequenz live gibt es noch nicht; hrv-core hat `atmung.atemfrequenz` für
  Fenster (Modus Atemübung). Nötig ist `LiveFenster.atemfrequenz()` wie `dfa_a1()`: letzte 60 s,
  alle 5 s. Unter Belastung „–“ statt Zahl.
- Vergessen? Zwei Kandidaten, beide keine HRV: der Timer-Abschnitt beim Intervallplan
  („Abschnitt 3/8, Rest 0:45“) und das GPS-Tempo. Und ein Gurt-Hinweis in Zeile 3, wenn der
  Artefaktanteil steigt („Gurt prüfen“), weil ein rutschender Gurt DFA α1 entwertet.
- Einstellung: Kontrollkästchen je Seite unter „Uhr am Handgelenk“, HF immer an, Reihenfolge
  fest, ▶ blättert nur durch die angekreuzten.

**Entscheidung (Ben, 26.10.):** Timer-Abschnitt und GPS-Tempo kommen mit. Die Gurt-Warnung
ist kein Blatt, sondern eine Meldung: die trainingsapp warnt selbst und reicht sie an die
Uhr weiter. Einstellung wie vorgeschlagen. Seiten zusammenlegen, wo es geht.

**Umsetzung (26.10., Claude, trainingsapp 0.3.22):** fünf Seiten in `core/uhr_seiten.py`:
1 `HF 148 ↑ | RR 492 · α1 0,62`, 2 `Atmung 6,0/min | RMSSD 45 ms`, 3 `Gipfel 141 | Tal 95, vor
0:45 | ⏯ neu ab jetzt`, 4 `HRR30 17 | HRR60 in 20 s | ab HF 160 · ⏯ neu`, 5 `Belastung 2/8 |
Rest 0:45 · 5:12/km | 2,3 km · 12 min`. ⏯ je Seite (`TASTE_JE_SEITE`), Kontrollkästchen unter
„Uhr am Handgelenk“ (`HrvEinstellungen.uhr_seiten`). Atemfrequenz live in hrv-core
(`LiveFenster.atemfrequenz`, 60 s, Modus Atemübung). Gurt-Warnung: `LiveFenster.artefaktanteil`
(60 s) → `core/gurt_warnung.Artefaktwache` im Messlauf (ab 10 %, Entwarnung unter 5 %, Alarm
höchstens alle 2 min) → rote Zeile in der Statuskarte der Messung, Zeile 3 am Band, und
`flet_medien.warnen` als Benachrichtigung mit Vibration (Kanal „Warnungen der Messung“; in
Galaxy Wearable müssen Benachrichtigungen der trainingsapp an sein). Am Gerät noch nicht geprüft.

---

## 6 — Uhranbindung: Werte aus der trainingsapp, Kanäle statt Geräte

**Frage:** HF, RR, RMSSD und DFA α1 sollen vom Handy auf die Uhr. Eigene App oder Teil der
trainingsapp? Wird die trainingsapp zu groß, wenn jede Uhr eingebaut wird?

**Befund:** Den Gurt hält die trainingsapp (`devices/messlauf.py` → `hrv_core.live.LiveFenster`
→ `LiveKennzahlen` mit `hf_bpm`, `rr_ms`, `rmssd_30s`, `hf_reihe()`). Eine zweite App bräuchte
entweder eine zweite BLE-Verbindung zum H10 (zweite Rechnung, zweite Wahrheit, verstößt
gegen #2) oder eine App-zu-App-Schnittstelle, die mehr Code wäre als die Anbindung selbst.
DFA α1 gibt es in hrv-core nur für fertige Aufnahmen (`nonlinear.compute_dfa_alpha1`,
`core/hrv_training.py` je Kilometer), nicht live. Die trainingsapp hat 74 000 Zeilen; die
Erweiterungen `flet_ton` (190 Zeilen) und `flet_ble` (800) zeigen, was ein Kanal kostet.

**Vorschlag:** drei Schichten, jede mit genau einer Aufgabe.

1. **Werte** (hrv-core, trainingsapp): `LiveKennzahlen` um `dfa_a1_120s` ergänzen, gleitendes
   2-Minuten-Fenster über die korrigierten RR, alle 5 s neu, wie bei fertigen Aufnahmen gerechnet.
   Das ist eine hrv-core-Änderung mit Golden-Master-Test, nicht in der App nachgebaut.
2. **Seiten** (trainingsapp `core/uhr_seiten.py`, reines Python, kein flet): baut aus
   `LiveKennzahlen` und dem HF-Verlauf ein geräteunabhängiges Paket: Textseiten (Titel,
   Zeile 2, Zeile 3, Zeilenlänge als Parameter) für Textkanäle, dazu Zahlen und die HF-Reihe der
   letzten 150 s für Grafikkanäle. Enthält die Intervall-Kennzahlen (HF mit Tendenz, Maximum des
   letzten Laufs, Minimum der Pause, Sekunden seit dem Gipfel). Testbar ohne Gerät.
3. **Kanäle** (Flet-Erweiterungen in hrv-watch, von trainingsapp wie hrv-core als Schwesterordner
   eingebunden): je Übertragungsweg eine, nicht je Uhr.
   - `flet_medien` (MediaSession, Android): Kotlin aus der Sonde (`AnzeigeDienst`) minus
     Simulation, H10 und Rechnung. Bedient die Fit3 und jedes Band mit Mediensteuerung.
     Tasten zurück an die App: ▶ blättert, ⏯ setzt eine Marke (`abschnitt_markieren`).
     **Nie** Aufnahme starten oder stoppen (Ben, 26.10.).
   - `flet_wear` (Wearable Data Layer) später, wenn eine Wear-OS-Uhr da ist. Die Uhren-App
     selbst (`wearos/`) lebt in hrv-watch, nie in der trainingsapp.

   In der trainingsapp bleibt: ein Modul `core/uhr_seiten.py`, ein Beobachter am `Messlauf`,
   der jede Sekunde die Seiten an die aktiven Kanäle gibt (in `ui/`, weil Flet-Dienste), ein
   Schalter „Uhr“ in den Einstellungen. Geschätzt 300 bis 400 Zeilen plus Tests. Gerätewissen
   (Fit3: Zeichen je Zeile, kein Cover) steht in hrv-watch, nicht in der trainingsapp.

**Reihenfolge:** (a) `uhr_seiten.py` mit Tests, (b) `flet_medien` aus der Sonde, (c) Beobachter
und Schalter, (d) DFA α1 live in hrv-core, (e) Wear OS, wenn eine Uhr da ist.

**Entscheidung (Ben, 26.10.):** so bauen. ⏯ setzt eine Marke, auf keinen Fall Aufnahme
starten oder stoppen. `flet_medien` liegt in hrv-watch (`hrv-watch/flet_medien/`), das ist
übersichtlicher als ein vierter Ordner unter `trainingsapp/erweiterungen/`.

**Einbindung in die trainingsapp**, nach dem Muster von hrv-core (Schwesterordner):
`[tool.uv.sources] flet-medien = { path = "../hrv-watch/flet_medien", editable = true }` für
die Entwicklung und `[tool.flet.dev_packages] flet-medien = "../hrv-watch/flet_medien"` für
`flet build`; `flet build` nimmt Python- und Flutter-Teil von dort, wie bei `flet_ble`. Für CI
und APK-Workflow eine Datei `hrv-watch.ref` mit dem Commit-SHA und ein Checkout-Schritt wie für
hrv-core. hrv-watch ist auf GitHub **privat** (hrv-core ist öffentlich und braucht kein
Token): die Workflows der trainingsapp brauchen für den Checkout ein Secret, etwa ein
feingranulares PAT nur mit Leserecht auf hrv-watch, als `token:` im Checkout-Schritt.

**Umsetzung (26.10., Claude):** (a) `trainingsapp/core/uhr_seiten.py` mit Tests, (b)
`hrv-watch/flet_medien` (Python, Dart, Java; MediaSession ohne androidx), (c)
`ui/geraete/flet_medien_kanal.py` mit Takt, Tasten und Marke, Schalter unter Einstellungen →
HRV-Messung → „Uhr am Handgelenk“, `pyproject.toml`, `hrv-watch.ref`, CI- und APK-Workflow.
(d) `LiveFenster.dfa_a1()` in hrv-core (200 gute Schläge, Rechnung wie in der Aufzeichnung,
Artefaktgrenze 3 %), im Kanal alle 5 s und nur bei EKG-Gurten. Die Session meldet vorerst
immer PLAYING, damit ⏯ zuverlässig als Marke ankommt. **Am Gerät (26.10., trainingsapp 0.3.19):**
die Testseite aus den Einstellungen kam am Band an, der Kanal Python → Dart → MediaSession →
Galaxy Wearable trägt also. Werte kamen erst mit 0.3.20: der Kanal hatte `jetzt_s` am
Messlauf als Attribut statt als Methode gelesen, und die Testattrappe hatte den Fehler
mitgemacht; seitdem läuft ein Test gegen den echten Messlauf mit Simulator. 0.3.20 stürzte beim
ersten ▶ ab (Flet löst `on_taste` mit String-Annotationen nicht auf, wie flet_ble), 0.3.21: Seiten,
▶ und ⏯-Marke am Band bestätigt (Ben, 26.10.). Offen: (e) Wear OS.

**Folge:** Die trainingsapp kennt Kanäle, keine Uhren. Jede neue Uhr kostet entweder nichts
(zeigt Medien oder Benachrichtigungen) oder eine App in hrv-watch plus höchstens einen Kanal.

## 5 — Fit3 bleibt Textanzeige; Kurve braucht eine Wear-OS-Uhr

**Frage:** Ben will beim Intervalllauf den HF-Verlauf der letzten 2,5 Minuten als Kurve am
Handgelenk sehen (Anstieg auf ~140, Pause, Abfall, nächster Lauf). Geht das mit der Fit3, und
wie weit lässt sich ihre Anzeige überhaupt beeinflussen?

**Befund (Testlauf 26.10., `TESTPLAN_fit3.md`):** Die Fit3-Firmware ist geschlossen, kein SDK,
keine Apps, kein Zifferblatt-Format. Vom Handy kommen nur feste Inhaltstypen an, die das Band
in festen Masken zeigt. Für die Mediensteuerung heißt das: drei Textzeilen, Tasten, Cover als
Hintergrund hinter den Tasten. Text trägt (F1 bis F5). Eine Kurve als Text scheitert an der
Schrift (Blockzeichen fehlen, ASCII zu grob), eine Kurve als Cover an Maske und Ladeverhalten
(nur beim Öffnen der Ansicht geladen, unzuverlässig, Tasten davor). Gadgetbridge (Weg 3)
ersetzt nur Galaxy Wearable, nicht die Masken des Bands; es brächte höchstens den Wetterkanal
als weitere Textfläche.

**Vorschlag:**
1. Fit3 für das, was sie kann: HF, RMSSD, Zustand als Text, Seiten per ▶, ⏯ als Start/Stopp.
   Dafür `flet_medien` in der trainingsapp (Entscheidung #3). Statt Kurve Kennzahlen des
   Intervalls als Text: aktuelle HF mit Tendenz, Maximum des letzten Laufs, Minimum der
   Pause, Sekunden seit dem Gipfel. Das deckt die Frage „zu schnell oder zu langsam" ohne Grafik.
2. Für die Kurve eine Wear-OS-Uhr (Galaxy Watch 4 oder neuer, Pixel Watch; gebraucht ab etwa
   100 €). Eigene App mit ganzem Bildschirm, Kurve aus den Werten des Handys über den Wearable
   Data Layer, später wahlweise eigener H10-Zugriff (`geraete.md`). Erst dann wird die Uhr ein
   HRV-Werkzeug statt einer Anzeige.

**Folge:** Sonde bleibt als Referenz für den Medienkanal liegen; Kurven-Code darin (Kurve.kt,
Seiten 3 und 4) ist dokumentiertes Negativergebnis. Zusammenfassung für
`hrv-core/docs/geraete/galaxy_fit3.md` folgt, sobald F8 und Teil 2 durch sind.

## 4 — Sonde aus dem Terminal bauen

**Frage:** Die Sonde-Anleitung sah ein frisches Android-Studio-Projekt vor, in das die
Kotlin-Dateien kopiert werden. Ben arbeitet in PyCharm; braucht es Android Studio?

**Befund:** Der Flet-Build der trainingsapp hat alles Nötige hinterlassen: Android-SDK mit
Plattformen 34–36 und Build-Tools in `~/Android/sdk`, adb, JDK 17 in `~/java`, Gradle 8.14
samt Android-Gradle-Plugin im Gradle-Cache. Es fehlte nur das Gradle-Gerüst, das sonst der
Android-Studio-Assistent erzeugt: vier Build-Dateien und der Wrapper. Das System-Java 25 kann
Gradle 8.14 nicht ausführen, daher `org.gradle.java.home` auf das JDK 17.

**Entscheidung:** Gerüst von Hand ins Repo (`sonde/settings.gradle.kts`, `build.gradle.kts`,
`app/build.gradle.kts`, `gradle.properties`, `gradle/wrapper/`), Bauen mit `./gradlew
assembleDebug`, Installieren mit `adb`. Das Manifest nutzt ein Systemicon statt eigener
Mipmap-Dateien. Erster Build am 26.10.2026 ohne Fehler und Warnungen.

**Folge:** Android Studio bleibt optional. Die Sonde-README beschreibt den Terminalweg;
`local.properties` mit `sdk.dir` ist rechnerabhängig und bleibt ungetrackt (`.gitignore`).
PyCharm bietet für Kotlin keine Vervollständigung, Fehler zeigt der Gradle-Build.

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
