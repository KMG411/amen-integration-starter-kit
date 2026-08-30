import { test } from "node:test";
import assert from "node:assert/strict";
import { computeSignature, verifySignature, WebhookHandler } from "../../src/amen/index.js";

const SECRET = "unit-test-secret";

// Golden vector from a real Amen sandbox delivery (verified 2026-08-30).
const GOLDEN_SECRET = "whsec_test";
const GOLDEN_TS = "2026-08-30T18:53:23.885957+00:00";
const GOLDEN_BODY = '{"event":"deal.status.changed","timestamp":"2026-08-30T18:53:23.885957+00:00","payload":{"number":"D-0000000002","status":"paid"}}';
const GOLDEN_HEX = "950ca0ff7494dd435d4dc9d7e7ebe31cf54f0859a28a69a686d77e8db9dfd45c";

test("computeSignature matches the golden HMAC hex", () => {
  assert.equal(computeSignature(GOLDEN_SECRET, GOLDEN_TS, Buffer.from(GOLDEN_BODY)), GOLDEN_HEX);
  assert.ok(verifySignature(GOLDEN_SECRET, GOLDEN_TS, Buffer.from(GOLDEN_BODY), `sha256=${GOLDEN_HEX}`));
});

test("signature roundtrip, prefix, tamper, and missing timestamp", () => {
  const ts = "2026-08-30T00:00:00+00:00";
  const body = Buffer.from('{"event":"deal.status.changed","timestamp":"2026-08-30T00:00:00+00:00"}');
  const sig = computeSignature(SECRET, ts, body);
  // Accepts bare hex and the optional `sha256=` prefix.
  assert.ok(verifySignature(SECRET, ts, body, sig));
  assert.ok(verifySignature(SECRET, ts, body, `sha256=${sig}`));
  // Tampered body fails.
  assert.ok(!verifySignature(SECRET, ts, Buffer.concat([body, Buffer.from(" ")]), sig));
  // Wrong timestamp fails (timestamp is part of the signed message).
  assert.ok(!verifySignature(SECRET, "2026-01-01T00:00:00+00:00", body, sig));
  // Missing signature / missing timestamp fail closed.
  assert.ok(!verifySignature(SECRET, ts, body, undefined));
  assert.ok(!verifySignature(SECRET, undefined, body, sig));
  assert.ok(!verifySignature(SECRET, "", body, sig));
});

test("handler: 401 bad signature, 200 dispatch, duplicate by event timestamp", async () => {
  const seen = [];
  const h = new WebhookHandler(SECRET, (e) => seen.push({ id: e.id, type: e.type }));
  const ts = "2026-08-30T18:53:23.885957+00:00";
  const body = Buffer.from(JSON.stringify({ event: "deal.status.changed", timestamp: ts, payload: { number: "D-1", status: "paid" } }));
  const good = { "X-Webhook-Signature": `sha256=${computeSignature(SECRET, ts, body)}`, "X-Webhook-Timestamp": ts, "X-Webhook-Event": "deal.status.changed" };

  assert.equal((await h.handle({ "x-webhook-signature": "sha256=bad", "x-webhook-timestamp": ts }, body)).status, 401);
  assert.deepEqual(await h.handle(good, body), { status: 200, body: { ok: true } });
  assert.deepEqual(await h.handle(good, body), { status: 200, body: { ok: true, duplicate: true } });
  assert.deepEqual(seen, [{ id: ts, type: "deal.status.changed" }]);
});
