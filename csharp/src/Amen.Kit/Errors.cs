namespace Amen.Kit;

/// <summary>Any non-2xx response. <see cref="Codes"/> holds the API's error codes, e.g. "price__required".</summary>
public sealed class AmenApiError(int status, IReadOnlyList<string> codes, string method, string path, string body)
    : Exception($"{status} {method} {path}: {(codes.Count == 0 ? body : string.Join(", ", codes))}")
{
    public int Status { get; } = status;
    public IReadOnlyList<string> Codes { get; } = codes;
    public string Method { get; } = method;
    public string Path { get; } = path;
    public string Body { get; } = body;
    public bool Has(string code) => Codes.Contains(code);
    public bool Retryable => Status == 429 || Status >= 500;
}

/// <summary>Thrown locally, before any HTTP call, when an action is not valid for the deal's status.</summary>
public sealed class AmenLifecycleError(string message) : InvalidOperationException(message);
