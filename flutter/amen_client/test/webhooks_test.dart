import 'dart:convert';
import 'package:amen_client/amen_client.dart';
import 'package:test/test.dart';

void main() {
  const secret = 'unit-test-secret';
  test('signature roundtrip with optional prefix', () {
    final body = utf8.encode('{"id":"e1","event":"deal.paid"}');
    final sig = computeSignature(secret, body);
    expect(verifySignature(secret, body, sig), isTrue);
    expect(verifySignature(secret, body, 'sha256=$sig'), isTrue);
    expect(verifySignature(secret, [...body, 32], sig), isFalse);
    expect(verifySignature(secret, body, null), isFalse);
  });
  test('handler rejects bad signature and de-duplicates', () async {
    final seen = <String>[];
    final h = WebhookHandler(secret, (e) async => seen.add(e.id));
    final body = utf8.encode(jsonEncode({'id': 'e1', 'event': 'deal.paid'}));
    final good = {'X-Signature': computeSignature(secret, body)};
    expect((await h.handle({'X-Signature': 'bad'}, body)).status, 401);
    expect((await h.handle(good, body)).body, {'ok': true});
    expect((await h.handle(good, body)).body, {'ok': true, 'duplicate': true});
    expect(seen, ['e1']);
  });
}
