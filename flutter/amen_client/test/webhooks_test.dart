import 'dart:convert';
import 'package:amen_client/amen_client.dart';
import 'package:test/test.dart';

void main() {
  // Golden vector captured from a real Amen delivery (2026-08-30).
  const goldenSecret = 'whsec_test';
  const goldenTimestamp = '2026-08-30T18:53:23.885957+00:00';
  const goldenBody =
      '{"event":"deal.status.changed","timestamp":"2026-08-30T18:53:23.885957+00:00","payload":{"number":"D-0000000002","status":"paid"}}';
  const goldenHex =
      '950ca0ff7494dd435d4dc9d7e7ebe31cf54f0859a28a69a686d77e8db9dfd45c';

  test('computeSignature matches the golden hex exactly', () {
    final raw = utf8.encode(goldenBody);
    expect(
      computeSignature(goldenSecret, goldenTimestamp, raw),
      goldenHex,
    );
  });

  test('verifySignature accepts the golden signature with/without prefix', () {
    final raw = utf8.encode(goldenBody);
    expect(
      verifySignature(goldenSecret, goldenTimestamp, raw, goldenHex),
      isTrue,
    );
    expect(
      verifySignature(goldenSecret, goldenTimestamp, raw, 'sha256=$goldenHex'),
      isTrue,
    );
  });

  test('verifySignature rejects tampering and missing inputs', () {
    final raw = utf8.encode(goldenBody);
    // Tampered body.
    expect(
      verifySignature(goldenSecret, goldenTimestamp, [...raw, 32], goldenHex),
      isFalse,
    );
    // Tampered timestamp changes the signed message.
    expect(
      verifySignature(goldenSecret, '$goldenTimestamp ', raw, goldenHex),
      isFalse,
    );
    // Missing signature.
    expect(
      verifySignature(goldenSecret, goldenTimestamp, raw, null),
      isFalse,
    );
    expect(verifySignature(goldenSecret, goldenTimestamp, raw, ''), isFalse);
    // Missing timestamp.
    expect(verifySignature(goldenSecret, null, raw, goldenHex), isFalse);
    expect(verifySignature(goldenSecret, '', raw, goldenHex), isFalse);
  });

  test('handler verifies, dispatches, and de-dupes on event timestamp',
      () async {
    final seen = <String>[];
    String? seenType;
    final h = WebhookHandler(goldenSecret, (e) async {
      seen.add(e.id);
      seenType = e.type;
    });
    final raw = utf8.encode(goldenBody);
    final sig = computeSignature(goldenSecret, goldenTimestamp, raw);
    final headers = {
      'X-Webhook-Signature': 'sha256=$sig',
      'X-Webhook-Timestamp': goldenTimestamp,
      'X-Webhook-Event': 'deal.status.changed',
    };

    // Bad signature -> 401.
    final bad = await h.handle({
      'X-Webhook-Signature': 'sha256=bad',
      'X-Webhook-Timestamp': goldenTimestamp,
    }, raw);
    expect(bad.status, 401);

    // Missing timestamp header -> 401.
    final noTs = await h.handle({'X-Webhook-Signature': 'sha256=$sig'}, raw);
    expect(noTs.status, 401);

    // Valid delivery -> 200 and dispatch.
    final ok = await h.handle(headers, raw);
    expect(ok.status, 200);
    expect(ok.body, {'ok': true});

    // Duplicate (same event timestamp) -> 200 duplicate, no re-dispatch.
    final dup = await h.handle(headers, raw);
    expect(dup.status, 200);
    expect(dup.body, {'ok': true, 'duplicate': true});

    expect(seen, [goldenTimestamp]);
    expect(seenType, 'deal.status.changed');
  });
}
