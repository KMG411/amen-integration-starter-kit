# Amen API — Kotlin / Android reference implementation

| Module | What |
|---|---|
| `amen-client/` | Kotlin JVM library: OkHttp + kotlinx.serialization + coroutines (`suspend` API). Usable from Android and server-side Kotlin/Ktor. |
| `golden-path/` | CLI example that walks `scenario/golden-path.yml`. |
| `sample-android/` | Notes + snippet for the **back-end proxy pattern** on Android (EncryptedSharedPreferences / Keystore, Retrofit to *your* server). |

```sh
gradle :amen-client:test                                   # offline unit tests (Gradle 8.x)
AMN_API_KEY=… gradle :golden-path:run                      # or put the key in ../.env
```

```kotlin
val amen = AmenClient(Config.fromEnvironment())
val deal = amen.deals.create(CreateDeal(offerType = "product", offerTitle = "iPhone 15", offerPrice = "3500.00", offerDeliveryFee = "25.00", offerCategory = 12))
amen.deals.setParties(deal.number, buyers = listOf(buyer.number), sellers = listOf(seller.number))
try { amen.deals.actions.submit(deal.number) }
catch (e: AmenApiError) { if (e.has("deal__delivery_address_required")) { /* … */ } else throw e }
```

> **Android apps must not hold `AMN_API_KEY`.** Use `amen-client` on your server and have the app call your server (see `sample-android/`).
