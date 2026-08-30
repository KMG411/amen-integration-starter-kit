import { describe, expect, it } from "vitest";
import {
  computeSignature,
  verifySignature,
  WebhookHandler,
  SIGNATURE_HEADER,
  TIMESTAMP_HEADER,
  EVENT_HEADER,
} from "../../src/amen/index.js";

const SECRET = "unit-test-secret";

// Golden cross-stack vector (must match the Python impl exactly).
const GOLDEN = {
  secret: "whsec_test",
  timestamp: "2026-08-30T18:53:23.885957+00:00",
  body: '{"event":"deal.status.changed","timestamp":"2026-08-30T18:53:23.885957+00:00","payload":{"number":"D-0000000002","status":"paid"}}',
  hex: "950ca0ff7494dd435d4dc9d7e7ebe31cf54f0859a28a69a686d77e8db9dfd45c",
};

describe("webhooks", () => {
  it("matches the golden HMAC-SHA256 vector (cross-stack agreement)", () => {
    expect(computeSignature(GOLDEN.secret, GOLDEN.timestamp, Buffer.from(GOLDEN.body))).toBe(GOLDEN.hex);
  });

  it("signs timestamp + '.' + rawBody and round-trips with/without the sha256= prefix", () => {
    const ts = "2026-08-30T18:53:23.885957+00:00";
    const body = Buffer.from('{"event":"deal.status.changed","timestamp":"' + ts + '"}');
    const sig = computeSignature(SECRET, ts, body);
    expect(verifySignature(SECRET, ts, body, sig)).toBe(true);
    expect(verifySignature(SECRET, ts, body, `sha256=${sig}`)).toBe(true);
  });

  it("rejects tampered body, tampered timestamp, missing timestamp, and wrong signature", () => {
    const ts = "2026-08-30T18:53:23.885957+00:00";
    const body = Buffer.from('{"event":"deal.status.changed","timestamp":"' + ts + '"}');
    const sig = computeSignature(SECRET, ts, body);
    // tampered body
    expect(verifySignature(SECRET, ts, Buffer.concat([body, Buffer.from(" ")]), sig)).toBe(false);
    // tampered timestamp
    expect(verifySignature(SECRET, "2026-08-30T18:53:23.885958+00:00", body, sig)).toBe(false);
    // missing timestamp
    expect(verifySignature(SECRET, undefined, body, sig)).toBe(false);
    // wrong / missing signature
    expect(verifySignature(SECRET, ts, body, "sha256=deadbeef")).toBe(false);
    expect(verifySignature(SECRET, ts, body, undefined)).toBe(false);
  });

  it("handles 401 / 200 / duplicate with correctly-signed headers", async () => {
    const seen: string[] = [];
    const h = new WebhookHandler(SECRET, (e) => { seen.push(e.id); });
    const ts = "2026-08-30T18:53:23.885957+00:00";
    const body = Buffer.from(JSON.stringify({ event: "deal.status.changed", timestamp: ts, payload: { number: "D-1" } }));
    const good = {
      [SIGNATURE_HEADER]: `sha256=${computeSignature(SECRET, ts, body)}`,
      [TIMESTAMP_HEADER]: ts,
      [EVENT_HEADER]: "deal.status.changed",
    };
    // bad signature -> 401
    expect((await h.handle({ [SIGNATURE_HEADER]: "sha256=bad", [TIMESTAMP_HEADER]: ts }, body)).status).toBe(401);
    // missing timestamp header -> 401
    expect((await h.handle({ [SIGNATURE_HEADER]: `sha256=${computeSignature(SECRET, ts, body)}` }, body)).status).toBe(401);
    // valid -> 200, dedupe on the delivery timestamp -> duplicate
    expect(await h.handle(good, body)).toEqual({ status: 200, body: { ok: true } });
    expect(await h.handle(good, body)).toEqual({ status: 200, body: { ok: true, duplicate: true } });
    expect(seen).toEqual([ts]);
  });

  it("looks up headers case-insensitively", async () => {
    const events: Array<string | undefined> = [];
    const h = new WebhookHandler(SECRET, (e) => { events.push(e.type); });
    const ts = "2026-08-30T18:53:23.885957+00:00";
    const body = Buffer.from(JSON.stringify({ event: "deal.status.changed", timestamp: ts }));
    const headers = {
      "x-webhook-signature": `sha256=${computeSignature(SECRET, ts, body)}`,
      "x-webhook-timestamp": ts,
    };
    expect((await h.handle(headers, body)).status).toBe(200);
    expect(events).toEqual(["deal.status.changed"]);
  });
});
