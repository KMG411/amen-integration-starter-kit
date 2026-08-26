import { describe, expect, it, vi } from "vitest";
import { AmenClient, AmenApiError, AmenLifecycleError } from "../../src/amen/index.js";

const json = (status: number, body: unknown) => new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json" } });
const client = (fetchImpl: typeof fetch) => new AmenClient({ apiKey: "test-token", baseUrl: "https://sandbox-api.amnn.sa", maxRetries: 1 }, fetchImpl);

describe("AmenClient", () => {
  it("sends the auth header to the sandbox base URL", async () => {
    const f = vi.fn(async (url: any, init: any) => { expect(String(url)).toBe("https://sandbox-api.amnn.sa/api/v1/account"); expect(init.headers["X-API-Token"]).toBe("test-token"); return json(200, { id: "a1" }); });
    expect(await client(f as any).account.get()).toEqual({ id: "a1" });
  });
  it("parses error codes into AmenApiError", async () => {
    const f = vi.fn(async () => json(400, { error: ["first_name__required"] }));
    await expect(client(f as any).customers.create({ first_name: "", last_name: "x", phone_code: "SA", phone_number: "5" })).rejects.toSatisfy((e: any) => e instanceof AmenApiError && e.has("first_name__required") && !e.retryable);
  });
  it("retries 429 then succeeds", async () => {
    const f = vi.fn().mockResolvedValueOnce(json(429, { error: ["rate_limit__exceeded"] })).mockResolvedValueOnce(json(200, [{ id: 1 }]));
    vi.spyOn(globalThis, "setTimeout").mockImplementation(((cb: any) => { cb(); return 0 as any; }) as any);
    expect(await client(f as any).lookups.cities()).toEqual([{ id: 1 }]);
    expect(f).toHaveBeenCalledTimes(2);
  });
  it("blocks lifecycle-invalid actions locally", async () => {
    const f = vi.fn(async () => json(200, { number: "DL-1", status: "draft" }));
    await expect(client(f as any).deals.actions.approve("DL-1")).rejects.toBeInstanceOf(AmenLifecycleError);
    expect(f).toHaveBeenCalledTimes(1);   // only the GET, no POST
  });
  it("sends CSRF token (header + cookie) and Origin on mutating requests", async () => {
    const f = vi.fn(async (_u: any, init: any) => {
      expect(init.headers.Origin).toBe("https://sandbox-api.amnn.sa");
      expect(init.headers["X-CSRFToken"]).toBeTruthy();
      expect(init.headers.Cookie).toBe(`csrftoken=${init.headers["X-CSRFToken"]}`);
      return json(201, { id: "w", url: "u", secret_key: "s" });
    });
    expect((await client(f as any).webhooks.create("https://example.com/hook")).secret_key).toBe("s");
  });
});
