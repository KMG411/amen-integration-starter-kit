import Foundation

public struct WebhookEvent: Sendable { public let id: String; public let type: String?; public let data: [String: Any]; public let raw: Data
    public init(id: String, type: String?, data: [String: Any], raw: Data) { self.id = id; self.type = type; self.data = data; self.raw = raw } }
public struct WebhookResult: Sendable { public let status: Int; public let body: [String: Any] }

/// Framework-agnostic (Vapor, Hummingbird, …): call `handle(headers:rawBody:)` with the RAW body bytes.
/// Verifies first, de-duplicates by id, then dispatches. Keep `onEvent` fast; queue heavy work.
public final class WebhookHandler: @unchecked Sendable {
    let secret: String; let onEvent: (WebhookEvent) async -> Void
    var seen = Set<String>(); let lock = NSLock()
    public init(secret: String, onEvent: @escaping (WebhookEvent) async -> Void) { self.secret = secret; self.onEvent = onEvent }

    public func handle(headers: [String: String], rawBody: Data) async -> WebhookResult {
        let sig = headers.first { $0.key.caseInsensitiveCompare(WebhookSignature.header) == .orderedSame }?.value
        guard WebhookSignature.verify(secret: secret, rawBody: rawBody, received: sig) else { return WebhookResult(status: 401, body: ["error": "invalid signature"]) }
        guard let data = (try? JSONSerialization.jsonObject(with: rawBody)) as? [String: Any] else { return WebhookResult(status: 400, body: ["error": "invalid json"]) }
        let id = (data["id"] ?? data["event_id"]).map { "\($0)" } ?? ""
        if !id.isEmpty {
            lock.lock(); let dup = seen.contains(id); if !dup { seen.insert(id) }; lock.unlock()
            if dup { return WebhookResult(status: 200, body: ["ok": true, "duplicate": true]) }
        }
        await onEvent(WebhookEvent(id: id, type: (data["event"] ?? data["type"]) as? String, data: data, raw: rawBody))
        return WebhookResult(status: 200, body: ["ok": true])
    }
}
