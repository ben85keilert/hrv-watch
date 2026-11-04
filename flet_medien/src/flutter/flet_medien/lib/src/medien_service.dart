
import 'package:flet/flet.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

/// Flet-Service „FletMedien": reicht die Aufrufe an das Android-Plugin durch
/// (Kanal `flet_medien/session`) und meldet Tasten als Ereignis `taste` an Python —
/// mit `t_mono_us`, der monotonen Uhr der Dart-Seite beim Eintreffen.
///
/// Methoden (Python → Dart → Java): `starten` {titel, zeile2, zeile3, laeuft},
/// `zeigen` {titel, zeile2, zeile3}, `zustand` {laeuft}, `beenden`, `aktiv` → bool,
/// `warnen` {titel, text} (Benachrichtigung mit Vibration).
/// Außerhalb von Android tun die Methoden nichts (kein Fehler), `aktiv` ist false.
class FletMedienService extends FletService {
  FletMedienService({required super.control});

  static const MethodChannel _kanal = MethodChannel("flet_medien/session");

  /// Monotone Uhr der Dart-Seite für die Tasten (wie `flet_ble` je Paket): Python rechnet
  /// sie auf seine Uhr um und setzt die Marke an den Zeitpunkt des Drucks, nicht an die
  /// Ankunft (Diagnosemodus der trainingsapp, Entscheidung 8).
  static final Stopwatch _uhr = Stopwatch()..start();

  static bool get _istAndroid => !kIsWeb && Platform.isAndroid;

  @override
  void init() {
    super.init();
    debugPrint("FletMedienService(${control.id}).init");
    control.addInvokeMethodListener(_invoke);
    if (_istAndroid) _kanal.setMethodCallHandler(_vonAndroid);
  }

  /// Java → Dart: eine Taste am Band (`taste`: play, pause, stop, weiter, zurueck).
  Future<dynamic> _vonAndroid(MethodCall call) async {
    if (call.method == "taste") {
      final a = (call.arguments as Map?) ?? const {};
      control.triggerEvent("taste", {
        "taste": a["taste"] as String? ?? "",
        "t_mono_us": _uhr.elapsedMicroseconds,
      });
    }
    return null;
  }

  Future<dynamic> _invoke(String name, dynamic args) async {
    final a = (args as Map?) ?? const {};
    if (!_istAndroid) {
      return name == "aktiv" ? false : null;
    }
    switch (name) {
      case "starten":
        return await _kanal.invokeMethod<bool>("starten", _zeilen(a, laeuft: true));
      case "zeigen":
        return await _kanal.invokeMethod<bool>("zeigen", _zeilen(a));
      case "zustand":
        return await _kanal.invokeMethod<bool>(
            "zustand", {"laeuft": a["laeuft"] as bool? ?? true});
      case "beenden":
        await _kanal.invokeMethod<void>("beenden");
        return null;
      case "warnen":
        await _kanal.invokeMethod<void>("warnen", {
          "titel": a["titel"] as String? ?? "",
          "text": a["text"] as String? ?? "",
        });
        return null;
      case "aktiv":
        return await _kanal.invokeMethod<bool>("aktiv") ?? false;
      default:
        throw Exception("Unbekannte Methode: $name");
    }
  }

  static Map<String, Object> _zeilen(Map a, {bool laeuft = false}) => {
        "titel": a["titel"] as String? ?? "",
        "zeile2": a["zeile2"] as String? ?? "",
        "zeile3": a["zeile3"] as String? ?? "",
        if (laeuft) "laeuft": a["laeuft"] as bool? ?? true,
      };

  @override
  void dispose() {
    debugPrint("FletMedienService(${control.id}).dispose");
    control.removeInvokeMethodListener(_invoke);
    if (_istAndroid) {
      _kanal.setMethodCallHandler(null);
      _kanal.invokeMethod<void>("beenden").catchError((Object e) {
        debugPrint("FletMedien beenden: $e");
      });
    }
    super.dispose();
  }
}
