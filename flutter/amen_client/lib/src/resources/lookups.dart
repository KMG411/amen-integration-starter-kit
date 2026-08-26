import '../client.dart';

class Lookups {
  final AmenClient _c;
  Lookups(this._c);
  Future<List<dynamic>> countryCodes() async => await _c.request('GET', '/allowed-country-codes/') as List;
  Future<List<dynamic>> cities() async => await _c.request('GET', '/cities') as List;
  Future<List<dynamic>> categories() async => await _c.request('GET', '/categories/') as List;
  Future<List<dynamic>> disputeReasons() async => await _c.request('GET', '/dispute-reasons/') as List;
  Future<List<dynamic>> disputeResolutionReasons() async => await _c.request('GET', '/dispute-resolution-reasons/') as List;
  Future<List<dynamic>> cancelReasons({String? partyType}) async => await _c.request('GET', '/cancel-reasons/', params: {'party_type': partyType}) as List;
}
