// Golden path (scenario/golden-path.yml). `dart run example/golden_path.dart [DL-000123]` — pass a deal number to resume from 'paid'.
import 'dart:io';
import 'package:amen_client/amen_client.dart';

String phone(String p) => '$p${(DateTime.now().millisecondsSinceEpoch ~/ 1000).toString().substring(3)}'.substring(0, 9);
void step(String label, [Deal? d]) => print('✔ $label${d == null ? '' : ' → status=${d.status}'}');

Future<void> main(List<String> args) async {
  final amen = AmenClient(Config.fromEnvironment());
  print('environment: ${amen.config.env} (${amen.config.baseUrl})\n');
  Future<void> continueFromPaid(String n) async {
    step('execution-start', await amen.deals.actions.executionStart(n));
    step('execution-complete', await amen.deals.actions.executionComplete(n));
    step('complete', await amen.deals.actions.complete(n));
    step('transfer-seller-amount (payout)', await amen.deals.actions.transferSellerAmount(n));
    print('\n🎉 deal $n finished: ${(await amen.deals.get(n)).status}');
  }
  if (args.isNotEmpty) { await continueFromPaid(args.first); return; }

  final buyer = await amen.customers.create(firstName: 'Buyer', lastName: 'Kit', phoneCode: 'SA', phoneNumber: phone('57'));
  final seller = await amen.customers.create(firstName: 'Seller', lastName: 'Kit', phoneCode: 'SA', phoneNumber: phone('58'));
  step('customers ${buyer.number} (buyer), ${seller.number} (seller)');
  final category = (await amen.lookups.categories()).first['id'] as int;
  final city = (await amen.lookups.cities()).first['id'] as int;
  final deal = await amen.deals.create(offerType: 'product', offerCategory: category, offerTitle: 'Starter Kit golden path',
      offerDescription: 'Reference deal created by the Amen integration starter kit', offerPrice: '100.00', offerDeliveryFee: '10.00');
  final n = deal.number; step('deal $n created', deal);
  step('parties', await amen.deals.setParties(n, buyers: [buyer.number], sellers: [seller.number]));
  step('delivery address', await amen.deals.setDeliveryAddress(n, city: city, district: 'Al Olaya', street: 'King Fahd Rd', buildingNumber: '1234', unitNumber: '1', zipCode: '12211'));
  step('submit', await amen.deals.actions.submit(n));
  step('approve', await amen.deals.actions.approve(n));
  print('  allowed payment methods: ${await amen.deals.allowedPaymentMethods(n)}');
  try { step('pay with wallet', await amen.deals.actions.payWithWallet(n)); }
  on AmenApiError catch (e) {
    final checkout = await amen.deals.actions.payOnline(n, paymentMethod: 'mada');
    print('\n⏸  NEEDS_TOP_UP — wallet payment not possible (${e.codes.isEmpty ? e.status : e.codes.join(', ')}).\n   HyperPay checkout created: ${checkout.raw}\n   Top up the sandbox wallet (GET /api/v1/account → wallet.top_up_account) or complete the checkout, then:\n       dart run example/golden_path.dart $n');
    exit(0);
  }
  await continueFromPaid(n);
}
