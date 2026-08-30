import Foundation
import CryptoKit

/// Signature verification over `timestamp + "." + rawBody`. Matches real Amen deliveries (docs/04-webhooks.md).
/// Delivery headers: `X-Webhook-Signature: sha256=<hex>`, `X-Webhook-Timestamp: <ISO-8601>`, `X-Webhook-Event: <type>`.
public enum WebhookSignature {
    public static let signatureHeader = "X-Webhook-Signature"
    public static let timestampHeader = "X-Webhook-Timestamp"
    public static let eventHeader = "X-Webhook-Event"

    /// Lowercase hex of HMAC-SHA256 over `utf8(timestamp) + "." + rawBody`.
    public static func compute(secret: String, timestamp: String, rawBody: Data) -> String {
        var signed = Data(timestamp.utf8)
        signed.append(0x2E) // "."
        signed.append(rawBody)
        return HMAC<SHA256>.authenticationCode(for: signed, using: SymmetricKey(data: Data(secret.utf8))).map { String(format: "%02x", $0) }.joined()
    }

    /// The full header value: `sha256=<hex>`.
    public static func signatureHeaderValue(secret: String, timestamp: String, rawBody: Data) -> String {
        "sha256=" + compute(secret: secret, timestamp: timestamp, rawBody: rawBody)
    }

    /// Accepts hex digests, optionally prefixed like "sha256=<hex>". Constant-time comparison.
    /// Returns false if either the received signature or the timestamp is nil/empty.
    public static func verify(secret: String, timestamp: String?, rawBody: Data, received: String?) -> Bool {
        guard let received, !received.isEmpty, let timestamp, !timestamp.isEmpty else { return false }
        let given = (received.hasPrefix("sha256=") ? String(received.dropFirst("sha256=".count)) : received).trimmingCharacters(in: .whitespaces).lowercased()
        let expected = compute(secret: secret, timestamp: timestamp, rawBody: rawBody)
        guard given.utf8.count == expected.utf8.count else { return false }
        return zip(given.utf8, expected.utf8).reduce(0) { $0 | ($1.0 ^ $1.1) } == 0
    }
}
