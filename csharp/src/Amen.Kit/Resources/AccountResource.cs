namespace Amen.Kit.Resources;
public sealed class AccountResource(AmenClient c)
{
    public Task<Account?> GetAsync() => c.GetAsync<Account>("/account");
    public Task<List<BankAccount>?> BankAccountsAsync() => c.GetAsync<List<BankAccount>>("/account/bank-accounts/");
    public Task<BankAccount?> LinkBankAccountAsync(string iban, (byte[] Data, string FileName)? proofDocument = null)
    {
        var form = new MultipartFormDataContent { { new StringContent(iban), "iban" } };
        if (proofDocument is { } p) form.Add(new ByteArrayContent(p.Data), "proof_document", p.FileName);
        return c.PostFormAsync<BankAccount>("/account/bank-accounts/", form);
    }
    public Task DeleteBankAccountAsync(string id) => c.DeleteAsync($"/account/bank-accounts/{id}");
}
