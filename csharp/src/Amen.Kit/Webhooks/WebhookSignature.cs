using System.Security.Cryptography;
using System.Text;

namespace Amen.Kit.Webhooks;

/// <summary>Signature verification for Amen deliveries. The signed message is
/// <c>timestamp + "." + rawBody</c> (bytes), and the digest is HMAC-SHA256 (verified against real
/// deliveries 2026-08-30). Header names and algorithm are the one configuration point (docs/04-webhooks.md).</summary>
public static class WebhookSignature
{
    /// <summary>HMAC-SHA256 hex of the signed message, e.g. <c>sha256=&lt;hex&gt;</c>.</summary>
    public const string SignatureHeader = "X-Webhook-Signature";
    /// <summary>ISO-8601 delivery timestamp; part of the signed message and the de-dupe key.</summary>
    public const string TimestampHeader = "X-Webhook-Timestamp";
    /// <summary>Event type, also mirrored as <c>event</c> in the JSON body.</summary>
    public const string EventHeader = "X-Webhook-Event";

    /// <summary>Lowercase hex of HMACSHA256(secret, utf8(timestamp) + "." + rawBody). No prefix.</summary>
    public static string Compute(string secret, string timestamp, ReadOnlySpan<byte> rawBody)
    {
        var ts = Encoding.UTF8.GetBytes(timestamp);
        var message = new byte[ts.Length + 1 + rawBody.Length];
        ts.CopyTo(message, 0);
        message[ts.Length] = (byte)'.';
        rawBody.CopyTo(message.AsSpan(ts.Length + 1));
        return Convert.ToHexString(HMACSHA256.HashData(Encoding.UTF8.GetBytes(secret), message)).ToLowerInvariant();
    }

    /// <summary>Accepts hex digests, optionally prefixed like "sha256=&lt;hex&gt;". Constant-time comparison.
    /// Returns false when the received signature or timestamp is null or empty.</summary>
    public static bool Verify(string secret, string? timestamp, ReadOnlySpan<byte> rawBody, string? received)
    {
        if (string.IsNullOrWhiteSpace(received) || string.IsNullOrWhiteSpace(timestamp)) return false;
        var given = (received.StartsWith("sha256=", StringComparison.OrdinalIgnoreCase) ? received["sha256=".Length..] : received).Trim().ToLowerInvariant();
        return CryptographicOperations.FixedTimeEquals(Encoding.UTF8.GetBytes(Compute(secret, timestamp, rawBody)), Encoding.UTF8.GetBytes(given));
    }
}
