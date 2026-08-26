namespace Amen.Kit.Resources;
public sealed class Withdrawals(AmenClient c)
{
    public async Task<Withdrawal> CreateAsync(string bankAccountId, string amount) => (await c.PostAsync<Withdrawal>("/withdrawals/", new { bank_account_id = bankAccountId, amount }))!;
    public async Task<Withdrawal> GetAsync(string n) => (await c.GetAsync<Withdrawal>($"/withdrawals/{n}"))!;
    public Task<Page<Withdrawal>> ListAsync(IDictionary<string, string?>? query = null) => c.PageAsync<Withdrawal>("/withdrawals/", query, "withdrawals");
}
