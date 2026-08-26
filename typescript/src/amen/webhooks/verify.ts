/** Signature verification over the RAW body. Header name and algorithm are a single configuration point
 *  (to be confirmed with the Amen team — see docs/04-webhooks.md). */
import { createHmac, timingSafeEqual } from "node:crypto";
export const SIGNATURE_HEADER = "x-signature";
export const ALGORITHM = "sha256";

export function computeSignature(secret: string, rawBody: Buffer | string, algorithm = ALGORITHM): string {
  return createHmac(algorithm, secret).update(rawBody).digest("hex");
}
/** Accepts hex digests, optionally prefixed like "sha256=<hex>". Constant-time comparison. */
export function verifySignature(secret: string, rawBody: Buffer | string, received?: string | null, algorithm = ALGORITHM): boolean {
  if (!received) return false;
  const given = (received.includes("=") ? received.split("=", 2)[1] : received).trim().toLowerCase();
  const expected = computeSignature(secret, rawBody, algorithm);
  return given.length === expected.length && timingSafeEqual(Buffer.from(given), Buffer.from(expected));
}
