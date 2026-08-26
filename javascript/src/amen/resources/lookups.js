export class Lookups {
  constructor(c) { this.c = c; }
  countryCodes() { return this.c.request("GET", "/allowed-country-codes/"); }
  cities() { return this.c.request("GET", "/cities"); }
  categories() { return this.c.request("GET", "/categories/"); }
  disputeReasons() { return this.c.request("GET", "/dispute-reasons/"); }
  disputeResolutionReasons() { return this.c.request("GET", "/dispute-resolution-reasons/"); }
  cancelReasons(party_type) { return this.c.request("GET", "/cancel-reasons/", { params: { party_type } }); }
}
