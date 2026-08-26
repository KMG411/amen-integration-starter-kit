# sample_app — back-end proxy pattern

`lib/main.dart` calls **your** server at `BACKEND_URL` (`flutter run --dart-define=BACKEND_URL=https://…`). Your server holds `AMN_API_KEY` and uses any server-side stack from this kit (e.g. `../amen_client` in Dart, or `python/`, `typescript/`, `php/`). The app stores only its own session token, in `flutter_secure_storage`.

Never put `AMN_API_KEY` in a mobile app — it can be extracted from the binary.
