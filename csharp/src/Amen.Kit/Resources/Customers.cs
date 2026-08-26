namespace Amen.Kit.Resources;
public sealed class Customers(AmenClient c)
{
    public async Task<Customer> CreateAsync(CreateCustomer body) => (await c.PostAsync<Customer>("/customers/", body))!;
    public async Task<Customer> GetAsync(string customerNumber) => (await c.GetAsync<Customer>($"/customers/{customerNumber}"))!;
    public Task<Page<Customer>> ListAsync(IDictionary<string, string?>? query = null) => c.PageAsync<Customer>("/customers/", query, "customers");
    /// <summary>Iterate every page — never process only the first page by accident.</summary>
    public async IAsyncEnumerable<Customer> AllAsync(IDictionary<string, string?>? filters = null)
    {
        for (var page = 0; ; page++)
        {
            var q = new Dictionary<string, string?>(filters ?? new Dictionary<string, string?>()) { ["page"] = page.ToString() };
            var p = await ListAsync(q);
            foreach (var i in p.Items) yield return i;
            if (page + 1 >= p.Pages || p.Items.Count == 0) yield break;
        }
    }
}
