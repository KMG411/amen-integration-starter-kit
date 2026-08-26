/** Signature verification over the RAW body. Header name and algorithm are one configuration point (docs/04-webhooks.md). */
import { createHmac, timingSafeEqual } from "node:crypto";
export const SIGNATURE_HEADER = "x-signature";
export const ALGORITHM = "sha256";
export const computeSignature = (secret, rawBody, algorithm = ALGORITHM) => createHmac(algorithm, secret).update(rawBody).digest("hex");
export function verifySignature(secret, rawBody, received, algorithm = ALGORITHM) {
  if (!received) return false;
  const given = (received.includes("=") ? received.split("=", 2)[1] : received).trim().toLowerCase();
  const expected = computeSignature(secret, rawBody, algorithm);
  return given.length === expected.length && timingSafeEqual(Buffer.from(given), Buffer.from(expected));
}
