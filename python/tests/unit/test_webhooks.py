import json
from amen.webhooks import compute_signature, verify_signature, WebhookHandler
from amen.webhooks import SIGNATURE_HEADER, TIMESTAMP_HEADER

SECRET = "unit-test-secret"
TS = "2026-08-30T18:53:23.885957+00:00"


def test_signature_roundtrip_and_prefix():
    body = b'{"event":"deal.status.changed","timestamp":"' + TS.encode() + b'"}'
    sig = compute_signature(SECRET, TS, body)
    assert verify_signature(SECRET, TS, body, sig)
    assert verify_signature(SECRET, TS, body, f"sha256={sig}")
    # tampering with the body, the timestamp, or dropping either input must fail
    assert not verify_signature(SECRET, TS, body + b" ", sig)
    assert not verify_signature(SECRET, TS + "0", body, sig)
    assert not verify_signature(SECRET, None, body, sig)
    assert not verify_signature(SECRET, TS, body, None)


def test_handler_verifies_then_dedupes():
    seen = []
    h = WebhookHandler(SECRET, on_event=lambda e: seen.append((e.type, e.id)))
    body = json.dumps({"event": "deal.status.changed", "timestamp": TS,
                       "payload": {"number": "D-0000000002", "status": "paid"}}).encode()
    good = {SIGNATURE_HEADER: f"sha256={compute_signature(SECRET, TS, body)}", TIMESTAMP_HEADER: TS}
    bad = {SIGNATURE_HEADER: "sha256=bad", TIMESTAMP_HEADER: TS}
    assert h.handle(bad, body)[0] == 401
    assert h.handle(good, body) == (200, {"ok": True})
    assert h.handle(good, body) == (200, {"ok": True, "duplicate": True})
    assert seen == [("deal.status.changed", TS)]
