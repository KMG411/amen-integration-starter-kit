package sa.amnn.kit.examples;

import java.util.List;
import sa.amnn.kit.AmenApiError;
import sa.amnn.kit.AmenClient;
import sa.amnn.kit.Models.*;

/** Golden path (scenario/golden-path.yml). `mvn compile exec:java [-Dexec.args=DL-000123]` — pass a deal number to resume from 'paid'. */
public final class GoldenPath {
    static String phone(String p) { String t = String.valueOf(System.currentTimeMillis() / 1000); return (p + t.substring(t.length() - 7)).substring(0, 9); }
    static void step(String label, Deal d) { System.out.println("✔ " + label + (d == null ? "" : " → status=" + d.status())); }

    public static void main(String[] args) {
        AmenClient amen = new AmenClient();
        System.out.printf("environment: %s (%s)%n%n", amen.config.env(), amen.config.baseUrl());
        if (args.length > 0) { continueFromPaid(amen, args[0]); return; }

        Customer buyer = amen.customers().create(new CreateCustomer("Buyer", "Kit", "SA", phone("57")));
        Customer seller = amen.customers().create(new CreateCustomer("Seller", "Kit", "SA", phone("58")));
        step("customers " + buyer.number() + " (buyer), " + seller.number() + " (seller)", null);
        int category = amen.lookups().categories().get(0).id(), city = amen.lookups().cities().get(0).id();
        Deal deal = amen.deals().create(new CreateDeal("product", "Starter Kit golden path").category(category).price("100.00").deliveryFee("0.00")
            .description("Reference deal created by the Amen integration starter kit"));
        String n = deal.number(); step("deal " + n + " created", deal);
        step("parties", amen.deals().setParties(n, List.of(buyer.number()), List.of(seller.number())));
        step("delivery address", amen.deals().setDeliveryAddress(n, new Address(city, "King Fahd Rd", "1234", "12211", "Al Olaya", "1")));
        step("submit", amen.deals().actions().submit(n));
        step("approve", amen.deals().actions().approve(n, null));
        System.out.println("  allowed payment methods: " + amen.deals().allowedPaymentMethods(n));
        try { step("pay with wallet", amen.deals().actions().payWithWallet(n)); }
        catch (AmenApiError e) {
            Checkout checkout = amen.deals().actions().payOnline(n, "mada");
            System.out.printf("%n⏸  NEEDS_TOP_UP — wallet payment not possible (%s).%n   HyperPay checkout created: %s%n   Top up the sandbox wallet (GET /api/v1/account → wallet.top_up_account) or complete the checkout, then:%n       mvn compile exec:java -Dexec.args=%s%n",
                e.codes.isEmpty() ? e.status : String.join(", ", e.codes), checkout, n);
            return;
        }
        continueFromPaid(amen, n);
    }

    static void continueFromPaid(AmenClient amen, String n) {
        step("execution-start", amen.deals().actions().executionStart(n));
        step("execution-complete", amen.deals().actions().executionComplete(n));
        step("complete", amen.deals().actions().complete(n));
        step("transfer-seller-amount (payout)", amen.deals().actions().transferSellerAmount(n));
        System.out.println("\n🎉 deal " + n + " finished: " + amen.deals().get(n).status());
    }
}
