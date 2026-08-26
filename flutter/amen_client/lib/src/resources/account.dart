import 'package:http/http.dart' as http;
import '../client.dart';

class AccountResource {
  final AmenClient _c;
  AccountResource(this._c);
  Future<Map<String, dynamic>> get() async => (await _c.request('GET', '/account') as Map).cast();
  Future<List<dynamic>> bankAccounts() async => await _c.request('GET', '/account/bank-accounts/') as List;
  Future<Map<String, dynamic>> linkBankAccount(String iban, {http.MultipartFile? proofDocument}) async =>
      (await _c.request('POST', '/account/bank-accounts/', form: {'iban': iban}, files: proofDocument == null ? null : [proofDocument]) as Map).cast();
  Future<void> deleteBankAccount(String id) => _c.request('DELETE', '/account/bank-accounts/$id');
}
