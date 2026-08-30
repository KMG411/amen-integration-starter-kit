/** Signature verification over `timestamp + "." + rawBody` (docs/04-webhooks.md).
 * Verified against real Amen sandbox deliveries: HMAC-SHA256, hex, `sha256=` prefixed. */
import { createHmac, timingSafeEqual } from "node:crypto";
export const SIGNATURE_HEADER = "x-webhook-signature";
export const TIMESTAMP_HEADER = "x-webhook-timestamp";
export const EVENT_HEADER = "x-webhook-event";
export const ALGORITHM = "sha256";
export const computeSignature = (secret, timestamp, rawBody, algorithm = ALGORITHM) =>
  createHmac(algorithm, secret).update(String(timestamp) + ".").update(rawBody).digest("hex");
export function verifySignature(secret, timestamp, rawBody, received, algorithm = ALGORITHM) {
  if (!received || timestamp == null || timestamp === "") return false;
  const given = (received.startsWith("sha256=") ? received.slice("sha256=".length) : received).trim().toLowerCase();
  const expected = computeSignature(secret, timestamp, rawBody, algorithm);
  return given.length === expected.length && timingSafeEqual(Buffer.from(given), Buffer.from(expected));
}
