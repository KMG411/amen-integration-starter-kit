using System.Net;
using System.Text;
using Amen.Kit;
using Xunit;

public class ClientTests
{
    sealed class Stub(Func<HttpRequestMessage, HttpResponseMessage> f) : HttpMessageHandler
    { protected override Task<HttpResponseMessage> SendAsync(HttpRequestMessage r, CancellationToken _) => Task.FromResult(f(r)); }
    static HttpResponseMessage Json(int status, string body) => new((HttpStatusCode)status) { Content = new StringContent(body, Encoding.UTF8, "application/json") };
    static AmenClient Client(Func<HttpRequestMessage, HttpResponseMessage> f) => new(new Config("sandbox", "test-token", "https://sandbox-api.amnn.sa", MaxRetries: 1), new HttpClient(new Stub(f)));

    [Fact] public async Task AuthHeaderAndBaseUrl()
    {
        var acc = await Client(r => { Assert.Equal("https://sandbox-api.amnn.sa/api/v1/account", r.RequestUri!.ToString()); Assert.Equal("test-token", r.Headers.GetValues("X-API-Token").Single()); return Json(200, "{\"id\":\"a1\",\"name\":\"test\"}"); }).Account.GetAsync();
        Assert.Equal("a1", acc!.Id);
    }
    [Fact] public async Task ErrorCodesParsed()
    {
        var e = await Assert.ThrowsAsync<AmenApiError>(() => Client(_ => Json(400, "{\"error\":[\"first_name__required\"]}")).Customers.CreateAsync(new("", "x", "SA", "5")));
        Assert.Equal(400, e.Status); Assert.True(e.Has("first_name__required")); Assert.False(e.Retryable);
    }
    [Fact] public async Task Retries429ThenSucceeds()
    {
        var calls = 0;
        var cities = await Client(_ => ++calls == 1 ? Json(429, "{\"error\":[\"rate_limit__exceeded\"]}") : Json(200, "[{\"id\":1,\"name\":\"Riyadh\"}]")).Lookups.CitiesAsync();
        Assert.Equal(1, cities![0].Id); Assert.Equal(2, calls);
    }
    [Fact] public async Task LifecycleGuardBlocksLocally()
    {
        var calls = 0;
        await Assert.ThrowsAsync<AmenLifecycleError>(() => Client(_ => { calls++; return Json(200, "{\"number\":\"DL-1\",\"status\":\"draft\"}"); }).Deals.Actions.ApproveAsync("DL-1"));
        Assert.Equal(1, calls);
    }
    [Fact] public async Task OriginOnMutatingRequests()
    {
        var wh = await Client(r => { Assert.Equal("https://sandbox-api.amnn.sa", r.Headers.GetValues("Origin").Single()); return Json(201, "{\"id\":\"w\",\"url\":\"u\",\"secret_key\":\"s\"}"); }).Webhooks.CreateAsync("https://example.com/hook");
        Assert.Equal("s", wh.SecretKey);
    }
    [Fact] public void SnakeCaseAndEpochMillis()
    {
        Assert.Contains("\"offer_type\"", System.Text.Json.JsonSerializer.Serialize(new CreateDeal("product", "t") { OfferPrice = "1.00" }, Amen.Kit.Json.Options));
        var deal = System.Text.Json.JsonSerializer.Deserialize<Deal>("{\"number\":\"DL-1\",\"status\":\"draft\",\"created_at\":1679568486000,\"unknown\":1}", Amen.Kit.Json.Options)!;
        Assert.Equal(2023, Amen.Kit.Json.ToDate(deal.CreatedAt)!.Value.Year);
    }
}
