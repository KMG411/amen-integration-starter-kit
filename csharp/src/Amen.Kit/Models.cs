using System.Text.Json;
using System.Text.Json.Serialization;

namespace Amen.Kit;

/// <summary>Models mirror openapi/openapi.yml. Money is a string ("100.00"); timestamps are epoch milliseconds. Unknown fields are ignored.</summary>
public static class Json
{
    public static readonly JsonSerializerOptions Options = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.SnakeCaseLower, DictionaryKeyPolicy = JsonNamingPolicy.SnakeCaseLower,
        DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull, PropertyNameCaseInsensitive = true,
    };
    /// <summary>Parse an API timestamp: ISO-8601 string (e.g. "2026-08-26T18:04:42.825Z").</summary>
    public static DateTimeOffset? ToDate(string? iso) => string.IsNullOrEmpty(iso) ? null : (DateTimeOffset.TryParse(iso, System.Globalization.CultureInfo.InvariantCulture, System.Globalization.DateTimeStyles.RoundtripKind, out var d) ? d : null);
}

public sealed record Customer(string? Id, string Number, string? FirstName, string? LastName, string? Status, string? CreatedAt);
public sealed record Deal(string? Id, string Number, string Status, string? Price, string? CreatedAt, string? UpdatedAt);
public sealed record Checkout(int? Id, string? Provider, Dictionary<string, object?>? Hyperpay, string? Amount);
public sealed record Withdrawal(string? Id, string Number, string Status, string? Amount);
/// <summary>SecretKey is returned ONLY at creation — store it in a secret manager immediately.</summary>
public sealed record Webhook(string Id, string Url, string? SecretKey);
public sealed record Account(string? Id, string? Name, Dictionary<string, object?>? Wallet);
public sealed record Lookup(int Id, string? Name);
public sealed record BankAccount(string Id, string? Iban, string? Status);
public sealed record Page<T>(IReadOnlyList<T> Items, int PageNumber, int Pages, int Total);

public sealed record CreateCustomer(string FirstName, string LastName, string PhoneCode, string PhoneNumber);
public sealed record CreateDeal(string OfferType, string OfferTitle)
{
    public string? OfferPrice { get; init; } public string? OfferDeliveryFee { get; init; } public int? OfferCategory { get; init; }
    public string? OfferDescription { get; init; } public string? DealSubjectDetails { get; init; }
}
public sealed record Address(int City, string Street, string BuildingNumber, string ZipCode, string? District = null, string? UnitNumber = null);
