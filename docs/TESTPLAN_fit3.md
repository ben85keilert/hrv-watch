# Testplan: Galaxy Fit3 als Anzeige für H10-Werte

Ziel des ersten Tests: die offenen Fragen aus `hrv-research/Geraete/Fit3/anzeige_idee.md` am
Gerät beantworten. Dauer etwa eine Stunde. Ergebnisse unten eintragen, auch die negativen.

## Vorbereitung

1. Fit3 per Galaxy Wearable gekoppelt, Firmware aktuell (Galaxy Wearable → Band-Software).
2. Am Band: **Einstellungen → Anzeige → Mediensteuerung anzeigen** ein.
3. Galaxy Wearable → Benachrichtigungen: die Sonde („hrv-watch Sonde") erlauben, sobald sie
   installiert ist; „Auch bei Handynutzung anzeigen" ein, sonst kommt beim Testen mit dem Handy
   in der Hand nichts an.
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
| F1 Zeigt das Band Titel/Interpret/Album der Sonde? | Steuerung öffnen | |
| F2 Kommen Updates im 1-s-Takt an? | 30 s zuschauen, Nummern vergleichen; dann **Takt 2 s**, **Takt 5 s** | |
| F3 Bleibt die Medienansicht offen oder fällt das Band aufs Zifferblatt zurück? Nach wie vielen Sekunden? Hilft Always-on / längeres Display-Timeout? | Band nicht berühren, Stoppuhr | |
| F4 Zeilenlänge: ab wann abgeschnitten oder Laufschrift? | Seite „Zustand" hat die längste Zeile | |
| F5 Funktionieren ◀ ▶ als Blättern (Seite wechselt), ⏯ als Start/Stopp (Protokoll der Sonde zeigt „Band: …")? | Tasten am Band drücken | |
| F6 Parallel echte Musik starten: welche Session zeigt das Band? Kommt die Sonde zurück, wenn die Musik pausiert? | Spotify o. ä. starten, pausieren | |
| F7 Zeigt das Band die Sonde nur, wenn wirklich Ton läuft? | Falls bei F1 nichts erscheint: **Stille abspielen** einschalten und F1 wiederholen | |
| F8 Handy gesperrt, in der Tasche, 10 min: läuft die Anzeige weiter? | Akkuoptimierung für die Sonde ggf. ausschalten | |

## Teil 2: Benachrichtigungskanal (10 min)

Sonde: **Benachrichtigung an/aus** einschalten (Medienkanal kann anbleiben).

| Frage | Ergebnis |
|---|---|
| N1 Kommt jedes Update als neues Popup mit Vibration, oder wird die Benachrichtigung in der Liste ersetzt? | |
| N2 Vibration je App am Band abschaltbar? Bleibt dann das Popup? | |
| N3 Bleibt eine dauerhafte (ongoing) Benachrichtigung in der Liste stehen? | |
| N4 Bei Takt 5 s und 30 s: erträglich als Live-Anzeige, oder nur als Zusammenfassung nach einer Übung? | |

## Teil 3: Echter H10 (15 min)

Sonde: **Polar H10 verbinden**, dann **Start**. Protokoll zeigt „gefunden … verbunden …
abonniert 0x2A37". Danach zeigen die Seiten HF und RR vom Gurt; RMSSD ist eine Vorschau
(60-s-Fenster, keine Artefaktkorrektur, nicht hrv-core).

- [ ] HF am Band plausibel gegen die Sonde am Handy?
- [ ] Latenz Gurt → Band gefühlt (Puls kurz hochtreiben, Treppe)?
- [ ] Seite „Intervalle": zählt n hoch, letztes RR um 60000/HF?

## Protokoll

| Datum | Firmware Band | Handy / Android | Galaxy Wearable | Tester |
|---|---|---|---|---|
| | | | | Ben |

**Befund Weg 1 (Medien):**

**Befund Weg 2 (Benachrichtigung):**

**Entscheidung / nächster Schritt:**

## Danach

- Trägt Weg 1: `flet_medien` für trainingsapp planen (ENTSCHEIDUNGEN #2, #3); Seitenformat und
  Zeilenlänge aus F4 festhalten; Zusammenfassung nach `hrv-core/docs/geraete/galaxy_fit3.md`.
- Trägt nur Weg 2: Anzeige als Zusammenfassung je Übung oder je Minute; Live-Werte am Band
  aufgeben oder Weg 3 (Gadgetbridge) prüfen.
- Trägt nichts: `docs/geraete.md`, Abschnitt Wear OS.
