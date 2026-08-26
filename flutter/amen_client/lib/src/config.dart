import 'dart:io';

/// Environment-based configuration. On Flutter, pass values explicitly (dart-define) — never ship the API key in the app.
class Config {
  static const baseUrls = {'sandbox': 'https://sandbox-api.amnn.sa', 'live': 'https://api.amnn.sa'};
  static const apiPrefix = '/api/v1';

  final String env, apiKey, baseUrl;
  final Duration timeout;
  final String? webhookSecret;
  final int maxRetries;

  Config({required this.env, required this.apiKey, String? baseUrl, this.timeout = const Duration(seconds: 20), this.webhookSecret, this.maxRetries = 3})
      : baseUrl = baseUrl ?? baseUrls[env]!;

  /// Reads AMN_* from the process environment, falling back to a `.env` file found in cwd or up to 3 parents.
  factory Config.fromEnvironment({String? apiKey, String? env}) {
    final file = _dotenv();
    String? get(String k) => Platform.environment[k] ?? file[k];
    final e = (env ?? get('AMN_ENV') ?? 'sandbox').toLowerCase();
    if (!baseUrls.containsKey(e)) throw ArgumentError("AMN_ENV must be 'sandbox' or 'live', got '$e'");
    final key = apiKey ?? get('AMN_API_KEY');
    if (key == null || key.isEmpty) throw StateError('AMN_API_KEY is not set (see .env.example)');
    return Config(env: e, apiKey: key, baseUrl: get('AMN_BASE_URL'),
        timeout: Duration(milliseconds: int.tryParse(get('AMN_TIMEOUT_MS') ?? '') ?? 20000),
        webhookSecret: (get('AMN_WEBHOOK_SECRET') ?? '').isEmpty ? null : get('AMN_WEBHOOK_SECRET'));
  }

  static Map<String, String> _dotenv() {
    var dir = Directory.current;
    for (var i = 0; i < 3; i++) {
      final f = File('${dir.path}/.env');
      if (f.existsSync()) {
        final out = <String, String>{};
        for (final line in f.readAsLinesSync()) {
          if (line.trim().isEmpty || line.startsWith('#') || !line.contains('=')) continue;
          final idx = line.indexOf('=');
          out[line.substring(0, idx).trim()] = line.substring(idx + 1).replaceFirst(RegExp(r'\s+#.*$'), '').trim();
        }
        return out;
      }
      dir = dir.parent;
    }
    return const {};
  }
}
