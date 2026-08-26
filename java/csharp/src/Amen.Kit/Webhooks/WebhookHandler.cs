using System.Collections.Concurrent;
using System.Text.Json.Nodes;

namespace Amen.Kit.Webhooks;

public sealed record WebhookEvent(string Id, string? Type, JsonObject Data, byte[] Raw);
public sealed record WebhookResult(int Status, IReadOnlyDictionary<string, object> Body);

/// <summary>Framework-agnostic (ASP.NET Core minimal API, controllers, Azure Functions): call <see cref="HandleAsync"/> with the headers and RAW body bytes.
/// Verifies first, de-duplicates by id, then dispatches. Keep the callback fast; queue heavy work.</summary>
public sealed class WebhookHandler(string secret, Func<WebhookEvent, Task> onEvent)
{
    readonly ConcurrentDictionary<string, byte> _seen = new();

    public async Task<WebhookResult> HandleAsync(IEnumerable<KeyValuePair<string, string>> headers, byte[] rawBody)
    {
        var sig = headers.FirstOrDefault(h => string.Equals(h.Key, WebhookSignature.Header, StringComparison.OrdinalIgnoreCase)).Value;
        if (!WebhookSignature.Verify(secret, rawBody, sig)) return new(401, new Dictionary<string, object> { ["error"] = "invalid signature" });
        JsonObject data;
        try { data = JsonNode.Parse(rawBody) as JsonObject ?? throw new FormatException(); } catch { return new(400, new Dictionary<string, object> { ["error"] = "invalid json" }); }
        var id = (data["id"] ?? data["event_id"])?.ToString() ?? "";
        if (id.Length > 0 && !_seen.TryAdd(id, 0)) return new(200, new Dictionary<string, object> { ["ok"] = true, ["duplicate"] = true });
        await onEvent(new WebhookEvent(id, (data["event"] ?? data["type"])?.ToString(), data, rawBody));
        return new(200, new Dictionary<string, object> { ["ok"] = true });
    }
}
