# Amen API — Swift reference implementation

SwiftPM package (`AmenClient`, macOS 13 / iOS 16+, `URLSession` + `async/await` + `Codable`, no dependencies) plus a CLI golden-path example and notes for the iOS proxy pattern.

```sh
swift build
AMN_API_KEY=… swift run golden-path          # or put it in ../.env
swift test                                   # offline unit tests
```

```swift
let amen = try AmenClient()                                   // config from env
let deal = try await amen.deals.create(.init(offerType: .product, offerTitle: "iPhone 15", offerPrice: "3500.00", offerDeliveryFee: "25.00", offerCategory: 12))
_ = try await amen.deals.setParties(deal.number, buyers: [buyer.number], sellers: [seller.number])
do { _ = try await amen.deals.actions.submit(deal.number) }
catch let e as AmenApiError where e.has("deal__delivery_address_required") { /* … */ }
```

> **iOS apps must not hold `AMN_API_KEY`.** Use `AmenClient` on your server (or any server stack in this kit) and have the app call your server. Keep the app's own session token in the Keychain. See `SampleApp/README.md`.
