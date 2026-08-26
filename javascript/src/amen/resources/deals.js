import { AmenLifecycleError } from "../errors.js";
import { toPage } from "../models.js";

/** Which statuses each action may be called from (docs/02-deal-lifecycle.md). */
export const ALLOWED_FROM = {
  submit: ["draft"], approve: ["requested"],
  "make-payment-wallet": ["payment_pending"], "make-payment-online": ["payment_pending"],
  "execution-start": ["paid"], "execution-complete": ["executing"], complete: ["executed"],
  "transfer-seller-amount": ["completed"], dispute: ["completed"],
  "dispute-approve": ["disputed"], "dispute-decline": ["disputed"],
  cancel: ["draft", "requested", "payment_pending", "paid", "executing"],
};
const toForm = (o) => { const f = new FormData(); for (const [k, v] of Object.entries(o)) f.set(k, String(v)); return f; };

export class DealActions {
  constructor(c, deals) { this.c = c; this.deals = deals; }
  async act(n, action, { json, form, check = true } = {}) {
    if (check) {
      const { status } = await this.deals.get(n);
      if (!ALLOWED_FROM[action].includes(status)) throw new AmenLifecycleError(`action '${action}' is not allowed from status '${status}' (allowed: ${ALLOWED_FROM[action].join(", ")})`);
    }
    return this.c.request("POST", `/deals/${n}/action/${action}`, { json, form });
  }
  submit(n, check) { return this.act(n, "submit", { check }); }
  approve(n, price, check) { return this.act(n, "approve", { json: price ? { price } : {}, check }); }
  payWithWallet(n, check) { return this.act(n, "make-payment-wallet", { check }); }
  payOnline(n, payment_method = "mada", check) { return this.act(n, "make-payment-online", { json: { payment_method }, check }); }
  executionStart(n, check) { return this.act(n, "execution-start", { check }); }
  executionComplete(n, check) { return this.act(n, "execution-complete", { check }); }
  complete(n, check) { return this.act(n, "complete", { check }); }
  transferSellerAmount(n, check) { return this.act(n, "transfer-seller-amount", { check }); }
  cancel(n, body, check) { return this.act(n, "cancel", { json: body, check }); }
  dispute(n, { reason, comment, attachments = [] }, check) { const form = toForm({ reason, comment }); attachments.forEach((a, i) => form.set(`attachment_${i + 1}`, a)); return this.act(n, "dispute", { form, check }); }
  disputeApprove(n, body, check) { return this.act(n, "dispute-approve", { form: toForm(body), check }); }
  disputeDecline(n, body, check) { return this.act(n, "dispute-decline", { form: toForm(body), check }); }
}

export class Deals {
  constructor(c) { this.c = c; this.actions = new DealActions(c, this); }
  create(body) { return this.c.request("POST", "/deals/", { json: body }); }
  get(n) { return this.c.request("GET", `/deals/${n}`); }
  update(n, body) { return this.c.request("PUT", `/deals/${n}`, { json: body }); }
  delete(n) { return this.c.request("DELETE", `/deals/${n}`); }
  async list(params = {}) { return toPage(await this.c.request("GET", "/deals/", { params }), "deals"); }
  async *iterAll(filters = {}) { for (let page = 0; ; page++) { const p = await this.list({ ...filters, page }); yield* p.items; if (page + 1 >= p.pages || !p.items.length) return; } }
  setParties(n, body) { return this.c.request("POST", `/deals/${n}/parties/`, { json: body }); }
  setDeliveryAddress(n, body) { return this.c.request("POST", `/deals/${n}/delivery-address`, { json: body }); }
  setBillingAddress(n, body) { return this.c.request("POST", `/deals/${n}/billing-address`, { json: body }); }
  async allowedPaymentMethods(n) { return (await this.c.request("GET", `/deals/${n}/allowed-payment-methods/`)).payment_methods ?? []; }
}
