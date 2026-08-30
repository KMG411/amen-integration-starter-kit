# 04 · Webhooks

* Register: `POST /api/v1/web-hooks/ { "url": "https://…" }` → `{ id, url, secret_key }`.
* `secret_key` is returned **only at creation**. Store it in a secret manager immediately (`AMN_WEBHOOK_SECRET`).
* Receiver rules (implemented in every stack's `webhooks/` module):
  1. Read the **raw** request body before parsing.
  2. Verify the signature against the raw bytes + timestamp with `AMN_WEBHOOK_SECRET` (scheme below).
  3. Respond `200` quickly; do the work asynchronously.
  4. De-duplicate — deliveries may repeat (there is no event id; see below).
  5. Never trust the payload alone for money decisions: re-read the deal via `GET /deals/{n}`.

## Signature scheme (verified against real sandbox deliveries, 2026-08-30)

Amen signs the **timestamp and the raw body together**, Stripe-style — not the body alone:

```
signed_message = "{X-Webhook-Timestamp}" + "." + raw_body
X-Webhook-Signature: sha256=HMAC_SHA256(AMN_WEBHOOK_SECRET, signed_message)
```

Verify by recomputing that HMAC over the exact bytes received and comparing in constant time. Because
the timestamp is inside the signed message, you must read it from the header — do not reconstruct it from
the parsed JSON. (You can additionally reject deliveries whose timestamp is far from now to blunt replay.)

### Delivery headers

| Header | Example | Notes |
|---|---|---|
| `X-Webhook-Event` | `deal.status.changed` | event type; also present as `event` in the body |
| `X-Webhook-Signature` | `sha256=f8417828…25e1ff4d` | HMAC-SHA256 hex, `sha256=` prefixed |
| `X-Webhook-Timestamp` | `2026-08-30T18:53:29.884750+00:00` | ISO-8601; part of the signed message |
| `User-Agent` | `Amen-Webhooks/1.0` | |
| `Content-Type` | `application/json` | |

### Payload shape

Envelope is `{ event, timestamp, payload }` where `payload` is the full deal object. The only event type
observed so far is `deal.status.changed`, fired on every lifecycle transition (`paid`, `executing`,
`executed`, `completed`, …); read `payload.status` for the new state.

```json
{
  "event": "deal.status.changed",
  "timestamp": "2026-08-30T18:53:23.885957+00:00",
  "payload": {
    "id": "2",
    "number": "D-0000000002",
    "created_at": 1787841078956,
    "updated_at": 1788116003846,
    "offer": { "id": "4", "number": "O-0000000002", "title": "…", "type": "product",
               "category": { "id": 2, "name": "Product" }, "price": "100.00" },
    "buyer":  { "number": "IC-000000032", "full_name": "Buyer Kit", "phone": "966577841076" },
    "seller": { "number": "IC-000000033", "full_name": "Seller Kit", "phone": "966587841076" },
    "delivery_address": { "city": { "id": 108655, "name": "'Inak" }, "district": "Al Olaya" },
    "price": "100.00", "delivery_fee": "10.00", "seller_amount_transferred": false,
    "status": "paid", "payment": null
  }
}
```

Notes:
- The deal object inside a webhook uses **epoch-ms** for `created_at`/`updated_at`.
- There is **no event id** in the body. For idempotency, de-duplicate on the event `timestamp` (unique per
  delivery, microsecond precision) together with `payload.number`.
