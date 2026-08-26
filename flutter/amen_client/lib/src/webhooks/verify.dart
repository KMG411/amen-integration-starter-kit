import 'dart:convert';
import 'package:crypto/crypto.dart';

/// Signature verification over the RAW body. Header name and algorithm are one configuration point (docs/04-webhooks.md).
const signatureHeader = 'x-signature';

String computeSignature(String secret, List<int> rawBody) => Hmac(sha256, utf8.encode(secret)).convert(rawBody).toString();

/// Accepts hex digests, optionally prefixed like "sha256=<hex>". Constant-time comparison.
bool verifySignature(String secret, List<int> rawBody, String? received) {
  if (received == null || received.isEmpty) return false;
  final given = (received.contains('=') ? received.split('=')[1] : received).trim().toLowerCase();
  final expected = computeSignature(secret, rawBody);
  if (given.length != expected.length) return false;
  var diff = 0;
  for (var i = 0; i < expected.length; i++) { diff |= given.codeUnitAt(i) ^ expected.codeUnitAt(i); }
  return diff == 0;
}
