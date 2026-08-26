namespace Amen.Kit.Resources;
public sealed class Webhooks(AmenClient c)
{
    public async Task<List<Webhook>> ListAsync() => await c.GetAsync<List<Webhook>>("/web-hooks/") ?? [];
    /// <summary>SecretKey in the response is shown ONLY now — store it in a secret manager immediately.</summary>
    public async Task<Webhook> CreateAsync(string url) => (await c.PostAsync<Webhook>("/web-hooks/", new { url }))!;
    public Task DeleteAsync(string id) => c.DeleteAsync($"/web-hooks/{id}");
}
