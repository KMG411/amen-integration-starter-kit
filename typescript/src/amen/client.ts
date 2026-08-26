/** AmenClient — the one place that knows about auth headers, base URL, timeouts and retries. */
import { randomBytes } from "node:crypto";
import { API_PREFIX, loadConfig, type Config } from "./config.js";
import { AmenApiError } from "./errors.js";
import { Lookups } from "./resources/lookups.js";
import { AccountResource } from "./resources/account.js";
import { Customers } from "./resources/customers.js";
import { Deals } from "./resources/deals.js";
import { Withdrawals } from "./resources/withdrawals.js";
import { Webhooks } from "./resources/webhooks.js";

export type Method = "GET" | "POST" | "PUT" | "DELETE";
export interface RequestOptions { json?: unknown; params?: Record<string, string | number | undefined>; form?: FormData }

export class AmenClient {
  readonly config: Config;
  readonly lookups: Lookups; readonly account: AccountResource; readonly customers: Customers;
  readonly deals: Deals; readonly withdrawals: Withdrawals; readonly webhooks: Webhooks;
  private readonly fetchImpl: typeof fetch;
  private readonly csrf = randomBytes(16).toString("hex");   // 32 hex chars — Django CSRF token format

  constructor(config?: Partial<Config>, fetchImpl: typeof fetch = fetch) {
    this.config = loadConfig(config);
    this.fetchImpl = fetchImpl;
    this.lookups = new Lookups(this); this.account = new AccountResource(this);
    this.customers = new Customers(this); this.deals = new Deals(this);
    this.withdrawals = new Withdrawals(this); this.webhooks = new Webhooks(this);
  }

  async request<T = unknown>(method: Method, path: string, opts: RequestOptions = {}): Promise<T> {
    const url = new URL(API_PREFIX + path, this.config.baseUrl);
    for (const [k, v] of Object.entries(opts.params ?? {})) if (v !== undefined) url.searchParams.set(k, String(v));
    const headers: Record<string, string> = { "X-API-Token": this.config.apiKey, Accept: "application/json", "Accept-Language": "en", "User-Agent": "amen-starter-kit-ts/0.1", Cookie: `csrftoken=${this.csrf}` };
    if (method !== "GET") {  // Django CSRF double-submit: token in both the header and the csrftoken cookie
      headers["X-CSRFToken"] = this.csrf;
      headers.Origin = headers.Referer = this.config.baseUrl;
    }
    let body: BodyInit | undefined;
    if (opts.form) body = opts.form;
    else if (opts.json !== undefined) { body = JSON.stringify(opts.json); headers["Content-Type"] = "application/json"; }

    for (let attempt = 1; ; attempt++) {
      let res: Response;
      try {
        res = await this.fetchImpl(url, { method, headers, body, signal: AbortSignal.timeout(this.config.timeoutMs) });
      } catch (err) {
        if (attempt > this.config.maxRetries) throw err;
        await sleep(backoff(attempt)); continue;
      }
      if (res.ok) return (res.status === 204 || res.headers.get("content-length") === "0") ? (undefined as T) : (await res.json()) as T;
      const err = await toError(res, method, url.pathname);
      if (err.retryable && attempt <= this.config.maxRetries) { await sleep(backoff(attempt, res.headers.get("retry-after"))); continue; }
      throw err;
    }
  }
}

async function toError(res: Response, method: string, path: string): Promise<AmenApiError> {
  let body: unknown; try { body = await res.json(); } catch { body = await res.text().catch(() => ""); }
  const raw = (body as { error?: unknown })?.error;
  const codes = Array.isArray(raw) ? raw.map(String) : typeof raw === "string" ? [raw] : [];
  return new AmenApiError(res.status, codes, method, path, body);
}
const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));
const backoff = (attempt: number, retryAfter?: string | null) =>
  retryAfter && /^\d+$/.test(retryAfter) ? Number(retryAfter) * 1000 : Math.min(2 ** attempt, 20) * 1000 + Math.random() * 1000;
