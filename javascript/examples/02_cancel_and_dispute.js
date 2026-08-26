/** node examples/02_cancel_and_dispute.js cancel | dispute DL-000123 (deal must be 'completed') */
import { AmenClient } from "../src/amen/index.js";
import { step, uniquePhone } from "./_common.js";
const amen = new AmenClient(); const [mode = "cancel", dealNo] = process.argv.slice(2);
if (mode === "cancel") {
  const buyer = await amen.customers.create({ first_name: "Buyer", last_name: "Kit", phone_code: "SA", phone_number: uniquePhone("57") });
  const seller = await amen.customers.create({ first_name: "Seller", last_name: "Kit", phone_code: "SA", phone_number: uniquePhone("58") });
  const deal = await amen.deals.create({ offer_type: "service", offer_title: "Cancel scenario", offer_price: "50.00", offer_category: (await amen.lookups.categories())[0].id, deal_subject_details: "Kit test" });
  await amen.deals.setParties(deal.number, { buyers: [buyer.number], sellers: [seller.number] });
  const [reason] = await amen.lookups.cancelReasons("buyer");
  step("cancel", await amen.deals.actions.cancel(deal.number, { deal_party: "buyer", reason: reason.id, comment: "Changed my mind" }));
} else {
  const [reason] = await amen.lookups.disputeReasons();
  step("dispute", await amen.deals.actions.dispute(dealNo, { reason: reason.id, comment: "Item not as described" }));
  const [resolution] = await amen.lookups.disputeResolutionReasons();
  step("dispute-approve", await amen.deals.actions.disputeApprove(dealNo, { reason: resolution.id, comment: "Refund the buyer" }));
}
