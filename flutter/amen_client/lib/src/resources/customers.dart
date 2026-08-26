import '../client.dart';
import '../models.dart';

class Customers {
  final AmenClient _c;
  Customers(this._c);
  Future<Customer> create({required String firstName, required String lastName, required String phoneCode, required String phoneNumber}) async =>
      Customer((await _c.request('POST', '/customers/', json: {'first_name': firstName, 'last_name': lastName, 'phone_code': phoneCode, 'phone_number': phoneNumber}) as Map).cast());
  Future<Customer> get(String customerNumber) async => Customer((await _c.request('GET', '/customers/$customerNumber') as Map).cast());
  Future<Page<Customer>> list({int? page, int? perPage, String? type, String? status}) async =>
      Page.from((await _c.request('GET', '/customers/', params: {'page': page?.toString(), 'per_page': perPage?.toString(), 'type': type, 'status': status}) as Map?)?.cast(), 'customers', Customer.new);
  /// Iterate every page — never process only the first page by accident.
  Stream<Customer> iterAll({String? type, String? status}) async* {
    for (var page = 0;; page++) {
      final p = await list(page: page, type: type, status: status);
      yield* Stream.fromIterable(p.items);
      if (page + 1 >= p.pages || p.items.isEmpty) return;
    }
  }
}
