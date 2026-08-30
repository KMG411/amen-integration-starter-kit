using System.Collections.Concurrent;
using System.Text.Json.Nodes;

namespace Amen.Kit.Webhooks;

public sealed record WebhookEvent(string Id, string? Type, JsonObject Data, byte[] Raw);
public sealed record WebhookResult(int Status, IReadOnlyDictionary<string, object> Body);

/// <summary>Framework-agnostic (ASP.NET Core minimal API, controllers, Azure Functions): call <see cref="HandleAsync"/> with the headers and RAW body bytes.
/// Verifies the signature over <c>timestamp + "." + rawBody</c> first, de-duplicates by the delivery timestamp, then dispatches.
/// Keep the callback fast; queue heavy work.</summary>
public sealed class WebhookHandler(string secret, Func<WebhookEvent, Task> onEvent)
{
    readonly ConcurrentDictionary<string, byte> _seen = new();

    public async Task<WebhookResult> HandleAsync(IEnumerable<KeyValuePair<string, string>> headers, byte[] rawBody)
    {
        var list = headers as ICollection<KeyValuePair<string, string>> ?? headers.ToList();
        string? Header(string name) => list.FirstOrDefault(h => string.Equals(h.Key, name, StringComparison.OrdinalIgnoreCase)).Value;

        var sig = Header(WebhookSignature.SignatureHeader);
        var timestampHeader = Header(WebhookSignature.TimestampHeader);
        if (!WebhookSignature.Verify(secret, timestampHeader, rawBody, sig)) return new(401, new Dictionary<string, object> { ["error"] = "invalid signature" });

        JsonObject data;
        try { data = JsonNode.Parse(rawBody) as JsonObject ?? throw new FormatException(); } catch { return new(400, new Dictionary<string, object> { ["error"] = "invalid json" }); }

        // No event id in the body: de-dupe on the top-level delivery timestamp (body first, header as fallback).
        var id = data["timestamp"]?.ToString() ?? timestampHeader ?? "";
        var type = data["event"]?.ToString() ?? Header(WebhookSignature.EventHeader);
        if (id.Length > 0 && !_seen.TryAdd(id, 0)) return new(200, new Dictionary<string, object> { ["ok"] = true, ["duplicate"] = true });
        await onEvent(new WebhookEvent(id, type, data, rawBody));
        return new(200, new Dictionary<string, object> { ["ok"] = true });
    }
}
