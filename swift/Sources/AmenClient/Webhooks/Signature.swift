import Foundation
import CryptoKit

/// Signature verification over the RAW body. Header name and algorithm are one configuration point (docs/04-webhooks.md).
public enum WebhookSignature {
    public static let header = "X-Signature"

    public static func compute(secret: String, rawBody: Data) -> String {
        HMAC<SHA256>.authenticationCode(for: rawBody, using: SymmetricKey(data: Data(secret.utf8))).map { String(format: "%02x", $0) }.joined()
    }
    /// Accepts hex digests, optionally prefixed like "sha256=<hex>". Constant-time comparison.
    public static func verify(secret: String, rawBody: Data, received: String?) -> Bool {
        guard let received, !received.isEmpty else { return false }
        let given = (received.contains("=") ? String(received.split(separator: "=", maxSplits: 1)[1]) : received).trimmingCharacters(in: .whitespaces).lowercased()
        let expected = compute(secret: secret, rawBody: rawBody)
        guard given.utf8.count == expected.utf8.count else { return false }
        return zip(given.utf8, expected.utf8).reduce(0) { $0 | ($1.0 ^ $1.1) } == 0
    }
}
