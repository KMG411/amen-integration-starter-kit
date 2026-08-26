using System.Security.Cryptography;
using System.Text;

namespace Amen.Kit.Webhooks;

/// <summary>Signature verification over the RAW body. Header name and algorithm are one configuration point (docs/04-webhooks.md).</summary>
public static class WebhookSignature
{
    public const string Header = "X-Signature";
    public static string Compute(string secret, ReadOnlySpan<byte> rawBody) => Convert.ToHexString(HMACSHA256.HashData(Encoding.UTF8.GetBytes(secret), rawBody)).ToLowerInvariant();
    /// <summary>Accepts hex digests, optionally prefixed like "sha256=&lt;hex&gt;". Constant-time comparison.</summary>
    public static bool Verify(string secret, ReadOnlySpan<byte> rawBody, string? received)
    {
        if (string.IsNullOrWhiteSpace(received)) return false;
        var given = (received.Contains('=') ? received[(received.IndexOf('=') + 1)..] : received).Trim().ToLowerInvariant();
        return CryptographicOperations.FixedTimeEquals(Encoding.UTF8.GetBytes(Compute(secret, rawBody)), Encoding.UTF8.GetBytes(given));
    }
}
