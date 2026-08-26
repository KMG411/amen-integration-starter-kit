import Foundation

/// Which statuses each action may be called from (docs/02-deal-lifecycle.md).
public let allowedFrom: [String: Set<String>] = [
    "submit": ["draft"], "approve": ["requested"],
    "make-payment-wallet": ["payment_pending"], "make-payment-online": ["payment_pending"],
    "execution-start": ["paid"], "execution-complete": ["executing"], "complete": ["executed"],
    "transfer-seller-amount": ["completed"], "dispute": ["completed"],
    "dispute-approve": ["disputed"], "dispute-decline": ["disputed"],
    "cancel": ["draft", "requested", "payment_pending", "paid", "executing"],
]

public final class DealActions {
    let c: AmenClient; unowned let deals: Deals
    init(_ c: AmenClient, _ deals: Deals) { self.c = c; self.deals = deals }

    private func act<T: Decodable>(_ n: String, _ action: String, json: (some Encodable)? = Optional<Empty>.none, form: [String: String]? = nil, files: [MultipartFile] = [], check: Bool) async throws -> T {
        if check {
            let status = try await deals.get(n).status
            guard allowedFrom[action]!.contains(status) else { throw AmenLifecycleError(description: "action '\(action)' is not allowed from status '\(status)' (allowed: \(allowedFrom[action]!.sorted().joined(separator: ", ")))") }
        }
        return try await c.request(.POST, "/deals/\(n)/action/\(action)", json: json, form: form, files: files)
    }
    public func submit(_ n: String, check: Bool = true) async throws -> Deal { try await act(n, "submit", check: check) }
    public func approve(_ n: String, price: String? = nil, check: Bool = true) async throws -> Deal { try await act(n, "approve", json: ["price": price].compactMapValues { $0 }, check: check) }
    public func payWithWallet(_ n: String, check: Bool = true) async throws -> Deal { try await act(n, "make-payment-wallet", check: check) }
    public func payOnline(_ n: String, paymentMethod: String = "mada", check: Bool = true) async throws -> Checkout { try await act(n, "make-payment-online", json: ["payment_method": paymentMethod], check: check) }
    public func executionStart(_ n: String, check: Bool = true) async throws -> Deal { try await act(n, "execution-start", check: check) }
    public func executionComplete(_ n: String, check: Bool = true) async throws -> Deal { try await act(n, "execution-complete", check: check) }
    public func complete(_ n: String, check: Bool = true) async throws -> Deal { try await act(n, "complete", check: check) }
    public func transferSellerAmount(_ n: String, check: Bool = true) async throws -> Deal { try await act(n, "transfer-seller-amount", check: check) }
    public func cancel(_ n: String, dealParty: String, reason: Int, comment: String, check: Bool = true) async throws -> Deal {
        struct B: Encodable { let dealParty: String; let reason: Int; let comment: String }
        return try await act(n, "cancel", json: B(dealParty: dealParty, reason: reason, comment: comment), check: check)
    }
    public func dispute(_ n: String, reason: Int, comment: String, attachments: [MultipartFile] = [], check: Bool = true) async throws -> Deal {
        try await act(n, "dispute", form: ["reason": String(reason), "comment": comment], files: attachments.enumerated().map { MultipartFile(field: "attachment_\($0.offset + 1)", filename: $0.element.filename, data: $0.element.data, mimeType: $0.element.mimeType) }, check: check)
    }
    public func disputeApprove(_ n: String, reason: Int, comment: String, check: Bool = true) async throws -> Deal { try await act(n, "dispute-approve", form: ["reason": String(reason), "comment": comment], check: check) }
    public func disputeDecline(_ n: String, reason: Int, comment: String, check: Bool = true) async throws -> Deal { try await act(n, "dispute-decline", form: ["reason": String(reason), "comment": comment], check: check) }
}

public final class Deals {
    let c: AmenClient
    public private(set) lazy var actions = DealActions(c, self)
    init(_ c: AmenClient) { self.c = c }
    public func create(_ body: CreateDeal) async throws -> Deal { try await c.request(.POST, "/deals/", json: body) }
    public func get(_ n: String) async throws -> Deal { try await c.request(.GET, "/deals/\(n)") }
    public func update(_ n: String, _ body: CreateDeal) async throws -> Deal { try await c.request(.PUT, "/deals/\(n)", json: body) }
    public func delete(_ n: String) async throws { let _: Empty = try await c.request(.DELETE, "/deals/\(n)") }
    public func list(page: Int? = nil, perPage: Int? = nil, status: String? = nil) async throws -> Page<Deal> { try await c.requestPage(.GET, "/deals/", key: "deals", params: ["page": page.map(String.init), "per_page": perPage.map(String.init), "status": status]) }
    public func setParties(_ n: String, buyers: [String], sellers: [String]) async throws -> Deal { try await c.request(.POST, "/deals/\(n)/parties/", json: ["buyers": buyers, "sellers": sellers]) }
    public func setDeliveryAddress(_ n: String, _ a: Address) async throws -> Deal { try await c.request(.POST, "/deals/\(n)/delivery-address", json: a) }
    public func setBillingAddress(_ n: String, _ a: Address) async throws -> Deal { try await c.request(.POST, "/deals/\(n)/billing-address", json: a) }
    public func allowedPaymentMethods(_ n: String) async throws -> [String] { let r: PaymentMethods = try await c.request(.GET, "/deals/\(n)/allowed-payment-methods/"); return r.paymentMethods ?? [] }
}
