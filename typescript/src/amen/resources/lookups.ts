import type { AmenClient } from "../client.js";
export type Lookup = { id: number; name: string };
export class Lookups {
  constructor(private c: AmenClient) {}
  countryCodes() { return this.c.request<unknown[]>("GET", "/allowed-country-codes/"); }
  cities() { return this.c.request<Lookup[]>("GET", "/cities"); }
  categories() { return this.c.request<Lookup[]>("GET", "/categories/"); }
  disputeReasons() { return this.c.request<Lookup[]>("GET", "/dispute-reasons/"); }
  disputeResolutionReasons() { return this.c.request<Lookup[]>("GET", "/dispute-resolution-reasons/"); }
  cancelReasons(party_type?: "buyer" | "seller" | "broker") { return this.c.request<Lookup[]>("GET", "/cancel-reasons/", { params: { party_type } }); }
}
