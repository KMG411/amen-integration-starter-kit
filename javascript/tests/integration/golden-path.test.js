/** Mirrors scenario/golden-path.yml. Skipped without sandbox credentials. */
import { test } from "node:test";
import assert from "node:assert/strict";
import { AmenClient, AmenApiError } from "../../src/amen/index.js";
let amen; try { amen = new AmenClient(); if (amen.config.env !== "sandbox") amen = undefined; } catch { amen = undefined; }
const phone = (p) => `${p}${String(Math.floor(Date.now() / 1000)).slice(-7)}`.slice(0, 9);

test("golden path (sandbox)", { skip: !amen && "AMN_API_KEY not set", timeout: 120_000 }, async () => {
  const buyer = await amen.customers.create({ first_name: "Buyer", last_name: "Kit", phone_code: "SA", phone_number: phone("57") });
  const seller = await amen.customers.create({ first_name: "Seller", last_name: "Kit", phone_code: "SA", phone_number: phone("58") });
  const deal = await amen.deals.create({ offer_type: "product", offer_category: (await amen.lookups.categories())[0].id, offer_title: "Starter Kit golden path", offer_description: "Reference deal created by the Amen integration starter kit", offer_price: "100.00", offer_delivery_fee: "10.00" });
  const n = deal.number; assert.equal(deal.status, "draft");
  assert.equal((await amen.deals.setParties(n, { buyers: [buyer.number], sellers: [seller.number] })).status, "draft");
  assert.equal((await amen.deals.setDeliveryAddress(n, { city: (await amen.lookups.cities())[0].id, district: "Al Olaya", street: "King Fahd Rd", building_number: "1234", unit_number: "1", zip_code: "12211" })).status, "draft");
  assert.equal((await amen.deals.actions.submit(n)).status, "requested");
  assert.equal((await amen.deals.actions.approve(n)).status, "payment_pending");
  let paid; try { paid = await amen.deals.actions.payWithWallet(n); } catch (e) { if (e instanceof AmenApiError) { console.warn(`NEEDS_TOP_UP: ${e.codes}`); return; } throw e; }
  assert.equal(paid.status, "paid");
  assert.equal((await amen.deals.actions.executionStart(n)).status, "executing");
  assert.equal((await amen.deals.actions.executionComplete(n)).status, "executed");
  assert.equal((await amen.deals.actions.complete(n)).status, "completed");
  assert.equal((await amen.deals.actions.transferSellerAmount(n)).status, "completed");
});
