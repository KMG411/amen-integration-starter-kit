import { toPage } from "../models.js";
export class Withdrawals {
  constructor(c) { this.c = c; }
  create(body) { return this.c.request("POST", "/withdrawals/", { json: body }); }
  get(n) { return this.c.request("GET", `/withdrawals/${n}`); }
  async list(params = {}) { return toPage(await this.c.request("GET", "/withdrawals/", { params }), "withdrawals"); }
}
