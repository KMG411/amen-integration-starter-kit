/** AmenClient — the one place that knows about auth headers, base URL, timeouts and retries. */
import { API_PREFIX, loadConfig } from "./config.js";
import { AmenApiError } from "./errors.js";
import { Lookups } from "./resources/lookups.js";
import { AccountResource } from "./resources/account.js";
import { Customers } from "./resources/customers.js";
import { Deals } from "./resources/deals.js";
import { Withdrawals } from "./resources/withdrawals.js";
import { Webhooks } from "./resources/webhooks.js";

export class AmenClient {
  constructor(config, fetchImpl = fetch) {
    this.config = loadConfig(config);
    this.fetchImpl = fetchImpl;
    this.lookups = new Lookups(this); this.account = new AccountResource(this); this.customers = new Customers(this);
    this.deals = new Deals(this); this.withdrawals = new Withdrawals(this); this.webhooks = new Webhooks(this);
  }

  /** @param {"GET"|"POST"|"PUT"|"DELETE"} method @param {string} path @param {{json?:unknown, params?:object, form?:FormData}} [opts] */
  async request(method, path, opts = {}) {
    const url = new URL(API_PREFIX + path, this.config.baseUrl);
    for (const [k, v] of Object.entries(opts.params ?? {})) if (v !== undefined) url.searchParams.set(k, String(v));
    const headers = { "X-API-Token": this.config.apiKey, Accept: "application/json", "User-Agent": "amen-starter-kit-js/0.1" };
    if (method !== "GET") headers.Origin = headers.Referer = this.config.baseUrl;   // origin checks on mutating calls
    let body;
    if (opts.form) body = opts.form;
    else if (opts.json !== undefined) { body = JSON.stringify(opts.json); headers["Content-Type"] = "application/json"; }

    for (let attempt = 1; ; attempt++) {
      let res;
      try { res = await this.fetchImpl(url, { method, headers, body, signal: AbortSignal.timeout(this.config.timeoutMs) }); }
      catch (err) { if (attempt > this.config.maxRetries) throw err; await sleep(backoff(attempt)); continue; }
      if (res.ok) return res.status === 204 || res.headers.get("content-length") === "0" ? undefined : res.json();
      const err = await toError(res, method, url.pathname);
      if (err.retryable && attempt <= this.config.maxRetries) { await sleep(backoff(attempt, res.headers.get("retry-after"))); continue; }
      throw err;
    }
  }
}
async function toError(res, method, path) {
  let body; try { body = await res.json(); } catch { body = await res.text().catch(() => ""); }
  const raw = body?.error; const codes = Array.isArray(raw) ? raw.map(String) : typeof raw === "string" ? [raw] : [];
  return new AmenApiError(res.status, codes, method, path, body);
}
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const backoff = (attempt, retryAfter) => retryAfter && /^\d+$/.test(retryAfter) ? Number(retryAfter) * 1000 : Math.min(2 ** attempt, 20) * 1000 + Math.random() * 1000;
