/** Environment-based configuration. Walks up from cwd to find a .env (stack or kit root). */
import { config as dotenv } from "dotenv";
import { existsSync } from "node:fs";
import { resolve } from "node:path";

export const BASE_URLS = { sandbox: "https://sandbox-api.amnn.sa", live: "https://api.amnn.sa" };
export const API_PREFIX = "/api/v1";

export function loadConfig(overrides = {}) {
  let dir = resolve(".");
  for (let i = 0; i < 4; i++) { const p = resolve(dir, ".env"); if (existsSync(p)) { dotenv({ path: p, override: false }); break; } dir = resolve(dir, ".."); }
  const env = (process.env.AMN_ENV ?? "sandbox").toLowerCase();
  if (!BASE_URLS[env]) throw new Error(`AMN_ENV must be 'sandbox' or 'live', got '${env}'`);
  const apiKey = overrides.apiKey ?? process.env.AMN_API_KEY;
  if (!apiKey) throw new Error("AMN_API_KEY is not set (see .env.example)");
  return { env, apiKey, baseUrl: process.env.AMN_BASE_URL ?? BASE_URLS[env], timeoutMs: Number(process.env.AMN_TIMEOUT_MS ?? 20000),
           webhookSecret: process.env.AMN_WEBHOOK_SECRET || undefined, maxRetries: 3, ...overrides };
}
