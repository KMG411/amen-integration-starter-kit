import 'dart:convert';
import 'package:amen_client/amen_client.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:test/test.dart';

AmenClient client(Future<http.Response> Function(http.Request) handler) =>
    AmenClient(Config(env: 'sandbox', apiKey: 'test-token', maxRetries: 1), httpClient: MockClient(handler));
http.Response json(int status, Object body) => http.Response(jsonEncode(body), status, headers: {'content-type': 'application/json'});

void main() {
  test('auth header and sandbox base URL', () async {
    final c = client((req) async { expect(req.url.toString(), 'https://sandbox-api.amnn.sa/api/v1/account'); expect(req.headers['X-API-Token'], 'test-token'); return json(200, {'id': 'a1'}); });
    expect(await c.account.get(), {'id': 'a1'});
  });
  test('error codes parsed into AmenApiError', () async {
    final c = client((_) async => json(400, {'error': ['first_name__required']}));
    await expectLater(c.customers.create(firstName: '', lastName: 'x', phoneCode: 'SA', phoneNumber: '5'),
        throwsA(isA<AmenApiError>().having((e) => e.has('first_name__required'), 'has code', true).having((e) => e.retryable, 'retryable', false)));
  });
  test('lifecycle guard blocks invalid action locally', () async {
    var calls = 0;
    final c = client((_) async { calls++; return json(200, {'number': 'DL-1', 'status': 'draft'}); });
    await expectLater(c.deals.actions.approve('DL-1'), throwsA(isA<AmenLifecycleError>()));
    expect(calls, 1);
  });
  test('Origin sent on mutating requests', () async {
    final c = client((req) async { expect(req.headers['Origin'], 'https://sandbox-api.amnn.sa'); return json(201, {'id': 'w', 'url': 'u', 'secret_key': 's'}); });
    expect((await c.webhooks.create('https://example.com/hook')).secretKey, 's');
  });
  test('timestamps are epoch milliseconds', () => expect(toDate(1679568486000)!.year, 2023));
}
