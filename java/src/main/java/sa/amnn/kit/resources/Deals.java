package sa.amnn.kit.resources;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import sa.amnn.kit.AmenClient;
import sa.amnn.kit.AmenClient.Part;
import sa.amnn.kit.AmenLifecycleError;
import sa.amnn.kit.Models.*;

public final class Deals {
    private final AmenClient c; private final DealActions actions;
    public Deals(AmenClient c) { this.c = c; this.actions = new DealActions(c, this); }
    public DealActions actions() { return actions; }

    public Deal create(CreateDeal body) { return c.post("/deals/", body, new TypeReference<>() {}); }
    public Deal get(String n) { return c.get("/deals/" + n, Map.of(), new TypeReference<>() {}); }
    public Deal update(String n, CreateDeal body) { return c.put("/deals/" + n, body, new TypeReference<>() {}); }
    public void delete(String n) { c.delete("/deals/" + n); }
    public Page<Deal> list(Map<String, String> params) { return c.page("/deals/", params, "deals", Deal.class); }
    public Deal setParties(String n, List<String> buyers, List<String> sellers) { return c.post("/deals/" + n + "/parties/", Map.of("buyers", buyers, "sellers", sellers), new TypeReference<>() {}); }
    public Deal setDeliveryAddress(String n, Address a) { return c.post("/deals/" + n + "/delivery-address", a, new TypeReference<>() {}); }
    public Deal setBillingAddress(String n, Address a) { return c.post("/deals/" + n + "/billing-address", a, new TypeReference<>() {}); }
    public List<String> allowedPaymentMethods(String n) {
        Map<String, List<String>> r = c.get("/deals/" + n + "/allowed-payment-methods/", Map.of(), new TypeReference<>() {});
        return r == null ? List.of() : r.getOrDefault("payment_methods", List.of());
    }

    /** POST /deals/{n}/action/* — every method returns the updated Deal (or a Checkout for online payment). */
    public static final class DealActions {
        /** Which statuses each action may be called from (docs/02-deal-lifecycle.md). */
        public static final Map<String, Set<String>> ALLOWED_FROM = Map.ofEntries(
            Map.entry("submit", Set.of("draft")), Map.entry("approve", Set.of("requested")),
            Map.entry("make-payment-wallet", Set.of("payment_pending")), Map.entry("make-payment-online", Set.of("payment_pending")),
            Map.entry("execution-start", Set.of("paid")), Map.entry("execution-complete", Set.of("executing")), Map.entry("complete", Set.of("executed")),
            Map.entry("transfer-seller-amount", Set.of("completed")), Map.entry("dispute", Set.of("completed")),
            Map.entry("dispute-approve", Set.of("disputed")), Map.entry("dispute-decline", Set.of("disputed")),
            Map.entry("cancel", Set.of("draft", "requested", "payment_pending", "paid", "executing")));
        private final AmenClient c; private final Deals deals;
        DealActions(AmenClient c, Deals deals) { this.c = c; this.deals = deals; }

        private <T> T act(String n, String action, Object json, List<Part> form, boolean check, TypeReference<T> type) {
            if (check) {
                String status = deals.get(n).status();
                if (!ALLOWED_FROM.get(action).contains(status)) throw new AmenLifecycleError("action '" + action + "' is not allowed from status '" + status + "' (allowed: " + ALLOWED_FROM.get(action) + ")");
            }
            return form != null ? c.postForm("/deals/" + n + "/action/" + action, form, type) : c.post("/deals/" + n + "/action/" + action, json == null ? Map.of() : json, type);
        }
        private static final TypeReference<Deal> DEAL = new TypeReference<>() {};
        public Deal submit(String n) { return act(n, "submit", null, null, true, DEAL); }
        public Deal approve(String n, String price) { return act(n, "approve", price == null ? Map.of() : Map.of("price", price), null, true, DEAL); }
        public Deal payWithWallet(String n) { return act(n, "make-payment-wallet", null, null, true, DEAL); }
        public Checkout payOnline(String n, String paymentMethod) { return act(n, "make-payment-online", Map.of("payment_method", paymentMethod), null, true, new TypeReference<>() {}); }
        public Deal executionStart(String n) { return act(n, "execution-start", null, null, true, DEAL); }
        public Deal executionComplete(String n) { return act(n, "execution-complete", null, null, true, DEAL); }
        public Deal complete(String n) { return act(n, "complete", null, null, true, DEAL); }
        public Deal transferSellerAmount(String n) { return act(n, "transfer-seller-amount", null, null, true, DEAL); }
        public Deal cancel(String n, String dealParty, int reason, String comment) { return act(n, "cancel", Map.of("deal_party", dealParty, "reason", reason, "comment", comment), null, true, DEAL); }
        public Deal dispute(String n, int reason, String comment, List<Part> attachments) {
            List<Part> form = new ArrayList<>(List.of(Part.text("reason", String.valueOf(reason)), Part.text("comment", comment)));
            for (int i = 0; i < attachments.size(); i++) { Part a = attachments.get(i); form.add(Part.file("attachment_" + (i + 1), a.filename(), a.file(), a.mimeType())); }
            return act(n, "dispute", null, form, true, DEAL);
        }
        public Deal disputeApprove(String n, int reason, String comment) { return act(n, "dispute-approve", null, List.of(Part.text("reason", String.valueOf(reason)), Part.text("comment", comment)), true, DEAL); }
        public Deal disputeDecline(String n, int reason, String comment) { return act(n, "dispute-decline", null, List.of(Part.text("reason", String.valueOf(reason)), Part.text("comment", comment)), true, DEAL); }
    }
}
