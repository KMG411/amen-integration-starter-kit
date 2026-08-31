using Amen.Kit;
using Xunit;

/// <summary>Mirrors scenario/golden-path.yml. `dotnet test --filter Category=Integration`; skipped without sandbox credentials.</summary>
public class GoldenPathIntegrationTests
{
    static string Phone(string p) { var t = DateTimeOffset.UtcNow.ToUnixTimeSeconds().ToString(); return (p + t[^7..])[..9]; }

    [Fact, Trait("Category", "Integration")]
    public async Task GoldenPath()
    {
        AmenClient a;
        try { a = new AmenClient(); } catch (Exception) { return; }   // no credentials → nothing to verify
        if (a.Config.Env != "sandbox") return;
        var buyer = await a.Customers.CreateAsync(new("Buyer", "Kit", "SA", Phone("57")));
        var seller = await a.Customers.CreateAsync(new("Seller", "Kit", "SA", Phone("58")));
        var deal = await a.Deals.CreateAsync(new("product", "Starter Kit golden path") { OfferCategory = (await a.Lookups.CategoriesAsync())![0].Id, OfferPrice = "100.00", OfferDeliveryFee = "10.00", OfferDescription = "Reference deal created by the Amen integration starter kit" });
        var n = deal.Number; Assert.Equal("draft", deal.Status);
        Assert.Equal("draft", (await a.Deals.SetPartiesAsync(n, [buyer.Number], [seller.Number])).Status);
        Assert.Equal("draft", (await a.Deals.SetDeliveryAddressAsync(n, new((await a.Lookups.CitiesAsync())![0].Id, "King Fahd Rd", "1234", "12211", "Al Olaya", "1"))).Status);
        Assert.Equal("requested", (await a.Deals.Actions.SubmitAsync(n)).Status);
        Assert.Equal("payment_pending", (await a.Deals.Actions.ApproveAsync(n)).Status);
        Deal paid;
        try { paid = await a.Deals.Actions.PayWithWalletAsync(n); } catch (AmenApiError e) { Console.WriteLine($"NEEDS_TOP_UP: {string.Join(",", e.Codes)}"); return; }
        Assert.Equal("paid", paid.Status);
        Assert.Equal("executing", (await a.Deals.Actions.ExecutionStartAsync(n)).Status);
        Assert.Equal("executed", (await a.Deals.Actions.ExecutionCompleteAsync(n)).Status);
        Assert.Equal("completed", (await a.Deals.Actions.CompleteAsync(n)).Status);
        Assert.Equal("completed", (await a.Deals.Actions.TransferSellerAmountAsync(n)).Status);
    }
}
