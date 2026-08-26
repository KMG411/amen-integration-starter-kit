# Amen API — Flutter / Dart reference implementation

Two parts:

| Folder | What | Verified by |
|---|---|---|
| [`amen_client/`](amen_client/) | Pure-Dart package (`package:http`), usable from Flutter, Dart CLI and server-side Dart. Contains the client, models, lifecycle guard, retries and webhook verification. | `dart test` (CI) |
| [`sample_app/`](sample_app/) | Minimal Flutter app showing the **back-end proxy pattern**: the app never holds the Amen API token; it calls *your* server, which uses `amen_client`. Uses `flutter_secure_storage` for the app's own session token. | manual (`flutter run`) |

```sh
cd amen_client
dart pub get
AMN_API_KEY=… dart run example/golden_path.dart     # or put it in ../../.env
dart test                                            # offline unit tests
```

```dart
final amen = AmenClient(Config.fromEnvironment(apiKey: '…'));
final deal = await amen.deals.create(offerType: 'product', offerTitle: 'iPhone 15', offerPrice: '3500.00', offerDeliveryFee: '25.00', offerCategory: 12);
await amen.deals.setParties(deal.number, buyers: [buyer.number], sellers: [seller.number]);
try { await amen.deals.actions.submit(deal.number); }
on AmenApiError catch (e) { if (e.has('deal__delivery_address_required')) { /* … */ } else rethrow; }
```

> **Never embed `AMN_API_KEY` in a mobile app.** Anyone can extract it from the binary. The token belongs on your server; the app talks to your server (see `sample_app/`).
