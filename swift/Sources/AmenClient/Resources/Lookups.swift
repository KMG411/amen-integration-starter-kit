public final class Lookups {
    let c: AmenClient; init(_ c: AmenClient) { self.c = c }
    public func cities() async throws -> [Lookup] { try await c.request(.GET, "/cities") }
    public func categories() async throws -> [Lookup] { try await c.request(.GET, "/categories/") }
    public func disputeReasons() async throws -> [Lookup] { try await c.request(.GET, "/dispute-reasons/") }
    public func disputeResolutionReasons() async throws -> [Lookup] { try await c.request(.GET, "/dispute-resolution-reasons/") }
    public func cancelReasons(partyType: String? = nil) async throws -> [Lookup] { try await c.request(.GET, "/cancel-reasons/", params: ["party_type": partyType]) }
}
