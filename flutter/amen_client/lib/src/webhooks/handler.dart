import 'dart:convert';
import 'verify.dart';

class WebhookEvent {
  final String id;
  final String? type;
  final Map<String, dynamic> data;
  final List<int> raw;
  WebhookEvent(this.id, this.type, this.data, this.raw);
}

class WebhookResult {
  final int status;
  final Map<String, Object?> body;
  WebhookResult(this.status, this.body);
}

/// Framework-agnostic (shelf, dart_frog, …): call [handle] with the headers and RAW body bytes.
/// Verifies first, de-duplicates by id, then dispatches. Keep [onEvent] fast; queue heavy work.
class WebhookHandler {
  final String secret;
  final Future<void> Function(WebhookEvent) onEvent;
  final Set<String> seen;
  WebhookHandler(this.secret, this.onEvent, {Set<String>? seen}) : seen = seen ?? {};

  Future<WebhookResult> handle(Map<String, String> headers, List<int> rawBody) async {
    final sig = headers.entries.where((e) => e.key.toLowerCase() == signatureHeader).map((e) => e.value).firstOrNull;
    if (!verifySignature(secret, rawBody, sig)) return WebhookResult(401, {'error': 'invalid signature'});
    Map<String, dynamic> data;
    try { data = (jsonDecode(utf8.decode(rawBody)) as Map).cast(); } catch (_) { return WebhookResult(400, {'error': 'invalid json'}); }
    final id = (data['id'] ?? data['event_id'] ?? '').toString();
    if (id.isNotEmpty && seen.contains(id)) return WebhookResult(200, {'ok': true, 'duplicate': true});
    if (id.isNotEmpty) seen.add(id);
    await onEvent(WebhookEvent(id, (data['event'] ?? data['type']) as String?, data, rawBody));
    return WebhookResult(200, {'ok': true});
  }
}
