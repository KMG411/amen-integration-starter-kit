import 'package:http/http.dart' as http;
import '../client.dart';
import '../errors.dart';
import '../models.dart';

/// Which statuses each action may be called from (docs/02-deal-lifecycle.md).
const allowedFrom = <String, List<String>>{
  'submit': ['draft'], 'approve': ['requested'],
  'make-payment-wallet': ['payment_pending'], 'make-payment-online': ['payment_pending'],
  'execution-start': ['paid'], 'execution-complete': ['executing'], 'complete': ['executed'],
  'transfer-seller-amount': ['completed'], 'dispute': ['completed'],
  'dispute-approve': ['disputed'], 'dispute-decline': ['disputed'],
  'cancel': ['draft', 'requested', 'payment_pending', 'paid', 'executing'],
};

class DealActions {
  final AmenClient _c;
  final Deals _deals;
  DealActions(this._c, this._deals);

  Future<Map<String, dynamic>> _act(String n, String action, {Object? json, Map<String, String>? form, List<http.MultipartFile>? files, bool check = true}) async {
    if (check) {
      final status = (await _deals.get(n)).status;
      if (!allowedFrom[action]!.contains(status)) throw AmenLifecycleError("action '$action' is not allowed from status '$status' (allowed: ${allowedFrom[action]!.join(', ')})");
    }
    return ((await _c.request('POST', '/deals/$n/action/$action', json: json, form: form, files: files) as Map?) ?? {}).cast();
  }

  Future<Deal> submit(String n, {bool check = true}) async => Deal(await _act(n, 'submit', check: check));
  Future<Deal> approve(String n, {String? price, bool check = true}) async => Deal(await _act(n, 'approve', json: price == null ? {} : {'price': price}, check: check));
  Future<Deal> payWithWallet(String n, {bool check = true}) async => Deal(await _act(n, 'make-payment-wallet', check: check));
  Future<Checkout> payOnline(String n, {String paymentMethod = 'mada', bool check = true}) async => Checkout(await _act(n, 'make-payment-online', json: {'payment_method': paymentMethod}, check: check));
  Future<Deal> executionStart(String n, {bool check = true}) async => Deal(await _act(n, 'execution-start', check: check));
  Future<Deal> executionComplete(String n, {bool check = true}) async => Deal(await _act(n, 'execution-complete', check: check));
  Future<Deal> complete(String n, {bool check = true}) async => Deal(await _act(n, 'complete', check: check));
  Future<Deal> transferSellerAmount(String n, {bool check = true}) async => Deal(await _act(n, 'transfer-seller-amount', check: check));
  Future<Deal> cancel(String n, {required String dealParty, required int reason, required String comment, bool check = true}) async =>
      Deal(await _act(n, 'cancel', json: {'deal_party': dealParty, 'reason': reason, 'comment': comment}, check: check));
  Future<Deal> dispute(String n, {required int reason, required String comment, List<http.MultipartFile> attachments = const [], bool check = true}) async =>
      Deal(await _act(n, 'dispute', form: {'reason': '$reason', 'comment': comment}, files: attachments, check: check));
  Future<Deal> disputeApprove(String n, {required int reason, required String comment, bool check = true}) async => Deal(await _act(n, 'dispute-approve', form: {'reason': '$reason', 'comment': comment}, check: check));
  Future<Deal> disputeDecline(String n, {required int reason, required String comment, bool check = true}) async => Deal(await _act(n, 'dispute-decline', form: {'reason': '$reason', 'comment': comment}, check: check));
}

class Deals {
  final AmenClient _c;
  late final DealActions actions = DealActions(_c, this);
  Deals(this._c);

  Future<Deal> create({required String offerType, required String offerTitle, String? offerPrice, int? offerCategory, String? offerDescription, String? offerDeliveryFee, String? dealSubjectDetails}) async =>
      Deal((await _c.request('POST', '/deals/', json: {
        'offer_type': offerType, 'offer_title': offerTitle,
        if (offerPrice != null) 'offer_price': offerPrice, if (offerCategory != null) 'offer_category': offerCategory,
        if (offerDescription != null) 'offer_description': offerDescription, if (offerDeliveryFee != null) 'offer_delivery_fee': offerDeliveryFee,
        if (dealSubjectDetails != null) 'deal_subject_details': dealSubjectDetails,
      }) as Map).cast());
  Future<Deal> get(String n) async => Deal((await _c.request('GET', '/deals/$n') as Map).cast());
  Future<Deal> update(String n, Map<String, Object?> fields) async => Deal((await _c.request('PUT', '/deals/$n', json: fields) as Map).cast());
  Future<void> delete(String n) => _c.request('DELETE', '/deals/$n');
  Future<Page<Deal>> list({int? page, int? perPage, String? status}) async =>
      Page.from((await _c.request('GET', '/deals/', params: {'page': page?.toString(), 'per_page': perPage?.toString(), 'status': status}) as Map?)?.cast(), 'deals', Deal.new);
  Stream<Deal> iterAll({String? status}) async* {
    for (var page = 0;; page++) { final p = await list(page: page, status: status); yield* Stream.fromIterable(p.items); if (page + 1 >= p.pages || p.items.isEmpty) return; }
  }
  Future<Deal> setParties(String n, {required List<String> buyers, required List<String> sellers}) async => Deal((await _c.request('POST', '/deals/$n/parties/', json: {'buyers': buyers, 'sellers': sellers}) as Map).cast());
  Future<Deal> setDeliveryAddress(String n, {required int city, required String street, required String buildingNumber, required String zipCode, String? district, String? unitNumber}) async =>
      Deal((await _c.request('POST', '/deals/$n/delivery-address', json: {'city': city, 'street': street, 'building_number': buildingNumber, 'zip_code': zipCode, if (district != null) 'district': district, if (unitNumber != null) 'unit_number': unitNumber}) as Map).cast());
  Future<Deal> setBillingAddress(String n, Map<String, Object?> address) async => Deal((await _c.request('POST', '/deals/$n/billing-address', json: address) as Map).cast());
  Future<List<String>> allowedPaymentMethods(String n) async => (((await _c.request('GET', '/deals/$n/allowed-payment-methods/') as Map)['payment_methods'] as List?) ?? []).cast<String>();
}
