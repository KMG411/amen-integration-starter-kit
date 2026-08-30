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

/// Framework-agnostic (shelf, dart_frog, …): call [handle] with the headers and
/// RAW body bytes. Verifies first, de-duplicates by event timestamp, then
/// dispatches. Keep [onEvent] fast; queue heavy work.
class WebhookHandler {
  final String secret;
  final Future<void> Function(WebhookEvent) onEvent;
  final Set<String> seen;
  WebhookHandler(this.secret, this.onEvent, {Set<String>? seen})
      : seen = seen ?? {};

  String? _header(Map<String, String> headers, String name) => headers.entries
      .where((e) => e.key.toLowerCase() == name)
      .map((e) => e.value)
      .firstOrNull;

  Future<WebhookResult> handle(
    Map<String, String> headers,
    List<int> rawBody,
  ) async {
    final sig = _header(headers, signatureHeader);
    final timestamp = _header(headers, timestampHeader);
    if (!verifySignature(secret, timestamp, rawBody, sig)) {
      return WebhookResult(401, {'error': 'invalid signature'});
    }
    Map<String, dynamic> data;
    try {
      data = (jsonDecode(utf8.decode(rawBody)) as Map).cast();
    } catch (_) {
      return WebhookResult(400, {'error': 'invalid json'});
    }
    // No event id in the body — de-dupe on the event timestamp, falling back to
    // the header timestamp.
    final id = (data['timestamp'] ?? timestamp ?? '').toString();
    if (id.isNotEmpty && seen.contains(id)) {
      return WebhookResult(200, {'ok': true, 'duplicate': true});
    }
    if (id.isNotEmpty) seen.add(id);
    final type = (data['event'] ?? _header(headers, eventHeader)) as String?;
    await onEvent(WebhookEvent(id, type, data, rawBody));
    return WebhookResult(200, {'ok': true});
  }
}
