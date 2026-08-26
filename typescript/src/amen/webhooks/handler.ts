import { SIGNATURE_HEADER, verifySignature } from "./verify.js";

export interface WebhookEvent { id: string; type?: string; data: Record<string, unknown>; raw: Buffer }

/** Framework-agnostic: call `handle(headers, rawBody)` from Express/Next.js/Fastify.
 *  Verifies the raw body first, de-duplicates by id, then dispatches. Keep `onEvent` fast; queue heavy work. */
export class WebhookHandler {
  constructor(private secret: string, private onEvent: (e: WebhookEvent) => void | Promise<void>, private seen = new Set<string>()) {}

  async handle(headers: Record<string, string | string[] | undefined>, rawBody: Buffer): Promise<{ status: number; body: Record<string, unknown> }> {
    const sigRaw = headers[SIGNATURE_HEADER] ?? headers[SIGNATURE_HEADER.toUpperCase()] ?? headers["X-Signature"];
    const sig = Array.isArray(sigRaw) ? sigRaw[0] : sigRaw;
    if (!verifySignature(this.secret, rawBody, sig)) return { status: 401, body: { error: "invalid signature" } };
    let data: Record<string, unknown>;
    try { data = JSON.parse(rawBody.toString("utf8")); } catch { return { status: 400, body: { error: "invalid json" } }; }
    const id = String(data.id ?? data.event_id ?? "");
    if (id && this.seen.has(id)) return { status: 200, body: { ok: true, duplicate: true } };
    if (id) this.seen.add(id);
    await this.onEvent({ id, type: (data.event ?? data.type) as string | undefined, data, raw: rawBody });
    return { status: 200, body: { ok: true } };
  }
}
