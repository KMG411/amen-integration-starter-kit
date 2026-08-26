package sa.amnn.kit.resources;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import java.util.Map;
import sa.amnn.kit.AmenClient;
import sa.amnn.kit.AmenClient.Part;
import sa.amnn.kit.Models.Account;
import sa.amnn.kit.Models.BankAccount;

public final class AccountResource {
    private final AmenClient c; public AccountResource(AmenClient c) { this.c = c; }
    public Account get() { return c.get("/account", Map.of(), new TypeReference<>() {}); }
    public List<BankAccount> bankAccounts() { return c.get("/account/bank-accounts/", Map.of(), new TypeReference<>() {}); }
    public BankAccount linkBankAccount(String iban, Part proofDocument) {
        var parts = new java.util.ArrayList<>(List.of(Part.text("iban", iban))); if (proofDocument != null) parts.add(proofDocument);
        return c.postForm("/account/bank-accounts/", parts, new TypeReference<>() {});
    }
    public void deleteBankAccount(String id) { c.delete("/account/bank-accounts/" + id); }
}
