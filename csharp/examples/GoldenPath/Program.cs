// Golden path (scenario/golden-path.yml). `dotnet run --project examples/GoldenPath [-- DL-000123]` — pass a deal number to resume from 'paid'.
using Amen.Kit;

static string Phone(string p) { var t = DateTimeOffset.UtcNow.ToUnixTimeSeconds().ToString(); return (p + t[^7..])[..9]; }
static void Step(string label, Deal? d = null) => Console.WriteLine($"✔ {label}{(d is null ? "" : $" → status={d.Status}")}");

var amen = new AmenClient();
Console.WriteLine($"environment: {amen.Config.Env} ({amen.Config.BaseUrl})\n");
async Task ContinueFromPaid(string n)
{
    Step("execution-start", await amen.Deals.Actions.ExecutionStartAsync(n));
    Step("execution-complete", await amen.Deals.Actions.ExecutionCompleteAsync(n));
    Step("complete", await amen.Deals.Actions.CompleteAsync(n));
    Step("transfer-seller-amount (payout)", await amen.Deals.Actions.TransferSellerAmountAsync(n));
    Console.WriteLine($"\n🎉 deal {n} finished: {(await amen.Deals.GetAsync(n)).Status}");
}
if (args.Length > 0) { await ContinueFromPaid(args[0]); return; }

var buyer = await amen.Customers.CreateAsync(new("Buyer", "Kit", "SA", Phone("57")));
var seller = await amen.Customers.CreateAsync(new("Seller", "Kit", "SA", Phone("58")));
Step($"customers {buyer.Number} (buyer), {seller.Number} (seller)");
var category = (await amen.Lookups.CategoriesAsync())![0].Id; var city = (await amen.Lookups.CitiesAsync())![0].Id;
var deal = await amen.Deals.CreateAsync(new("product", "Starter Kit golden path") { OfferCategory = category, OfferPrice = "100.00", OfferDeliveryFee = "10.00", OfferDescription = "Reference deal created by the Amen integration starter kit" });
var n = deal.Number; Step($"deal {n} created", deal);
Step("parties", await amen.Deals.SetPartiesAsync(n, [buyer.Number], [seller.Number]));
Step("delivery address", await amen.Deals.SetDeliveryAddressAsync(n, new(city, "King Fahd Rd", "1234", "12211", "Al Olaya", "1")));
Step("submit", await amen.Deals.Actions.SubmitAsync(n));
Step("approve", await amen.Deals.Actions.ApproveAsync(n));
Console.WriteLine($"  allowed payment methods: {string.Join(", ", await amen.Deals.AllowedPaymentMethodsAsync(n))}");
try { Step("pay with wallet", await amen.Deals.Actions.PayWithWalletAsync(n)); }
catch (AmenApiError e)
{
    var checkout = await amen.Deals.Actions.PayOnlineAsync(n, "mada");
    Console.WriteLine($"\n⏸  NEEDS_TOP_UP — wallet payment not possible ({(e.Codes.Count == 0 ? e.Status.ToString() : string.Join(", ", e.Codes))}).\n   HyperPay checkout created: {checkout}\n   Top up the sandbox wallet (GET /api/v1/account → wallet.top_up_account) or complete the checkout, then:\n       dotnet run --project examples/GoldenPath -- {n}");
    return;
}
await ContinueFromPaid(n);
