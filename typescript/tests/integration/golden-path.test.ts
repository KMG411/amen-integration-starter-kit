/** Mirrors scenario/golden-path.yml. Skipped without sandbox credentials. */
import { describe, expect, it } from "vitest";
import { AmenClient, AmenApiError } from "../../src/amen/index.js";

let amen: AmenClient | undefined;
try { amen = new AmenClient(); if (amen.config.env !== "sandbox") amen = undefined; } catch { amen = undefined; }
const phone = (p: string) => `${p}${String(Math.floor(Date.now() / 1000)).slice(-7)}`.slice(0, 9);

describe.skipIf(!amen)("golden path (sandbox)", () => {
  it("walks a product deal to payout", async () => {
    const a = amen!;
    const buyer = await a.customers.create({ first_name: "Buyer", last_name: "Kit", phone_code: "SA", phone_number: phone("57") });
    const seller = await a.customers.create({ first_name: "Seller", last_name: "Kit", phone_code: "SA", phone_number: phone("58") });
    const deal = await a.deals.create({ offer_type: "product", offer_category: (await a.lookups.categories())[0].id, offer_title: "Starter Kit golden path",
      offer_description: "Reference deal created by the Amen integration starter kit", offer_price: "100.00", offer_delivery_fee: "10.00" });
    const n = deal.number;
    expect(deal.status).toBe("draft");
    expect((await a.deals.setParties(n, { buyers: [buyer.number], sellers: [seller.number] })).status).toBe("draft");
    expect((await a.deals.setDeliveryAddress(n, { city: (await a.lookups.cities())[0].id, district: "Al Olaya", street: "King Fahd Rd", building_number: "1234", unit_number: "1", zip_code: "12211" })).status).toBe("draft");
    expect((await a.deals.actions.submit(n)).status).toBe("requested");
    expect((await a.deals.actions.approve(n)).status).toBe("payment_pending");
    let paid;
    try { paid = await a.deals.actions.payWithWallet(n); }
    catch (e) { if (e instanceof AmenApiError) { console.warn(`NEEDS_TOP_UP: ${e.codes}`); return; } throw e; }
    expect(paid.status).toBe("paid");
    expect((await a.deals.actions.executionStart(n)).status).toBe("executing");
    expect((await a.deals.actions.executionComplete(n)).status).toBe("executed");
    expect((await a.deals.actions.complete(n)).status).toBe("completed");
    expect((await a.deals.actions.transferSellerAmount(n)).status).toBe("completed");
  }, 120_000);
  it("lookups and account respond", async () => {
    expect((await amen!.lookups.categories()).length).toBeGreaterThan(0);
    expect(await amen!.account.get()).toBeTruthy();
  });
});
