import { SIGNATURE_HEADER, TIMESTAMP_HEADER, EVENT_HEADER, verifySignature } from "./verify.js";

export interface WebhookEvent { id: string; type?: string; data: Record<string, unknown>; raw: Buffer }

/** Case-insensitive lookup of a header value (first value if repeated). */
function header(headers: Record<string, string | string[] | undefined>, name: string): string | undefined {
  const wanted = name.toLowerCase();
  for (const [k, v] of Object.entries(headers)) {
    if (k.toLowerCase() === wanted) return Array.isArray(v) ? v[0] : v;
  }
  return undefined;
}

/** Framework-agnostic: call `handle(headers, rawBody)` from Express/Next.js/Fastify.
 *  Verifies "timestamp.rawBody" first, de-duplicates by the delivery timestamp, then dispatches.
 *  Keep `onEvent` fast; queue heavy work. */
export class WebhookHandler {
  constructor(private secret: string, private onEvent: (e: WebhookEvent) => void | Promise<void>, private seen = new Set<string>()) {}

  async handle(headers: Record<string, string | string[] | undefined>, rawBody: Buffer): Promise<{ status: number; body: Record<string, unknown> }> {
    const sig = header(headers, SIGNATURE_HEADER);
    const timestamp = header(headers, TIMESTAMP_HEADER);
    if (!verifySignature(this.secret, timestamp, rawBody, sig)) return { status: 401, body: { error: "invalid signature" } };
    let data: Record<string, unknown>;
    try { data = JSON.parse(rawBody.toString("utf8")); } catch { return { status: 400, body: { error: "invalid json" } }; }
    // No event id in the body — the top-level timestamp is unique per delivery, so dedupe on it.
    const id = String(data.timestamp ?? timestamp ?? "");
    if (id && this.seen.has(id)) return { status: 200, body: { ok: true, duplicate: true } };
    if (id) this.seen.add(id);
    const type = (data.event ?? data.type ?? header(headers, EVENT_HEADER)) as string | undefined;
    await this.onEvent({ id, type, data, raw: rawBody });
    return { status: 200, body: { ok: true } };
  }
}
