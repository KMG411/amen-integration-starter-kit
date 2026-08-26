import 'dart:convert';
import 'dart:math';
import 'package:http/http.dart' as http;
import 'config.dart';
import 'errors.dart';
import 'resources/account.dart';
import 'resources/customers.dart';
import 'resources/deals.dart';
import 'resources/lookups.dart';
import 'resources/webhooks.dart';
import 'resources/withdrawals.dart';

String _csrfToken() { final r = Random.secure(); return List.generate(16, (_) => r.nextInt(256).toRadixString(16).padLeft(2, '0')).join(); }

/// AmenClient — the one place that knows about auth headers, base URL, timeouts and retries.
class AmenClient {
  final Config config;
  final http.Client _http;
  late final Lookups lookups = Lookups(this);
  late final AccountResource account = AccountResource(this);
  late final Customers customers = Customers(this);
  late final Deals deals = Deals(this);
  late final Withdrawals withdrawals = Withdrawals(this);
  late final Webhooks webhooks = Webhooks(this);

  final String _csrf = _csrfToken();   // 32 hex chars — Django CSRF token format
  AmenClient(this.config, {http.Client? httpClient}) : _http = httpClient ?? http.Client();

  Future<dynamic> request(String method, String path, {Object? json, Map<String, String?>? params, Map<String, String>? form, List<http.MultipartFile>? files}) async {
    var uri = Uri.parse('${config.baseUrl}${Config.apiPrefix}$path');
    if (params != null) uri = uri.replace(queryParameters: {for (final e in params.entries) if (e.value != null) e.key: e.value!});
    final headers = {'X-API-Token': config.apiKey, 'Accept': 'application/json', 'Accept-Language': 'en', 'User-Agent': 'amen-starter-kit-dart/0.1', 'Cookie': 'csrftoken=$_csrf'};
    if (method != 'GET') {  // Django CSRF double-submit: token in both the X-CSRFToken header and the csrftoken cookie
      headers['X-CSRFToken'] = _csrf;
      headers['Origin'] = config.baseUrl;
      headers['Referer'] = config.baseUrl;
    }

    for (var attempt = 1;; attempt++) {
      http.Response res;
      try {
        if (form != null || files != null) {
          final req = http.MultipartRequest(method, uri)..headers.addAll(headers)..fields.addAll(form ?? {})..files.addAll(files ?? []);
          res = await http.Response.fromStream(await _http.send(req).timeout(config.timeout));
        } else {
          final req = http.Request(method, uri)..headers.addAll(headers);
          if (json != null) { req.headers['Content-Type'] = 'application/json'; req.body = jsonEncode(json); }
          res = await http.Response.fromStream(await _http.send(req).timeout(config.timeout));
        }
      } catch (e) {
        if (attempt > config.maxRetries) rethrow;
        await Future.delayed(_backoff(attempt)); continue;
      }
      if (res.statusCode < 400) return res.body.isEmpty ? null : jsonDecode(res.body);
      final err = _toError(res, method, path);
      if (err.retryable && attempt <= config.maxRetries) { await Future.delayed(_backoff(attempt, res.headers['retry-after'])); continue; }
      throw err;
    }
  }

  AmenApiError _toError(http.Response res, String method, String path) {
    Object? body;
    try { body = jsonDecode(res.body); } catch (_) { body = res.body; }
    final raw = body is Map ? body['error'] : null;
    final codes = raw is List ? raw.map((e) => e.toString()).toList() : raw is String ? [raw] : <String>[];
    return AmenApiError(res.statusCode, codes, method, '${Config.apiPrefix}$path', body);
  }

  static Duration _backoff(int attempt, [String? retryAfter]) {
    final ra = int.tryParse(retryAfter ?? '');
    return ra != null ? Duration(seconds: ra) : Duration(milliseconds: min(1 << attempt, 20) * 1000 + Random().nextInt(1000));
  }

  void close() => _http.close();
}
