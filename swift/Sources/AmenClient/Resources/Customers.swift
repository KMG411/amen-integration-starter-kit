import Foundation
public final class Customers {
    let c: AmenClient; init(_ c: AmenClient) { self.c = c }
    public func create(_ body: CreateCustomer) async throws -> Customer { try await c.request(.POST, "/customers/", json: body) }
    public func get(_ number: String) async throws -> Customer { try await c.request(.GET, "/customers/\(number)") }
    public func list(page: Int? = nil, perPage: Int? = nil, type: String? = nil, status: String? = nil) async throws -> Page<Customer> {
        try await c.requestPage(.GET, "/customers/", key: "customers", params: ["page": page.map(String.init), "per_page": perPage.map(String.init), "type": type, "status": status])
    }
    /// Iterate every page — never process only the first page by accident.
    public func all(type: String? = nil, status: String? = nil) -> AsyncThrowingStream<Customer, Error> {
        AsyncThrowingStream { cont in Task { do { var page = 0; while true { let p = try await self.list(page: page, type: type, status: status); p.items.forEach { cont.yield($0) }; page += 1; if page >= p.pages || p.items.isEmpty { break } }; cont.finish() } catch { cont.finish(throwing: error) } } }
    }
}

extension AmenClient {
    /// List endpoints return `{ <key>: [...], page, pages, total }` (or a nested `page` object). Decode leniently.
    func requestPage<T: Decodable & Sendable>(_ method: Method, _ path: String, key: String, params: [String: String?] = [:]) async throws -> Page<T> {
        let raw: RawObject = try await request(method, path, params: params)
        let itemsAny = (raw.dict[key] ?? raw.dict["results"] ?? raw.dict["items"])?.value as? [Any] ?? []
        let items = try itemsAny.map { try snakeDecoder.decode(T.self, from: JSONSerialization.data(withJSONObject: $0)) }
        let meta = (raw.dict["page"]?.value as? [String: Any]) ?? raw.dict.mapValues { $0.value }
        return Page(items: items, page: meta["page"] as? Int ?? 0, pages: meta["pages"] as? Int ?? 1, total: meta["total"] as? Int ?? 0)
    }
}

struct RawObject: Decodable { let dict: [String: AnyCodable]; init(from d: Decoder) throws { dict = try d.singleValueContainer().decode([String: AnyCodable].self) } }

/// Minimal type-erased JSON value used only for lenient page decoding.
struct AnyCodable: Decodable { let value: Any
    init(from decoder: Decoder) throws {
        let c = try decoder.singleValueContainer()
        if c.decodeNil() { value = NSNull() } else if let v = try? c.decode(Bool.self) { value = v } else if let v = try? c.decode(Int.self) { value = v }
        else if let v = try? c.decode(Double.self) { value = v } else if let v = try? c.decode(String.self) { value = v }
        else if let v = try? c.decode([AnyCodable].self) { value = v.map(\.value) } else if let v = try? c.decode([String: AnyCodable].self) { value = v.mapValues(\.value) }
        else { throw DecodingError.dataCorruptedError(in: c, debugDescription: "unsupported JSON") }
    }
}
