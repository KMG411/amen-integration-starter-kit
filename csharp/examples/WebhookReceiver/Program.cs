// ASP.NET Core minimal API receiver. Reads the RAW body and verifies it before any parsing. Listens on :8080/webhook
using Amen.Kit;
using Amen.Kit.Webhooks;

var secret = Config.FromEnvironment().WebhookSecret ?? throw new InvalidOperationException("AMN_WEBHOOK_SECRET is required");
var handler = new WebhookHandler(secret, e => { Console.WriteLine($"📩 {e.Type} {e.Id}: {e.Data.ToJsonString()[..Math.Min(300, e.Data.ToJsonString().Length)]}"); return Task.CompletedTask; });

var app = WebApplication.CreateBuilder(args).Build();
app.MapPost("/webhook", async (HttpRequest req) =>
{
    using var ms = new MemoryStream(); await req.Body.CopyToAsync(ms);
    var r = await handler.HandleAsync(req.Headers.Select(h => new KeyValuePair<string, string>(h.Key, h.Value.ToString())), ms.ToArray());
    return Results.Json(r.Body, statusCode: r.Status);
});
app.Run("http://0.0.0.0:8080");
