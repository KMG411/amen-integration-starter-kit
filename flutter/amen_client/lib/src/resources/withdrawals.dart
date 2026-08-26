import '../client.dart';
import '../models.dart';

class Withdrawals {
  final AmenClient _c;
  Withdrawals(this._c);
  Future<Withdrawal> create({required String bankAccountId, required String amount}) async =>
      Withdrawal((await _c.request('POST', '/withdrawals/', json: {'bank_account_id': bankAccountId, 'amount': amount}) as Map).cast());
  Future<Withdrawal> get(String n) async => Withdrawal((await _c.request('GET', '/withdrawals/$n') as Map).cast());
  Future<Page<Withdrawal>> list({int? page, int? pageSize, String? status}) async =>
      Page.from((await _c.request('GET', '/withdrawals/', params: {'page': page?.toString(), 'page_size': pageSize?.toString(), 'status': status}) as Map?)?.cast(), 'withdrawals', Withdrawal.new);
}
