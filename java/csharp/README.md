# Amen API — C# / .NET reference implementation

.NET 8, `HttpClient` (works with `IHttpClientFactory`), `System.Text.Json`, no third-party runtime dependencies. xUnit tests.

```sh
dotnet test                                          # offline unit tests
AMN_API_KEY=… dotnet run --project examples/GoldenPath   # or put the key in ../.env
AMN_API_KEY=… dotnet test --filter Category=Integration   # sandbox; skipped when unset
```

```csharp
var amen = new AmenClient();                          // config from env
var deal = await amen.Deals.CreateAsync(new CreateDeal("product", "iPhone 15") { OfferPrice = "3500.00", OfferDeliveryFee = "25.00", OfferCategory = 12 });
await amen.Deals.SetPartiesAsync(deal.Number, buyers: [buyer.Number], sellers: [seller.Number]);
try { await amen.Deals.Actions.SubmitAsync(deal.Number); }
catch (AmenApiError e) when (e.Has("deal__delivery_address_required")) { /* … */ }
```

**ASP.NET Core:** `services.AddSingleton(new AmenClient(Config.FromEnvironment()))` (or `AddHttpClient` + `new AmenClient(config, httpClient)`). The webhook endpoint must read the raw body (`Request.Body` → `byte[]`) and hand it to `WebhookHandler` before any model binding.
