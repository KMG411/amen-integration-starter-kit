// Golden path (scenario/golden-path.yml). `swift run golden-path [DL-000123]` — pass a deal number to resume from 'paid'.
import Foundation
import AmenClient

func phone(_ p: String) -> String { String((p + String(String(Int(Date().timeIntervalSince1970)).suffix(7))).prefix(9)) }
func step(_ label: String, _ d: Deal? = nil) { print("✔ \(label)\(d.map { " → status=\($0.status)" } ?? "")") }

let amen = try AmenClient()
print("environment: \(amen.config.env) (\(amen.config.baseURL))\n")
func continueFromPaid(_ n: String) async throws {
    step("execution-start", try await amen.deals.actions.executionStart(n))
    step("execution-complete", try await amen.deals.actions.executionComplete(n))
    step("complete", try await amen.deals.actions.complete(n))
    step("transfer-seller-amount (payout)", try await amen.deals.actions.transferSellerAmount(n))
    print("\n🎉 deal \(n) finished: \(try await amen.deals.get(n).status)")
}
if CommandLine.arguments.count > 1 { try await continueFromPaid(CommandLine.arguments[1]); exit(0) }

let buyer = try await amen.customers.create(.init(firstName: "Buyer", lastName: "Kit", phoneCode: "SA", phoneNumber: phone("57")))
let seller = try await amen.customers.create(.init(firstName: "Seller", lastName: "Kit", phoneCode: "SA", phoneNumber: phone("58")))
step("customers \(buyer.number) (buyer), \(seller.number) (seller)")
let category = try await amen.lookups.categories()[0].id, city = try await amen.lookups.cities()[0].id
let deal = try await amen.deals.create(.init(offerType: .product, offerTitle: "Starter Kit golden path", offerPrice: "100.00", offerDeliveryFee: "0.00", offerCategory: category, offerDescription: "Reference deal created by the Amen integration starter kit"))
let n = deal.number; step("deal \(n) created", deal)
step("parties", try await amen.deals.setParties(n, buyers: [buyer.number], sellers: [seller.number]))
step("delivery address", try await amen.deals.setDeliveryAddress(n, .init(city: city, street: "King Fahd Rd", buildingNumber: "1234", zipCode: "12211", district: "Al Olaya", unitNumber: "1")))
step("submit", try await amen.deals.actions.submit(n))
step("approve", try await amen.deals.actions.approve(n))
print("  allowed payment methods: \(try await amen.deals.allowedPaymentMethods(n))")
do { step("pay with wallet", try await amen.deals.actions.payWithWallet(n)) }
catch let e as AmenApiError {
    let checkout = try await amen.deals.actions.payOnline(n)
    print("\n⏸  NEEDS_TOP_UP — wallet payment not possible (\(e.codes.isEmpty ? String(e.status) : e.codes.joined(separator: ", "))).\n   HyperPay checkout created: \(checkout)\n   Top up the sandbox wallet (GET /api/v1/account → wallet.top_up_account) or complete the checkout, then:\n       swift run golden-path \(n)")
    exit(0)
}
try await continueFromPaid(n)
