export class Webhooks {
  constructor(c) { this.c = c; }
  list() { return this.c.request("GET", "/web-hooks/"); }
  /** `secret_key` in the response is shown ONLY now — store it in a secret manager immediately. */
  create(url) { return this.c.request("POST", "/web-hooks/", { json: { url } }); }
  delete(id) { return this.c.request("DELETE", `/web-hooks/${id}`); }
}
