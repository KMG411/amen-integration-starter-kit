namespace Amen.Kit.Resources;
public sealed class Lookups(AmenClient c)
{
    public Task<List<Lookup>?> CitiesAsync() => c.GetAsync<List<Lookup>>("/cities");
    public Task<List<Lookup>?> CategoriesAsync() => c.GetAsync<List<Lookup>>("/categories/");
    public Task<List<Lookup>?> DisputeReasonsAsync() => c.GetAsync<List<Lookup>>("/dispute-reasons/");
    public Task<List<Lookup>?> DisputeResolutionReasonsAsync() => c.GetAsync<List<Lookup>>("/dispute-resolution-reasons/");
    public Task<List<Lookup>?> CancelReasonsAsync(string? partyType = null) => c.GetAsync<List<Lookup>>("/cancel-reasons/", new Dictionary<string, string?> { ["party_type"] = partyType });
}
