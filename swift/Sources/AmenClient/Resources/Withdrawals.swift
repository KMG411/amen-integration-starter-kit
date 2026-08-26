public final class Withdrawals {
    let c: AmenClient; init(_ c: AmenClient) { self.c = c }
    public func create(bankAccountId: String, amount: String) async throws -> Withdrawal { try await c.request(.POST, "/withdrawals/", json: ["bank_account_id": bankAccountId, "amount": amount]) }
    public func get(_ n: String) async throws -> Withdrawal { try await c.request(.GET, "/withdrawals/\(n)") }
    public func list(page: Int? = nil, pageSize: Int? = nil, status: String? = nil) async throws -> Page<Withdrawal> { try await c.requestPage(.GET, "/withdrawals/", key: "withdrawals", params: ["page": page.map(String.init), "page_size": pageSize.map(String.init), "status": status]) }
}
