import { toPage } from "../models.js";
export class Customers {
  constructor(c) { this.c = c; }
  create(body) { return this.c.request("POST", "/customers/", { json: body }); }
  get(customerNumber) { return this.c.request("GET", `/customers/${customerNumber}`); }
  async list(params = {}) { return toPage(await this.c.request("GET", "/customers/", { params }), "customers"); }
  /** Iterate every page — never process only the first page by accident. */
  async *iterAll(filters = {}) { for (let page = 0; ; page++) { const p = await this.list({ ...filters, page }); yield* p.items; if (page + 1 >= p.pages || !p.items.length) return; } }
}
