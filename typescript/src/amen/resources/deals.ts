import type { AmenClient } from "../client.js";
import { AmenLifecycleError } from "../errors.js";
import type { Address, Checkout, CreateDeal, Deal, DealParty, DealStatus, Page, PaymentMethod } from "../types.js";
import { toPage } from "./page.js";

/** Which statuses each action may be called from (docs/02-deal-lifecycle.md). */
export const ALLOWED_FROM: Record<string, DealStatus[]> = {
  submit: ["draft"], approve: ["requested"],
  "make-payment-wallet": ["payment_pending"], "make-payment-online": ["payment_pending"],
  "execution-start": ["paid"], "execution-complete": ["executing"], complete: ["executed"],
  "transfer-seller-amount": ["completed"], dispute: ["completed"],
  "dispute-approve": ["disputed"], "dispute-decline": ["disputed"],
  cancel: ["draft", "requested", "payment_pending", "paid", "executing"],
};

export class DealActions {
  constructor(private c: AmenClient, private deals: Deals) {}

  private async act<T = Deal>(n: string, action: string, o: { json?: unknown; form?: FormData; check?: boolean } = {}): Promise<T> {
    if (o.check !== false) {
      const { status } = await this.deals.get(n);
      if (!ALLOWED_FROM[action].includes(status))
        throw new AmenLifecycleError(`action '${action}' is not allowed from status '${status}' (allowed: ${ALLOWED_FROM[action].join(", ")})`);
    }
    return this.c.request<T>("POST", `/deals/${n}/action/${action}`, { json: o.json, form: o.form });
  }
  submit(n: string, check?: boolean) { return this.act(n, "submit", { check }); }
  approve(n: string, price?: string, check?: boolean) { return this.act(n, "approve", { json: price ? { price } : {}, check }); }
  payWithWallet(n: string, check?: boolean) { return this.act(n, "make-payment-wallet", { check }); }
  payOnline(n: string, payment_method: PaymentMethod = "mada", check?: boolean) { return this.act<Checkout>(n, "make-payment-online", { json: { payment_method }, check }); }
  executionStart(n: string, check?: boolean) { return this.act(n, "execution-start", { check }); }
  executionComplete(n: string, check?: boolean) { return this.act(n, "execution-complete", { check }); }
  complete(n: string, check?: boolean) { return this.act(n, "complete", { check }); }
  transferSellerAmount(n: string, check?: boolean) { return this.act(n, "transfer-seller-amount", { check }); }
  cancel(n: string, body: { deal_party: DealParty; reason: number; comment: string }, check?: boolean) { return this.act(n, "cancel", { json: body, check }); }
  dispute(n: string, body: { reason: number; comment: string; attachments?: Blob[] }, check?: boolean) {
    const form = new FormData(); form.set("reason", String(body.reason)); form.set("comment", body.comment);
    body.attachments?.forEach((a, i) => form.set(`attachment_${i + 1}`, a));
    return this.act(n, "dispute", { form, check });
  }
  disputeApprove(n: string, body: { reason: number; comment: string }, check?: boolean) { return this.act(n, "dispute-approve", { form: toForm(body), check }); }
  disputeDecline(n: string, body: { reason: number; comment: string }, check?: boolean) { return this.act(n, "dispute-decline", { form: toForm(body), check }); }
}
const toForm = (o: Record<string, string | number>) => { const f = new FormData(); for (const [k, v] of Object.entries(o)) f.set(k, String(v)); return f; };

export class Deals {
  readonly actions: DealActions;
  constructor(private c: AmenClient) { this.actions = new DealActions(c, this); }
  create(body: CreateDeal) { return this.c.request<Deal>("POST", "/deals/", { json: body }); }
  get(n: string) { return this.c.request<Deal>("GET", `/deals/${n}`); }
  update(n: string, body: Partial<CreateDeal>) { return this.c.request<Deal>("PUT", `/deals/${n}`, { json: body }); }
  delete(n: string) { return this.c.request<void>("DELETE", `/deals/${n}`); }
  async list(params: { page?: number; per_page?: number; status?: DealStatus } = {}): Promise<Page<Deal>> {
    return toPage<Deal>(await this.c.request("GET", "/deals/", { params }), "deals");
  }
  async *iterAll(filters: { status?: DealStatus } = {}): AsyncGenerator<Deal> {
    for (let page = 0; ; page++) { const p = await this.list({ ...filters, page }); yield* p.items; if (page + 1 >= p.pages || !p.items.length) return; }
  }
  setParties(n: string, body: { buyers: string[]; sellers: string[] }) { return this.c.request<Deal>("POST", `/deals/${n}/parties/`, { json: body }); }
  setDeliveryAddress(n: string, body: Address) { return this.c.request<Deal>("POST", `/deals/${n}/delivery-address`, { json: body }); }
  setBillingAddress(n: string, body: Address) { return this.c.request<Deal>("POST", `/deals/${n}/billing-address`, { json: body }); }
  async allowedPaymentMethods(n: string): Promise<PaymentMethod[]> {
    return (await this.c.request<{ payment_methods: PaymentMethod[] }>("GET", `/deals/${n}/allowed-payment-methods/`)).payment_methods ?? [];
  }
}
