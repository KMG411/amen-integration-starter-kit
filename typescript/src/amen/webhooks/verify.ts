/** Signature verification over the timestamped raw body.
 *  Real Amen scheme (verified against sandbox deliveries 2026-08-30):
 *    signedMessage = timestamp + "." + rawBody
 *    header value  = "sha256=" + HMAC_SHA256(secret, signedMessage) as lowercase hex
 *  See docs/04-webhooks.md. */
import { createHmac, timingSafeEqual } from "node:crypto";
export const SIGNATURE_HEADER = "X-Webhook-Signature";
export const TIMESTAMP_HEADER = "X-Webhook-Timestamp";
export const EVENT_HEADER = "X-Webhook-Event";
export const ALGORITHM = "sha256";

/** hex(HMAC_SHA256(secret, utf8(timestamp) + "." + rawBody)). Raw bytes are signed as received — never re-serialized. */
export function computeSignature(secret: string, timestamp: string, rawBody: Buffer | string, algorithm = ALGORITHM): string {
  const body = typeof rawBody === "string" ? Buffer.from(rawBody, "utf8") : rawBody;
  const message = Buffer.concat([Buffer.from(`${timestamp}.`, "utf8"), body]);
  return createHmac(algorithm, secret).update(message).digest("hex");
}
/** Accepts hex digests, optionally prefixed like "sha256=<hex>". Constant-time comparison. */
export function verifySignature(secret: string, timestamp: string | undefined | null, rawBody: Buffer | string, received?: string | null, algorithm = ALGORITHM): boolean {
  if (!received || !timestamp) return false;
  const given = (received.startsWith("sha256=") ? received.slice("sha256=".length) : received).trim().toLowerCase();
  const expected = computeSignature(secret, timestamp, rawBody, algorithm);
  return given.length === expected.length && timingSafeEqual(Buffer.from(given), Buffer.from(expected));
}
