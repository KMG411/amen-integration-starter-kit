"""Webhook signature verification.

Verified against real Amen sandbox deliveries (2026-08-30). Amen signs the
**timestamp and the raw body together**, Stripe-style:

    signed_message = f"{X-Webhook-Timestamp}.{raw_body}"
    X-Webhook-Signature: sha256=HMAC_SHA256(secret, signed_message)

Always feed the RAW bytes and the timestamp header straight from the request —
re-serialising the JSON changes the bytes and breaks the signature.
"""
from __future__ import annotations
import hashlib, hmac

SIGNATURE_HEADER = "X-Webhook-Signature"
TIMESTAMP_HEADER = "X-Webhook-Timestamp"
EVENT_HEADER = "X-Webhook-Event"
ALGORITHM = "sha256"


def compute_signature(secret: str, timestamp: str, raw_body: bytes, algorithm: str = ALGORITHM) -> str:
    message = timestamp.encode() + b"." + raw_body
    return hmac.new(secret.encode(), message, getattr(hashlib, algorithm)).hexdigest()


def verify_signature(secret: str, timestamp: str | None, raw_body: bytes, received: str | None,
                     algorithm: str = ALGORITHM) -> bool:
    """Constant-time comparison. `received` may be prefixed like 'sha256=<hex>'."""
    if not received or not timestamp:
        return False
    received = received.split("=", 1)[1] if "=" in received else received
    expected = compute_signature(secret, timestamp, raw_body, algorithm)
    return hmac.compare_digest(expected, received.strip().lower())
