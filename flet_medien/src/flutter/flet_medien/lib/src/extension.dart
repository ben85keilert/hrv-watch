import 'package:flet/flet.dart';

import 'medien_service.dart';

class Extension extends FletExtension {
  @override
  FletService? createService(Control control) {
    switch (control.type) {
      case "FletMedien":
        return FletMedienService(control: control);
      default:
        return null;
    }
  }
}
