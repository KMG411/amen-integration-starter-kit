import json
from amen.webhooks import compute_signature, verify_signature, WebhookHandler

SECRET = "unit-test-secret"


def test_signature_roundtrip_and_prefix():
    body = b'{"id":"e1","event":"deal.paid"}'
    sig = compute_signature(SECRET, body)
    assert verify_signature(SECRET, body, sig)
    assert verify_signature(SECRET, body, f"sha256={sig}")
    assert not verify_signature(SECRET, body + b" ", sig)
    assert not verify_signature(SECRET, body, None)


def test_handler_verifies_then_dedupes():
    seen = []
    h = WebhookHandler(SECRET, on_event=lambda e: seen.append(e.id))
    body = json.dumps({"id": "e1", "event": "deal.paid"}).encode()
    good = {"X-Signature": compute_signature(SECRET, body)}
    assert h.handle({"X-Signature": "bad"}, body)[0] == 401
    assert h.handle(good, body) == (200, {"ok": True})
    assert h.handle(good, body) == (200, {"ok": True, "duplicate": True})
    assert seen == ["e1"]
