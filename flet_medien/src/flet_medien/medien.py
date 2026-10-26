"""Mediensteuerung als Flet-Service — drei Textzeilen für ein Band am Handgelenk.

Android zeigt für eine aktive ``MediaSession`` Titel, Interpret und Album in der
Mediensteuerung, und Galaxy Wearable reicht sie an die Galaxy Fit3 weiter (Befund
hrv-watch/docs/TESTPLAN_fit3.md, 26.10.2026). Die Zeilen sind frei: Hier stehen
HF, RMSSD und Intervall-Kennzahlen darin. Die Tasten des Bands (▶ ◀ ⏯) kommen als
``on_taste`` zurück. ``warnen`` schickt eine Benachrichtigung (Gurt prüfen), die das
Band mit Vibration zeigt.

Die Session lebt im App-Prozess (kein eigener Dienst); am Leben hält den Prozess
der Vordergrunddienst der Messung (flet_ble). Es läuft kein Ton: Der Zustand
PLAYING genügt dem Band.

Regeln für Aufrufer (wie flet_ble, flet_ton):

- **Innerhalb von ``main(page)`` anlegen**, an ``page.services`` hängen und eine
  **starke Referenz halten** — Flet entfernt Dienste, auf die niemand mehr zeigt.
- ``on_taste`` im Konstruktor übergeben.
- Nur in der gebauten Android-App (``verfuegbar``).
- **Fehler:** Wirft die Dart-Seite, kommt ein ``RuntimeError`` an; läuft die
  Python-Frist ab, ``TimeoutError``.

Befunde am Fit3, die das Protokoll bestimmen: Jede Metadaten-Änderung setzt die
Laufschrift am Band zurück — ``zeigen`` nur rufen, wenn sich der Text geändert hat.
Meldet die Session immer PLAYING, schickt das Band bei ⏯ nur „pause“; darum
``zustand(laeuft)`` ehrlich setzen, dann wechselt das Band zwischen play und pause.

Kein ``from __future__ import annotations`` in diesem Modul (wie flet_ble): Flet löst
den Typ der ``on_*``-Felder zur Laufzeit auf und kommt mit String-Annotationen nicht
zurecht („[resolve error] on_taste: tuple index out of range“, trainingsapp 0.3.20
beim ersten ▶ am Band, 26.10.2026).
"""

from dataclasses import dataclass

import flet as ft

#: Fristen (s) auf der Python-Seite
_FRIST = 5.0

#: Was das Band schicken kann
TASTEN = ("play", "pause", "stop", "weiter", "zurueck")


@dataclass(kw_only=True)
class MedienTasteEvent(ft.Event["FletMedien"]):
    """Eine Taste am Band oder in der Mediensteuerung des Handys."""

    taste: str  # play | pause | stop | weiter | zurueck


@ft.control("FletMedien")
class FletMedien(ft.Service):
    """Eine MediaSession mit drei Textzeilen, Tasten kommen als ``on_taste``."""

    on_taste: ft.EventHandler[MedienTasteEvent] | None = None
    """▶ ◀ ⏯ am Band (``weiter``, ``zurueck``, ``play``/``pause``) oder ``stop``."""

    async def starten(self, titel: str, zeile2: str, zeile3: str, laeuft: bool = True) -> None:
        """Session anlegen (oder erneuern) und die erste Seite zeigen."""
        await self._invoke_method(
            "starten",
            {"titel": titel, "zeile2": zeile2, "zeile3": zeile3, "laeuft": bool(laeuft)},
            timeout=_FRIST,
        )

    async def zeigen(self, titel: str, zeile2: str, zeile3: str) -> None:
        """Die drei Zeilen ersetzen — nur rufen, wenn sich etwas geändert hat."""
        await self._invoke_method(
            "zeigen", {"titel": titel, "zeile2": zeile2, "zeile3": zeile3}, timeout=_FRIST
        )

    async def zustand(self, laeuft: bool) -> None:
        """PLAYING (``True``) oder PAUSED — das Band zeigt ⏸ bzw. ▶."""
        await self._invoke_method("zustand", {"laeuft": bool(laeuft)}, timeout=_FRIST)

    async def beenden(self) -> None:
        """Session freigeben, Benachrichtigung weg — das Band zeigt nichts mehr."""
        await self._invoke_method("beenden", None, timeout=_FRIST)

    async def warnen(self, titel: str, text: str) -> None:
        """Eine Warnung als eigene Benachrichtigung (Kanal „Warnungen“, hohe Wichtigkeit):
        das Band vibriert und zeigt sie, wenn Galaxy Wearable Benachrichtigungen der App
        durchlässt. Verschwindet nach einer Minute von selbst. Unabhängig von der Session —
        geht auch ohne ``starten``."""
        await self._invoke_method("warnen", {"titel": titel, "text": text}, timeout=_FRIST)

    async def aktiv(self) -> bool:
        """Gibt es gerade eine Session?"""
        return bool(await self._invoke_method("aktiv", None, timeout=_FRIST))


def verfuegbar(page: ft.Page) -> bool:
    """Nur die gebaute Android-App enthält den Dart- und Java-Teil.

    Auch ``flet run --android`` (Flet-App am Handy) meldet Android, hat die
    Erweiterung aber nicht — wie bei flet_ble.
    """
    return page.platform == ft.PagePlatform.ANDROID and not page.web
