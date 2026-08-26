import { test } from "node:test";
import assert from "node:assert/strict";
import { AmenClient, AmenApiError, AmenLifecycleError } from "../../src/amen/index.js";
const json = (status, body) => new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json" } });
const client = (f) => new AmenClient({ apiKey: "test-token", baseUrl: "https://sandbox-api.amnn.sa", maxRetries: 1 }, f);

test("auth header + sandbox base URL", async () => {
  const f = async (url, init) => { assert.equal(String(url), "https://sandbox-api.amnn.sa/api/v1/account"); assert.equal(init.headers["X-API-Token"], "test-token"); return json(200, { id: "a1" }); };
  assert.deepEqual(await client(f).account.get(), { id: "a1" });
});
test("error codes parsed into AmenApiError", async () => {
  await assert.rejects(client(async () => json(400, { error: ["first_name__required"] })).customers.create({}), (e) => e instanceof AmenApiError && e.has("first_name__required") && !e.retryable);
});
test("429 retried then succeeds", async () => {
  let calls = 0; const f = async () => (++calls === 1 ? json(429, { error: ["rate_limit__exceeded"] }) : json(200, [{ id: 1 }]));
  const orig = globalThis.setTimeout; globalThis.setTimeout = (cb) => { cb(); return 0; };
  try { assert.deepEqual(await client(f).lookups.cities(), [{ id: 1 }]); assert.equal(calls, 2); } finally { globalThis.setTimeout = orig; }
});
test("lifecycle guard blocks invalid action locally", async () => {
  let calls = 0; const f = async () => { calls++; return json(200, { number: "DL-1", status: "draft" }); };
  await assert.rejects(client(f).deals.actions.approve("DL-1"), AmenLifecycleError); assert.equal(calls, 1);
});
test("Origin sent on mutating requests", async () => {
  const f = async (_u, init) => { assert.equal(init.headers.Origin, "https://sandbox-api.amnn.sa"); return json(201, { id: "w", secret_key: "s" }); };
  assert.equal((await client(f).webhooks.create("https://example.com/hook")).secret_key, "s");
});
