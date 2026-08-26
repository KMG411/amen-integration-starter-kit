import Foundation
public final class AccountResource {
    let c: AmenClient; init(_ c: AmenClient) { self.c = c }
    public func get() async throws -> Account { try await c.request(.GET, "/account") }
    public func bankAccounts() async throws -> [BankAccount] { try await c.request(.GET, "/account/bank-accounts/") }
    public func linkBankAccount(iban: String, proofDocument: MultipartFile? = nil) async throws -> BankAccount { try await c.request(.POST, "/account/bank-accounts/", form: ["iban": iban], files: proofDocument.map { [$0] } ?? []) }
    public func deleteBankAccount(_ id: String) async throws { let _: Empty = try await c.request(.DELETE, "/account/bank-accounts/\(id)") }
}
