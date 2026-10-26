# Testplan: Galaxy Fit3 als Anzeige für H10-Werte

Ziel des ersten Tests: die offenen Fragen aus `hrv-research/Geraete/Fit3/anzeige_idee.md` am
Gerät beantworten. Dauer etwa eine Stunde. Ergebnisse unten eintragen, auch die negativen.

## Vorbereitung

1. Fit3 per Galaxy Wearable gekoppelt, Firmware aktuell (Galaxy Wearable → Band-Software).
2. Am Band: **Einstellungen → Anzeige → Mediensteuerung anzeigen** ein.
3. Galaxy Wearable → Benachrichtigungen: die Sonde („hrv-watch Sonde") erlauben, sobald sie
   installiert ist; „Benachr. auf Telefon stummschalten" aus (so heißt die Option in Galaxy
   Wearable 2.2.72), sonst kommt beim Testen mit dem Handy in der Hand nichts an.
4. Sonde installiert (`sonde/README.md`), Berechtigungen beim ersten Start erteilt
   (Benachrichtigungen, Geräte in der Nähe).
5. Polar H10 angelegt und angefeuchtet, falls Teil 3 getestet wird. Keine andere App mit
   H10-Verbindung offen (Polar Flow, trainingsapp), sonst taucht er im Scan nicht auf.

## Teil 0: Nullmessung ohne Sonde (5 min)

Beliebige Musik-App starten, Band ansehen.

- [ ] Erscheint die blaue Note? Tippen: zeigt die Steuerung **Titel und Interpret** oder nur Tasten?
- [ ] Titel in der Musik-App wechseln: aktualisiert das Band ohne Verzögerung?
- [ ] Wie viele Zeichen passen in eine Zeile, bevor abgeschnitten oder gescrollt wird?

Wenn hier schon kein Text erscheint, ist Weg 1 tot; dann direkt zu Teil 2.

## Teil 1: Medienkanal mit Simulation (20 min)

Sonde: **Start (Simulation)**. Medienkanal ist voreingestellt an, Takt 1 s. Jede Seite trägt
in Zeile 3 einen Stempel `#Nr Sekunden Takt`; daran sieht man verschluckte Updates.

| Frage | Vorgehen | Ergebnis |
|---|---|---|
| F1 Zeigt das Band Titel/Interpret/Album der Sonde? | Steuerung öffnen | **Ja** (26.10.): Titel, Interpret und Album erscheinen, auch Seite „Zustand". Ohne Stille-Loop, Zustand PLAYING reicht. |
| F2 Kommen Updates im 1-s-Takt an? | 30 s zuschauen, Nummern vergleichen; dann **Takt 2 s**, **Takt 5 s** | **Ja** (26.10.): nach Start wechselt der Wert am Band im Sekundentakt. Ohne Änderung am Handy ändert sich am Band nichts (Nullmessung). Takt 2 s / 5 s offen. |
| F3 Bleibt die Medienansicht offen oder fällt das Band aufs Zifferblatt zurück? Nach wie vielen Sekunden? Hilft Always-on / längeres Display-Timeout? | Band nicht berühren, Stoppuhr | **Bleibt offen** (26.10.). |
| F4 Zeilenlänge: ab wann abgeschnitten oder Laufschrift? | Seite „Zustand" hat die längste Zeile | **Laufschrift, aber jede Aktualisierung setzt sie zurück** (26.10.): „ruhig“ steht, die Zeile läuft 3–4 Zeichen weiter, dann lädt das Band neu (Stempel in Zeile 3 ändert sich jede Sekunde). Die ganze Zeile wird erst nach mehreren Durchläufen lesbar. Folge: Zeilen so kurz, dass sie ohne Laufschrift passen, und nur bei geändertem Text senden. Sonde hat dafür jetzt „Stempel an/aus“ und sendet unveränderten Text nicht mehr; Zeichenzahl pro Zeile noch zu messen. |
| F5 Funktionieren ◀ ▶ als Blättern (Seite wechselt), ⏯ als Start/Stopp (Protokoll der Sonde zeigt „Band: …")? | Tasten am Band drücken | **▶ ja, ⏯ halb** (26.10.): ▶ blättert, im Protokoll „Band: weiter“; „zurück“ kam nie an (hat das Band eine ◀-Taste?). Solange die Sonde immer PLAYING meldete, kam bei ⏯ nur „Pause“, nie „Play“: die Sonde hielt an, am Band kein sichtbarer Unterschied. Seit die Sonde PAUSED bei Stopp meldet, schickt das Band abwechselnd „Play“ und „Pause“ (Protokoll 07:47): **⏯ funktioniert als Start/Stopp.** |
| F6 Parallel echte Musik starten: welche Session zeigt das Band? Kommt die Sonde zurück, wenn die Musik pausiert? | Spotify o. ä. starten, pausieren | **Sonde bleibt** (26.10.): Am Band ist die HRV-Anzeige nie weg, auch mit Qobuz. Beim Start von Qobuz lud das Band die Sonden-Session neu und zeigte dabei kurz das Albumcover als Hintergrund. Was das Band während laufender Musik als Titel zeigt (Qobuz oder HF): noch nachfragen. |
| F7 Zeigt das Band die Sonde nur, wenn wirklich Ton läuft? | Falls bei F1 nichts erscheint: **Stille abspielen** einschalten und F1 wiederholen | **Entfällt**: F1 ging ohne Ton. |
| F8 Handy gesperrt, in der Tasche, 10 min: läuft die Anzeige weiter? | Akkuoptimierung für die Sonde ggf. ausschalten | offen |
| F9a Kurve als Text: zeigt das Band Blockzeichen (▁▂▃▅▆▇█) in einer Zeile? | Seite „Kurve“ (Seite 3) | **Nein** (26.10.): Blockzeichen werden Kästchen, die Schrift des Bands hat sie nicht. ASCII-Stufen `_.-'^` werden angezeigt, sind aber unbrauchbar: drei unterscheidbare Höhen ergeben keine Kurve. **Kurve als Text ist tot.** |
| F9b Kurve als Bild: zeigt das Band ein Albumcover? Bleibt es, wird es nachgeladen? | Seite „Bild“ (Seite 4), **Stempel aus**, **Bild (Albumcover) an**; Cover kommt alle 10 s mit Nummer #n rechts oben | **Ja, als Hintergrund der Mediensteuerung** (26.10.). Mit konstantem Text und 10-s-Covertakt: Das Bild steht, die Nummer zählt hoch, **aber erst nach Display aus und wieder an**; das Band lädt das Cover beim Öffnen der Ansicht, nicht während sie offen ist. Bei PLAYING flackert der Hintergrund bei jedem Coverupdate an und aus; bei PAUSED bleibt er eingefroren, solange das Display an ist. Folge: ein Live-Bild im 10-s-Takt flackert; brauchbar als Bild, das beim Blick aufs Band frisch ist (Display geht ja ohnehin aus), oder mit längerem Takt. |

## Teil 2: Benachrichtigungskanal (10 min)

Sonde: **Benachrichtigung an/aus** einschalten (Medienkanal kann anbleiben).

| Frage | Ergebnis |
|---|---|
| N1 Kommt jedes Update als neues Popup mit Vibration, oder wird die Benachrichtigung in der Liste ersetzt? | |
| N2 Vibration je App am Band abschaltbar? Bleibt dann das Popup? | |
| N3 Bleibt eine dauerhafte (ongoing) Benachrichtigung in der Liste stehen? | |
| N4 Bei Takt 5 s und 30 s: erträglich als Live-Anzeige, oder nur als Zusammenfassung nach einer Übung? | |

## Teil 3: Echter H10 (15 min)

Hinweis: hier verbindet die **Sonde auf dem Handy** mit dem Gurt, nicht das Band. Das Band
bekommt nur die Seiten vom Handy; so bleibt es auch später (ENTSCHEIDUNGEN #2). Vorher die
Messung in der trainingsapp beenden, sonst hält sie den Gurt.

Sonde: **Polar H10 verbinden**, dann **Start**. Protokoll zeigt „gefunden … verbunden …
abonniert 0x2A37". Danach zeigen die Seiten HF und RR vom Gurt; RMSSD ist eine Vorschau
(60-s-Fenster, keine Artefaktkorrektur, nicht hrv-core).

- [ ] HF am Band plausibel gegen die Sonde am Handy?
- [ ] Latenz Gurt → Band gefühlt (Puls kurz hochtreiben, Treppe)?
- [ ] Seite „Intervalle": zählt n hoch, letztes RR um 60000/HF?

## Protokoll

| Datum | Firmware Band | Handy / Android | Galaxy Wearable | Tester |
|---|---|---|---|---|
| 2026-10-26 | (nachtragen) | Pixel 10, Android 17 | 2.2.72, Fit3-Plugin 2.2.14 | Ben |

**Befund Weg 1 (Medien):** Trägt für Text (26.10.2026). Das Band zeigt Titel, Interpret und
Album der Sonde, aktualisiert im Sekundentakt, die Ansicht bleibt offen, auch mit laufender
Musik. ▶ blättert, ⏯ startet und stoppt, sobald die Sonde PAUSED ehrlich meldet; ◀ kam nie an.
Grenzen: jede Metadaten-Änderung setzt die Laufschrift zurück (Zeilen kurz halten, nur bei
Änderung senden); Blockzeichen fehlen in der Schrift, ASCII-Kurven sind zu grob. Das Albumcover
erscheint als Hintergrund der Mediensteuerung, aber nur beim Öffnen der Ansicht neu geladen,
unzuverlässig, und die Tasten liegen darüber: als Kurve unbrauchbar. **Die Fit3 taugt für
HF, RMSSD und Zustand als Text, nicht für eine Kurve.**

**Befund Weg 2 (Benachrichtigung):** noch nicht getestet (Teil 2 offen).

**Entscheidung / nächster Schritt:** Vorschlag in ENTSCHEIDUNGEN #5: Fit3 als Textanzeige
über `flet_medien` aus der trainingsapp weiterverfolgen; für die Intervallkurve (HF-Verlauf
150 s) eine Wear-OS-Uhr mit eigener App. Offen am Band: F8 (Tasche), Takt 2 s / 5 s, Zeichen
je Zeile ohne Laufschrift, Teil 2.

## Danach

- Trägt Weg 1: `flet_medien` für trainingsapp planen (ENTSCHEIDUNGEN #2, #3); Seitenformat und
  Zeilenlänge aus F4 festhalten; Zusammenfassung nach `hrv-core/docs/geraete/galaxy_fit3.md`.
- Trägt nur Weg 2: Anzeige als Zusammenfassung je Übung oder je Minute; Live-Werte am Band
  aufgeben oder Weg 3 (Gadgetbridge) prüfen.
- Trägt nichts: `docs/geraete.md`, Abschnitt Wear OS.
