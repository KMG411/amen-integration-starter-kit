package sa.amnn.kit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import sa.amnn.kit.Models.*;

/** Mirrors scenario/golden-path.yml. Runs with `mvn -Pintegration test`; skipped without sandbox credentials. */
@Tag("integration")
class GoldenPathIT {
    static String phone(String p) { String t = String.valueOf(System.currentTimeMillis() / 1000); return (p + t.substring(t.length() - 7)).substring(0, 9); }

    @Test void goldenPath() {
        AmenClient a;
        try { a = new AmenClient(); } catch (RuntimeException e) { assumeTrue(false, "AMN_API_KEY not set"); return; }
        assumeTrue(a.config.env().equals("sandbox"), "sandbox only");
        Customer buyer = a.customers().create(new CreateCustomer("Buyer", "Kit", "SA", phone("57")));
        Customer seller = a.customers().create(new CreateCustomer("Seller", "Kit", "SA", phone("58")));
        Deal deal = a.deals().create(new CreateDeal("product", "Starter Kit golden path").category(a.lookups().categories().get(0).id()).price("100.00").deliveryFee("10.00")
            .description("Reference deal created by the Amen integration starter kit"));
        String n = deal.number(); assertEquals("draft", deal.status());
        assertEquals("draft", a.deals().setParties(n, List.of(buyer.number()), List.of(seller.number())).status());
        assertEquals("draft", a.deals().setDeliveryAddress(n, new Address(a.lookups().cities().get(0).id(), "King Fahd Rd", "1234", "12211", "Al Olaya", "1")).status());
        assertEquals("requested", a.deals().actions().submit(n).status());
        assertEquals("payment_pending", a.deals().actions().approve(n, null).status());
        Deal paid;
        try { paid = a.deals().actions().payWithWallet(n); } catch (AmenApiError e) { assumeTrue(false, "NEEDS_TOP_UP: " + e.codes); return; }
        assertEquals("paid", paid.status());
        assertEquals("executing", a.deals().actions().executionStart(n).status());
        assertEquals("executed", a.deals().actions().executionComplete(n).status());
        assertEquals("completed", a.deals().actions().complete(n).status());
        assertEquals("completed", a.deals().actions().transferSellerAmount(n).status());
    }
}
