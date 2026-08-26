namespace Amen.Kit;

/// <summary>Environment-based configuration. Walks up from cwd (max 3 levels) to find a .env. Existing env vars win.</summary>
public sealed record Config(string Env, string ApiKey, string BaseUrl, int TimeoutMs = 20000, string? WebhookSecret = null, int MaxRetries = 3)
{
    public static readonly IReadOnlyDictionary<string, string> BaseUrls = new Dictionary<string, string> { ["sandbox"] = "https://sandbox-api.amnn.sa", ["live"] = "https://api.amnn.sa" };
    public const string ApiPrefix = "/api/v1";

    public static Config FromEnvironment()
    {
        var file = LoadDotenv();
        string? Get(string k) => Environment.GetEnvironmentVariable(k) ?? (file.TryGetValue(k, out var v) ? v : null);
        var env = (Get("AMN_ENV") ?? "sandbox").ToLowerInvariant();
        if (!BaseUrls.ContainsKey(env)) throw new ArgumentException($"AMN_ENV must be 'sandbox' or 'live', got '{env}'");
        var key = Get("AMN_API_KEY");
        if (string.IsNullOrWhiteSpace(key)) throw new InvalidOperationException("AMN_API_KEY is not set (see .env.example)");
        var secret = Get("AMN_WEBHOOK_SECRET");
        return new Config(env, key, Get("AMN_BASE_URL") ?? BaseUrls[env], int.TryParse(Get("AMN_TIMEOUT_MS"), out var t) ? t : 20000, string.IsNullOrWhiteSpace(secret) ? null : secret);
    }

    static Dictionary<string, string> LoadDotenv()
    {
        var dir = new DirectoryInfo(Directory.GetCurrentDirectory());
        for (var i = 0; i < 3 && dir != null; i++, dir = dir.Parent)
        {
            var f = Path.Combine(dir.FullName, ".env");
            if (!File.Exists(f)) continue;
            var outp = new Dictionary<string, string>();
            foreach (var raw in File.ReadAllLines(f))
            {
                var l = raw.Trim(); var eq = l.IndexOf('=');
                if (l.Length == 0 || l.StartsWith('#') || eq < 0) continue;
                var v = System.Text.RegularExpressions.Regex.Replace(l[(eq + 1)..], @"\s+#.*$", "").Trim();
                if (v.Length > 0) outp[l[..eq].Trim()] = v;
            }
            return outp;
        }
        return new();
    }
}
