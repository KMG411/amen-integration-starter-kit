import { test } from "node:test";
import assert from "node:assert/strict";
import { computeSignature, verifySignature, WebhookHandler } from "../../src/amen/index.js";
const SECRET = "unit-test-secret";
test("signature roundtrip with optional prefix", () => {
  const body = Buffer.from('{"id":"e1","event":"deal.paid"}'); const sig = computeSignature(SECRET, body);
  assert.ok(verifySignature(SECRET, body, sig)); assert.ok(verifySignature(SECRET, body, `sha256=${sig}`));
  assert.ok(!verifySignature(SECRET, Buffer.concat([body, Buffer.from(" ")]), sig)); assert.ok(!verifySignature(SECRET, body, undefined));
});
test("handler rejects bad signature and de-duplicates", async () => {
  const seen = []; const h = new WebhookHandler(SECRET, (e) => seen.push(e.id));
  const body = Buffer.from(JSON.stringify({ id: "e1", event: "deal.paid" })); const good = { "x-signature": computeSignature(SECRET, body) };
  assert.equal((await h.handle({ "x-signature": "bad" }, body)).status, 401);
  assert.deepEqual(await h.handle(good, body), { status: 200, body: { ok: true } });
  assert.deepEqual(await h.handle(good, body), { status: 200, body: { ok: true, duplicate: true } });
  assert.deepEqual(seen, ["e1"]);
});
