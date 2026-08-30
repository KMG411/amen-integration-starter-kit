import Foundation

public struct WebhookEvent: Sendable { public let id: String; public let type: String?; public let timestamp: String?; public let data: [String: Any]; public let raw: Data
    public init(id: String, type: String?, timestamp: String?, data: [String: Any], raw: Data) { self.id = id; self.type = type; self.timestamp = timestamp; self.data = data; self.raw = raw } }
public struct WebhookResult: Sendable { public let status: Int; public let body: [String: Any] }

/// Framework-agnostic (Vapor, Hummingbird, …): call `handle(headers:rawBody:)` with the RAW body bytes.
/// Verifies over `timestamp + "." + rawBody`, de-duplicates by the top-level event `timestamp`, then dispatches.
/// Keep `onEvent` fast; queue heavy work.
public final class WebhookHandler: @unchecked Sendable {
    let secret: String; let onEvent: (WebhookEvent) async -> Void
    var seen = Set<String>(); let lock = NSLock()
    public init(secret: String, onEvent: @escaping (WebhookEvent) async -> Void) { self.secret = secret; self.onEvent = onEvent }

    public func handle(headers: [String: String], rawBody: Data) async -> WebhookResult {
        func header(_ name: String) -> String? { headers.first { $0.key.caseInsensitiveCompare(name) == .orderedSame }?.value }
        let sig = header(WebhookSignature.signatureHeader)
        let timestamp = header(WebhookSignature.timestampHeader)
        guard WebhookSignature.verify(secret: secret, timestamp: timestamp, rawBody: rawBody, received: sig) else { return WebhookResult(status: 401, body: ["error": "invalid signature"]) }
        guard let data = (try? JSONSerialization.jsonObject(with: rawBody)) as? [String: Any] else { return WebhookResult(status: 400, body: ["error": "invalid json"]) }
        let type = (data["event"] as? String) ?? header(WebhookSignature.eventHeader)
        // No event id in the body — de-dupe on the top-level event `timestamp` (fall back to the header).
        let id = (data["timestamp"] as? String) ?? timestamp ?? ""
        if !id.isEmpty {
            lock.lock(); let dup = seen.contains(id); if !dup { seen.insert(id) }; lock.unlock()
            if dup { return WebhookResult(status: 200, body: ["ok": true, "duplicate": true]) }
        }
        await onEvent(WebhookEvent(id: id, type: type, timestamp: (data["timestamp"] as? String) ?? timestamp, data: data, raw: rawBody))
        return WebhookResult(status: 200, body: ["ok": true])
    }
}
