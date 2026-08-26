import { SIGNATURE_HEADER, verifySignature } from "./verify.js";
/** Framework-agnostic: call `handle(headers, rawBody)`. Verifies raw body first, de-duplicates by id, dispatches. */
export class WebhookHandler {
  constructor(secret, onEvent, seen = new Set()) { this.secret = secret; this.onEvent = onEvent; this.seen = seen; }
  async handle(headers, rawBody) {
    const raw = headers[SIGNATURE_HEADER] ?? headers["X-Signature"]; const sig = Array.isArray(raw) ? raw[0] : raw;
    if (!verifySignature(this.secret, rawBody, sig)) return { status: 401, body: { error: "invalid signature" } };
    let data; try { data = JSON.parse(rawBody.toString("utf8")); } catch { return { status: 400, body: { error: "invalid json" } }; }
    const id = String(data.id ?? data.event_id ?? "");
    if (id && this.seen.has(id)) return { status: 200, body: { ok: true, duplicate: true } };
    if (id) this.seen.add(id);
    await this.onEvent({ id, type: data.event ?? data.type, data, raw: rawBody });
    return { status: 200, body: { ok: true } };
  }
}
