/** Golden path (scenario/golden-path.yml): product deal from creation to seller payout.
 *    npm run example:golden-path            # full run
 *    npx tsx examples/01_golden_path.ts DL-000123   # resume a deal that is already 'paid'
 */
import { AmenClient, AmenApiError } from "../src/amen/index.js";
import { step, uniquePhone } from "./_common.js";

const amen = new AmenClient();
console.log(`environment: ${amen.config.env} (${amen.config.baseUrl})\n`);

async function continueFromPaid(n: string) {
  step("execution-start", await amen.deals.actions.executionStart(n));
  step("execution-complete", await amen.deals.actions.executionComplete(n));
  step("complete", await amen.deals.actions.complete(n));
  step("transfer-seller-amount (payout)", await amen.deals.actions.transferSellerAmount(n));
  console.log(`\n🎉 deal ${n} finished: ${(await amen.deals.get(n)).status}`);
}

const resume = process.argv[2];
if (resume) { await continueFromPaid(resume); process.exit(0); }

const buyer = await amen.customers.create({ first_name: "Buyer", last_name: "Kit", phone_code: "SA", phone_number: uniquePhone("57") });
const seller = await amen.customers.create({ first_name: "Seller", last_name: "Kit", phone_code: "SA", phone_number: uniquePhone("58") });
step(`customers ${buyer.number} (buyer), ${seller.number} (seller)`);

const [category] = await amen.lookups.categories();
const [city] = await amen.lookups.cities();
const deal = await amen.deals.create({ offer_type: "product", offer_category: category.id, offer_title: "Starter Kit golden path",
  offer_description: "Reference deal created by the Amen integration starter kit", offer_price: "100.00", offer_delivery_fee: "10.00" });
const n = deal.number;
step(`deal ${n} created`, deal);
step("parties", await amen.deals.setParties(n, { buyers: [buyer.number], sellers: [seller.number] }));
step("delivery address", await amen.deals.setDeliveryAddress(n, { city: city.id, district: "Al Olaya", street: "King Fahd Rd", building_number: "1234", unit_number: "1", zip_code: "12211" }));

step("submit", await amen.deals.actions.submit(n));
step("approve", await amen.deals.actions.approve(n));
console.log("  allowed payment methods:", await amen.deals.allowedPaymentMethods(n));
try {
  step("pay with wallet", await amen.deals.actions.payWithWallet(n));
} catch (e) {
  if (!(e instanceof AmenApiError)) throw e;
  const checkout = await amen.deals.actions.payOnline(n, "mada");
  console.log(`
⏸  NEEDS_TOP_UP — wallet payment not possible (${e.codes.join(", ") || e.status}).
   An online (HyperPay) checkout was created instead: ${JSON.stringify(checkout)}
   Complete that checkout in a UI or top up the sandbox wallet (GET /api/v1/account → wallet.top_up_account), then resume:
       npx tsx examples/01_golden_path.ts ${n}`);
  process.exit(0);
}
await continueFromPaid(n);
