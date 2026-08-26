import Foundation

/// Models mirror openapi/openapi.yml. Money is a String ("100.00"); timestamps are epoch **milliseconds**.
/// Unknown JSON fields are ignored by Codable, so additive API changes never break the client.
public struct Customer: Codable, Sendable { public let id: String?; public let number: String; public let firstName: String?; public let lastName: String?; public let status: String?; public let createdAt: String? }
public struct Deal: Codable, Sendable { public let id: String?; public let number: String; public let status: String; public let price: String?; public let createdAt: String?; public let updatedAt: String?
    /// The API returns ISO-8601 strings (e.g. "2026-08-26T18:04:42.825Z").
    public var created: Date? { createdAt.flatMap(parseAmenDate) } }

let amenISOFormatter: ISO8601DateFormatter = { let f = ISO8601DateFormatter(); f.formatOptions = [.withInternetDateTime, .withFractionalSeconds]; return f }()
public func parseAmenDate(_ s: String) -> Date? { amenISOFormatter.date(from: s) ?? ISO8601DateFormatter().date(from: s) }
public struct Checkout: Codable, Sendable { public struct HyperPay: Codable, Sendable { public let checkoutId: String? }; public let id: Int?; public let provider: String?; public let hyperpay: HyperPay?; public let amount: String? }
public struct Withdrawal: Codable, Sendable { public let id: String?; public let number: String; public let status: String; public let amount: String? }
public struct Webhook: Codable, Sendable { public let id: String; public let url: String; /// Returned ONLY at creation — store it in the Keychain / a secret manager immediately.
    public let secretKey: String? }
public struct Account: Codable, Sendable { public struct Wallet: Codable, Sendable { public let balance: String?; public let available: String?; public let escrow: String?; public let onHold: String? }; public let id: String?; public let name: String?; public let wallet: Wallet? }
public struct Lookup: Codable, Sendable { public let id: Int; public let name: String? }
public struct PaymentMethods: Codable, Sendable { public let paymentMethods: [String]? }
public struct BankAccount: Codable, Sendable { public let id: String; public let iban: String?; public let status: String? }

public enum OfferType: String, Codable, Sendable { case product, service }
public struct CreateCustomer: Codable, Sendable { public var firstName, lastName, phoneCode, phoneNumber: String
    public init(firstName: String, lastName: String, phoneCode: String, phoneNumber: String) { self.firstName = firstName; self.lastName = lastName; self.phoneCode = phoneCode; self.phoneNumber = phoneNumber } }
public struct CreateDeal: Codable, Sendable { public var offerType: OfferType; public var offerTitle: String; public var offerPrice: String?; public var offerDeliveryFee: String?; public var offerCategory: Int?; public var offerDescription: String?; public var dealSubjectDetails: String?
    public init(offerType: OfferType, offerTitle: String, offerPrice: String? = nil, offerDeliveryFee: String? = nil, offerCategory: Int? = nil, offerDescription: String? = nil, dealSubjectDetails: String? = nil) {
        self.offerType = offerType; self.offerTitle = offerTitle; self.offerPrice = offerPrice; self.offerDeliveryFee = offerDeliveryFee; self.offerCategory = offerCategory; self.offerDescription = offerDescription; self.dealSubjectDetails = dealSubjectDetails } }
public struct Address: Codable, Sendable { public var city: Int; public var street, buildingNumber, zipCode: String; public var district, unitNumber: String?
    public init(city: Int, street: String, buildingNumber: String, zipCode: String, district: String? = nil, unitNumber: String? = nil) { self.city = city; self.street = street; self.buildingNumber = buildingNumber; self.zipCode = zipCode; self.district = district; self.unitNumber = unitNumber } }

/// Normalised page wrapper over the API's list responses.
public struct Page<T: Decodable & Sendable>: Sendable { public let items: [T]; public let page: Int; public let pages: Int; public let total: Int }

public struct Empty: Codable, Sendable { public init() {} }
let snakeDecoder: JSONDecoder = { let d = JSONDecoder(); d.keyDecodingStrategy = .convertFromSnakeCase; return d }()
let snakeEncoder: JSONEncoder = { let e = JSONEncoder(); e.keyEncodingStrategy = .convertToSnakeCase; return e }()
