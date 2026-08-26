# 04 · Webhooks

* Register: `POST /api/v1/web-hooks/ { "url": "https://…" }` → `{ id, url, secret_key }`.
* `secret_key` is returned **only at creation**. Store it in a secret manager immediately (`AMN_WEBHOOK_SECRET`).
* Receiver rules (implemented in every stack's `webhooks/` module):
  1. Read the **raw** request body before parsing.
  2. Verify the signature against the raw bytes with `AMN_WEBHOOK_SECRET`.
  3. Respond `200` quickly; do the work asynchronously.
  4. De-duplicate by event id — deliveries may repeat.
  5. Never trust the payload alone for money decisions: re-read the deal via `GET /deals/{n}`.

> **Status:** event payload shape and the exact signature algorithm are being confirmed with the Amen team.
> The helpers in this kit verify HMAC-SHA256 over the raw body by default and are written so the header name
> and algorithm are a single configuration point. This document will be updated with captured examples.
