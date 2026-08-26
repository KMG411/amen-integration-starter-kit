import type { AmenClient } from "../client.js";
import type { CreateCustomer, Customer, Page } from "../types.js";
import { toPage } from "./page.js";
export class Customers {
  constructor(private c: AmenClient) {}
  create(body: CreateCustomer) { return this.c.request<Customer>("POST", "/customers/", { json: body }); }
  get(customerNumber: string) { return this.c.request<Customer>("GET", `/customers/${customerNumber}`); }
  async list(params: { page?: number; per_page?: number; type?: string; status?: string } = {}): Promise<Page<Customer>> {
    return toPage<Customer>(await this.c.request("GET", "/customers/", { params }), "customers");
  }
  /** Iterate every page — never process only the first page by accident. */
  async *iterAll(filters: { type?: string; status?: string } = {}): AsyncGenerator<Customer> {
    for (let page = 0; ; page++) {
      const p = await this.list({ ...filters, page });
      yield* p.items;
      if (page + 1 >= p.pages || p.items.length === 0) return;
    }
  }
}
