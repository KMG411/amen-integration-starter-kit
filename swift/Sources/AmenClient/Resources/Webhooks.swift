public final class Webhooks {
    let c: AmenClient; init(_ c: AmenClient) { self.c = c }
    public func list() async throws -> [Webhook] { try await c.request(.GET, "/web-hooks/") }
    /// `secretKey` in the response is shown ONLY now — store it in the Keychain / a secret manager immediately.
    public func create(url: String) async throws -> Webhook { try await c.request(.POST, "/web-hooks/", json: ["url": url]) }
    public func delete(_ id: String) async throws { let _: Empty = try await c.request(.DELETE, "/web-hooks/\(id)") }
}
