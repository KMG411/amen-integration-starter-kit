/** Environment-based configuration. Reads ./.env and ../.env (kit root) if present. */
import { config as dotenv } from "dotenv";
import { existsSync } from "node:fs";
import { resolve } from "node:path";

export const BASE_URLS = { sandbox: "https://sandbox-api.amnn.sa", live: "https://api.amnn.sa" } as const;
export const API_PREFIX = "/api/v1";
export type Env = keyof typeof BASE_URLS;

export interface Config {
  env: Env; apiKey: string; baseUrl: string; timeoutMs: number; webhookSecret?: string; maxRetries: number;
}

export function loadConfig(overrides: Partial<Config> = {}): Config {
  // Walk up from cwd so examples/ and tests/ find the stack or kit-root .env
  let dir = resolve(".");
  for (let i = 0; i < 4; i++) { const p = resolve(dir, ".env"); if (existsSync(p)) { dotenv({ path: p, override: false }); break; } dir = resolve(dir, ".."); }
  const env = (process.env.AMN_ENV ?? "sandbox").toLowerCase() as Env;
  if (!(env in BASE_URLS)) throw new Error(`AMN_ENV must be 'sandbox' or 'live', got '${env}'`);
  const apiKey = overrides.apiKey ?? process.env.AMN_API_KEY;
  if (!apiKey) throw new Error("AMN_API_KEY is not set (see .env.example)");
  return {
    env, apiKey, baseUrl: process.env.AMN_BASE_URL ?? BASE_URLS[env],
    timeoutMs: Number(process.env.AMN_TIMEOUT_MS ?? 20000),
    webhookSecret: process.env.AMN_WEBHOOK_SECRET || undefined, maxRetries: 3, ...overrides,
  };
}
