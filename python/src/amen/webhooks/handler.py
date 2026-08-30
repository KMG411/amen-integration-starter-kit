from __future__ import annotations
import json
from dataclasses import dataclass
from typing import Any, Callable
from .verify import verify_signature, SIGNATURE_HEADER, TIMESTAMP_HEADER


@dataclass
class WebhookEvent:
    id: str | None
    type: str | None
    data: dict[str, Any]
    raw: bytes


class WebhookHandler:
    """Framework-agnostic: call `handle(headers, raw_body)` from Flask/FastAPI/Django/etc.

    Rules implemented: verify raw body first, de-duplicate by id, dispatch by event type.
    """

    def __init__(self, secret: str, on_event: Callable[[WebhookEvent], None], seen: set[str] | None = None):
        self.secret, self.on_event, self.seen = secret, on_event, seen if seen is not None else set()

    def handle(self, headers: dict[str, str], raw_body: bytes) -> tuple[int, dict]:
        h = {k.lower(): v for k, v in headers.items()}
        sig = h.get(SIGNATURE_HEADER.lower())
        timestamp = h.get(TIMESTAMP_HEADER.lower())
        if not verify_signature(self.secret, timestamp, raw_body, sig):
            return 401, {"error": "invalid signature"}
        try:
            payload = json.loads(raw_body)
        except ValueError:
            return 400, {"error": "invalid json"}
        # Amen deliveries carry no event id; the event timestamp is unique per delivery.
        event = WebhookEvent(id=str(payload.get("timestamp") or timestamp or ""),
                             type=payload.get("event") or payload.get("type"), data=payload, raw=raw_body)
        if event.id and event.id in self.seen:
            return 200, {"ok": True, "duplicate": True}
        if event.id:
            self.seen.add(event.id)
        self.on_event(event)          # keep this fast; queue heavy work
        return 200, {"ok": True}
