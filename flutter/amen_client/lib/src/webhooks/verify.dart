import 'dart:convert';
import 'package:crypto/crypto.dart';

/// Amen webhook header names (verified against real deliveries, 2026-08-30).
const signatureHeader = 'x-webhook-signature';
const timestampHeader = 'x-webhook-timestamp';
const eventHeader = 'x-webhook-event';

/// Lowercase hex HMAC-SHA256 over `utf8(timestamp) + "." + rawBody`.
///
/// The signed message is the ISO-8601 timestamp, a literal dot, then the RAW
/// request body bytes — never a re-serialized copy of the JSON.
String computeSignature(String secret, String timestamp, List<int> rawBody) {
  final message = <int>[...utf8.encode(timestamp), 0x2e, ...rawBody];
  return Hmac(sha256, utf8.encode(secret)).convert(message).toString();
}

/// Verifies the `X-Webhook-Signature` header value.
///
/// Accepts an optional `sha256=` prefix. Returns false when the received
/// signature or [timestamp] is null or empty. Uses a constant-time comparison
/// (no short-circuit on the first mismatched byte).
bool verifySignature(
  String secret,
  String? timestamp,
  List<int> rawBody,
  String? received,
) {
  if (received == null || received.isEmpty) return false;
  if (timestamp == null || timestamp.isEmpty) return false;
  final given = _stripPrefix(received).trim().toLowerCase();
  final expected = computeSignature(secret, timestamp, rawBody);
  if (given.length != expected.length) return false;
  var diff = 0;
  for (var i = 0; i < expected.length; i++) {
    diff |= given.codeUnitAt(i) ^ expected.codeUnitAt(i);
  }
  return diff == 0;
}

String _stripPrefix(String value) =>
    value.startsWith('sha256=') ? value.substring('sha256='.length) : value;
