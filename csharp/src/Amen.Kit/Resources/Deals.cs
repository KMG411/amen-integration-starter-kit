namespace Amen.Kit.Resources;

public sealed class Deals
{
    readonly AmenClient _c;
    public DealActions Actions { get; }
    public Deals(AmenClient c) { _c = c; Actions = new DealActions(c, this); }

    public async Task<Deal> CreateAsync(CreateDeal body) => (await _c.PostAsync<Deal>("/deals/", body))!;
    public async Task<Deal> GetAsync(string n) => (await _c.GetAsync<Deal>($"/deals/{n}"))!;
    public async Task<Deal> UpdateAsync(string n, CreateDeal body) => (await _c.PutAsync<Deal>($"/deals/{n}", body))!;
    public Task DeleteAsync(string n) => _c.DeleteAsync($"/deals/{n}");
    public Task<Page<Deal>> ListAsync(IDictionary<string, string?>? query = null) => _c.PageAsync<Deal>("/deals/", query, "deals");
    public async Task<Deal> SetPartiesAsync(string n, IEnumerable<string> buyers, IEnumerable<string> sellers) => (await _c.PostAsync<Deal>($"/deals/{n}/parties/", new { buyers, sellers }))!;
    public async Task<Deal> SetDeliveryAddressAsync(string n, Address a) => (await _c.PostAsync<Deal>($"/deals/{n}/delivery-address", a))!;
    public async Task<Deal> SetBillingAddressAsync(string n, Address a) => (await _c.PostAsync<Deal>($"/deals/{n}/billing-address", a))!;
    public async Task<List<string>> AllowedPaymentMethodsAsync(string n) =>
        (await _c.GetAsync<Dictionary<string, List<string>>>($"/deals/{n}/allowed-payment-methods/"))?.GetValueOrDefault("payment_methods") ?? [];
}

/// <summary>POST /deals/{n}/action/* — every method returns the updated Deal (or a Checkout for online payment).</summary>
public sealed class DealActions(AmenClient c, Deals deals)
{
    /// <summary>Which statuses each action may be called from (docs/02-deal-lifecycle.md).</summary>
    public static readonly IReadOnlyDictionary<string, string[]> AllowedFrom = new Dictionary<string, string[]>
    {
        ["submit"] = ["draft"], ["approve"] = ["requested"],
        ["make-payment-wallet"] = ["payment_pending"], ["make-payment-online"] = ["payment_pending"],
        ["execution-start"] = ["paid"], ["execution-complete"] = ["executing"], ["complete"] = ["executed"],
        ["transfer-seller-amount"] = ["completed"], ["dispute"] = ["completed"],
        ["dispute-approve"] = ["disputed"], ["dispute-decline"] = ["disputed"],
        ["cancel"] = ["draft", "requested", "payment_pending", "paid", "executing"],
    };

    async Task<T> ActAsync<T>(string n, string action, object? json = null, MultipartFormDataContent? form = null, bool check = true)
    {
        if (check)
        {
            var status = (await deals.GetAsync(n)).Status;
            if (!AllowedFrom[action].Contains(status)) throw new AmenLifecycleError($"action '{action}' is not allowed from status '{status}' (allowed: {string.Join(", ", AllowedFrom[action])})");
        }
        var path = $"/deals/{n}/action/{action}";
        return (form is not null ? await c.PostFormAsync<T>(path, form) : await c.PostAsync<T>(path, json ?? new { }))!;
    }
    static MultipartFormDataContent Form(int reason, string comment) => new() { { new StringContent(reason.ToString()), "reason" }, { new StringContent(comment), "comment" } };

    public Task<Deal> SubmitAsync(string n, bool check = true) => ActAsync<Deal>(n, "submit", check: check);
    public Task<Deal> ApproveAsync(string n, string? price = null, bool check = true) => ActAsync<Deal>(n, "approve", price is null ? new { } : new { price }, check: check);
    public Task<Deal> PayWithWalletAsync(string n, bool check = true) => ActAsync<Deal>(n, "make-payment-wallet", check: check);
    public Task<Checkout> PayOnlineAsync(string n, string paymentMethod = "mada", bool check = true) => ActAsync<Checkout>(n, "make-payment-online", new { payment_method = paymentMethod }, check: check);
    public Task<Deal> ExecutionStartAsync(string n, bool check = true) => ActAsync<Deal>(n, "execution-start", check: check);
    public Task<Deal> ExecutionCompleteAsync(string n, bool check = true) => ActAsync<Deal>(n, "execution-complete", check: check);
    public Task<Deal> CompleteAsync(string n, bool check = true) => ActAsync<Deal>(n, "complete", check: check);
    public Task<Deal> TransferSellerAmountAsync(string n, bool check = true) => ActAsync<Deal>(n, "transfer-seller-amount", check: check);
    public Task<Deal> CancelAsync(string n, string dealParty, int reason, string comment, bool check = true) => ActAsync<Deal>(n, "cancel", new { deal_party = dealParty, reason, comment }, check: check);
    public Task<Deal> DisputeAsync(string n, int reason, string comment, IEnumerable<(byte[] Data, string FileName)>? attachments = null, bool check = true)
    {
        var form = Form(reason, comment); var i = 0;
        foreach (var a in attachments ?? []) form.Add(new ByteArrayContent(a.Data), $"attachment_{++i}", a.FileName);
        return ActAsync<Deal>(n, "dispute", form: form, check: check);
    }
    public Task<Deal> DisputeApproveAsync(string n, int reason, string comment, bool check = true) => ActAsync<Deal>(n, "dispute-approve", form: Form(reason, comment), check: check);
    public Task<Deal> DisputeDeclineAsync(string n, int reason, string comment, bool check = true) => ActAsync<Deal>(n, "dispute-decline", form: Form(reason, comment), check: check);
}
