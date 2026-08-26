package sa.amnn.kit.resources;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Map;
import sa.amnn.kit.AmenClient;
import sa.amnn.kit.Models.Page;
import sa.amnn.kit.Models.Withdrawal;

public final class Withdrawals {
    private final AmenClient c; public Withdrawals(AmenClient c) { this.c = c; }
    public Withdrawal create(String bankAccountId, String amount) { return c.post("/withdrawals/", Map.of("bank_account_id", bankAccountId, "amount", amount), new TypeReference<>() {}); }
    public Withdrawal get(String n) { return c.get("/withdrawals/" + n, Map.of(), new TypeReference<>() {}); }
    public Page<Withdrawal> list(Map<String, String> params) { return c.page("/withdrawals/", params, "withdrawals", Withdrawal.class); }
}
