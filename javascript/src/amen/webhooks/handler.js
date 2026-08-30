import { SIGNATURE_HEADER, TIMESTAMP_HEADER, verifySignature } from "./verify.js";
/** Framework-agnostic: call `handle(headers, rawBody)`. Case-insensitive header lookup;
 * verifies over `timestamp + "." + rawBody`, de-duplicates by the top-level event timestamp, dispatches. */
export class WebhookHandler {
  constructor(secret, onEvent, seen = new Set()) { this.secret = secret; this.onEvent = onEvent; this.seen = seen; }
  #header(headers, name) {
    for (const key of Object.keys(headers)) if (key.toLowerCase() === name) { const v = headers[key]; return Array.isArray(v) ? v[0] : v; }
    return undefined;
  }
  async handle(headers, rawBody) {
    const sig = this.#header(headers, SIGNATURE_HEADER);
    const tsHeader = this.#header(headers, TIMESTAMP_HEADER);
    if (!verifySignature(this.secret, tsHeader, rawBody, sig)) return { status: 401, body: { error: "invalid signature" } };
    let data; try { data = JSON.parse(rawBody.toString("utf8")); } catch { return { status: 400, body: { error: "invalid json" } }; }
    const type = data.event ?? data.type;
    const id = String(data.timestamp ?? tsHeader ?? "");
    if (id && this.seen.has(id)) return { status: 200, body: { ok: true, duplicate: true } };
    if (id) this.seen.add(id);
    await this.onEvent({ id, type, data, raw: rawBody });
    return { status: 200, body: { ok: true } };
  }
}
