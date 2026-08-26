import '../client.dart';
import '../models.dart';

class Webhooks {
  final AmenClient _c;
  Webhooks(this._c);
  Future<List<Webhook>> list() async => (await _c.request('GET', '/web-hooks/') as List).map((w) => Webhook((w as Map).cast())).toList();
  /// `secretKey` in the response is shown ONLY now — store it in a secret manager immediately.
  Future<Webhook> create(String url) async => Webhook((await _c.request('POST', '/web-hooks/', json: {'url': url}) as Map).cast());
  Future<void> delete(String id) => _c.request('DELETE', '/web-hooks/$id');
}
