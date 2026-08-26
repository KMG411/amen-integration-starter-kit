import type { AmenClient } from "../client.js";
import type { Webhook } from "../types.js";
export class Webhooks {
  constructor(private c: AmenClient) {}
  list() { return this.c.request<Webhook[]>("GET", "/web-hooks/"); }
  /** `secret_key` in the response is shown ONLY now — store it in a secret manager immediately. */
  create(url: string) { return this.c.request<Webhook>("POST", "/web-hooks/", { json: { url } }); }
  delete(id: string) { return this.c.request<void>("DELETE", `/web-hooks/${id}`); }
}
