using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using System.Text.Json.Nodes;
using Amen.Kit.Resources;

namespace Amen.Kit;

/// <summary>AmenClient — the one place that knows about auth headers, base URL, timeouts and retries.</summary>
public sealed class AmenClient
{
    public Config Config { get; }
    readonly HttpClient _http;
    readonly string _csrf = Guid.NewGuid().ToString("N");   // 32 hex chars — Django CSRF token format
    public Lookups Lookups { get; } public AccountResource Account { get; } public Customers Customers { get; }
    public Deals Deals { get; } public Withdrawals Withdrawals { get; } public WebhooksResource Webhooks { get; }

    public AmenClient() : this(Config.FromEnvironment()) { }
    public AmenClient(Config config, HttpClient? httpClient = null)
    {
        Config = config;
        // UseCookies=false so our manual csrftoken Cookie header is not stripped by the handler
        _http = httpClient ?? new HttpClient(new HttpClientHandler { UseCookies = false }) { Timeout = TimeSpan.FromMilliseconds(config.TimeoutMs) };
        Lookups = new(this); Account = new(this); Customers = new(this); Deals = new(this); Withdrawals = new(this); Webhooks = new(this);
    }

    public Task<T?> GetAsync<T>(string path, IDictionary<string, string?>? query = null) => RequestAsync<T>(HttpMethod.Get, path, query: query);
    public Task<T?> PostAsync<T>(string path, object? json) => RequestAsync<T>(HttpMethod.Post, path, json: json);
    public Task<T?> PostFormAsync<T>(string path, MultipartFormDataContent form) => RequestAsync<T>(HttpMethod.Post, path, form: form);
    public Task<T?> PutAsync<T>(string path, object json) => RequestAsync<T>(HttpMethod.Put, path, json: json);
    public Task DeleteAsync(string path) => RequestAsync<JsonNode>(HttpMethod.Delete, path);

    public async Task<T?> RequestAsync<T>(HttpMethod method, string path, IDictionary<string, string?>? query = null, object? json = null, MultipartFormDataContent? form = null)
    {
        var qs = query is null ? "" : string.Join("&", query.Where(kv => kv.Value is not null).Select(kv => $"{Uri.EscapeDataString(kv.Key)}={Uri.EscapeDataString(kv.Value!)}"));
        var url = Config.BaseUrl + Config.ApiPrefix + path + (qs.Length == 0 ? "" : "?" + qs);

        for (var attempt = 1; ; attempt++)
        {
            using var req = new HttpRequestMessage(method, url);
            req.Headers.TryAddWithoutValidation("X-API-Token", Config.ApiKey);
            req.Headers.Accept.Add(new MediaTypeWithQualityHeaderValue("application/json"));
            req.Headers.UserAgent.ParseAdd("amen-starter-kit-dotnet/0.1");
            req.Headers.TryAddWithoutValidation("Accept-Language", "en");
            req.Headers.TryAddWithoutValidation("Cookie", $"csrftoken={_csrf}");
            if (method != HttpMethod.Get) {  // Django CSRF double-submit: token in both the X-CSRFToken header and the csrftoken cookie
                req.Headers.TryAddWithoutValidation("X-CSRFToken", _csrf);
                req.Headers.TryAddWithoutValidation("Origin", Config.BaseUrl);
                req.Headers.Referrer = new Uri(Config.BaseUrl);
            }
            if (form is not null) req.Content = form;
            else if (json is not null) req.Content = new StringContent(JsonSerializer.Serialize(json, Json.Options), Encoding.UTF8, "application/json");

            HttpResponseMessage res;
            try { res = await _http.SendAsync(req); }
            catch (HttpRequestException) when (attempt <= Config.MaxRetries) { await Task.Delay(Backoff(attempt)); continue; }
            var body = await res.Content.ReadAsStringAsync();
            if ((int)res.StatusCode < 400)
                return string.IsNullOrWhiteSpace(body) ? default : JsonSerializer.Deserialize<T>(body, Json.Options);
            var err = ToError((int)res.StatusCode, body, method.Method, Config.ApiPrefix + path);
            if (err.Retryable && attempt <= Config.MaxRetries) { await Task.Delay(Backoff(attempt, res.Headers.RetryAfter?.Delta)); continue; }
            throw err;
        }
    }

    internal static AmenApiError ToError(int status, string body, string method, string path)
    {
        var codes = new List<string>();
        try { var e = JsonNode.Parse(body)?["error"]; if (e is JsonArray a) codes.AddRange(a.Select(x => x?.ToString() ?? "")); else if (e is not null) codes.Add(e.ToString()); } catch { }
        return new AmenApiError(status, codes, method, path, body);
    }
    static TimeSpan Backoff(int attempt, TimeSpan? retryAfter = null) => retryAfter ?? TimeSpan.FromMilliseconds(Math.Min(1 << attempt, 20) * 1000 + Random.Shared.Next(1000));

    /// <summary>Lenient page decoding: <c>{ &lt;key&gt;: [...], page, pages, total }</c> or a nested <c>page</c> object.</summary>
    public async Task<Page<T>> PageAsync<T>(string path, IDictionary<string, string?>? query, string key)
    {
        var n = await GetAsync<JsonNode>(path, query);
        if (n is null) return new Page<T>([], 0, 1, 0);
        var arr = (n[key] ?? n["results"] ?? n["items"]) as JsonArray ?? [];
        var items = arr.Select(x => x!.Deserialize<T>(Json.Options)!).ToList();
        var meta = n["page"] is JsonObject po ? po : n.AsObject();
        int I(string k) => meta[k] is JsonValue v && v.TryGetValue<int>(out var i) ? i : 0;
        return new Page<T>(items, I("page"), meta["pages"] is null ? 1 : I("pages"), I("total"));
    }
}
