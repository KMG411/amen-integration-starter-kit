import type { AmenClient } from "../client.js";
import type { Page, Withdrawal } from "../types.js";
import { toPage } from "./page.js";
export class Withdrawals {
  constructor(private c: AmenClient) {}
  create(body: { bank_account_id: string; amount: string }) { return this.c.request<Withdrawal>("POST", "/withdrawals/", { json: body }); }
  get(n: string) { return this.c.request<Withdrawal>("GET", `/withdrawals/${n}`); }
  async list(params: { page?: number; page_size?: number; status?: string } = {}): Promise<Page<Withdrawal>> {
    return toPage<Withdrawal>(await this.c.request("GET", "/withdrawals/", { params }), "withdrawals");
  }
}
