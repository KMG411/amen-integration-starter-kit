import { describe, expect, it } from "vitest";
import { computeSignature, verifySignature, WebhookHandler } from "../../src/amen/index.js";
const SECRET = "unit-test-secret";
describe("webhooks", () => {
  it("verifies signatures over the raw body (with optional prefix)", () => {
    const body = Buffer.from('{"id":"e1","event":"deal.paid"}');
    const sig = computeSignature(SECRET, body);
    expect(verifySignature(SECRET, body, sig)).toBe(true);
    expect(verifySignature(SECRET, body, `sha256=${sig}`)).toBe(true);
    expect(verifySignature(SECRET, Buffer.concat([body, Buffer.from(" ")]), sig)).toBe(false);
    expect(verifySignature(SECRET, body, undefined)).toBe(false);
  });
  it("rejects bad signatures and de-duplicates", async () => {
    const seen: string[] = [];
    const h = new WebhookHandler(SECRET, (e) => { seen.push(e.id); });
    const body = Buffer.from(JSON.stringify({ id: "e1", event: "deal.paid" }));
    const good = { "x-signature": computeSignature(SECRET, body) };
    expect((await h.handle({ "x-signature": "bad" }, body)).status).toBe(401);
    expect(await h.handle(good, body)).toEqual({ status: 200, body: { ok: true } });
    expect(await h.handle(good, body)).toEqual({ status: 200, body: { ok: true, duplicate: true } });
    expect(seen).toEqual(["e1"]);
  });
});
