"""Webhook signature verification over the RAW request body.

The header name and algorithm are a single configuration point here so they can be
updated once the Amen team confirms the final scheme (see docs/04-webhooks.md).
"""
from __future__ import annotations
import hashlib, hmac

SIGNATURE_HEADER = "X-Signature"      # configuration point
ALGORITHM = "sha256"                  # configuration point


def compute_signature(secret: str, raw_body: bytes, algorithm: str = ALGORITHM) -> str:
    return hmac.new(secret.encode(), raw_body, getattr(hashlib, algorithm)).hexdigest()


def verify_signature(secret: str, raw_body: bytes, received: str | None, algorithm: str = ALGORITHM) -> bool:
    """Constant-time comparison. Accepts hex digests, optionally prefixed like 'sha256=<hex>'."""
    if not received:
        return False
    received = received.split("=", 1)[1] if "=" in received else received
    return hmac.compare_digest(compute_signature(secret, raw_body, algorithm), received.strip().lower())
