# 08 · Troubleshooting

Format: **Symptom → Check → Cause → Fix**

| Symptom | Check | Cause | Fix |
|---|---|---|---|
| `403 api_token__invalid` on every call | base URL printed at startup | sandbox token vs live URL (or vice-versa), or mistyped token | set `AMN_ENV` correctly / re-copy the token |
| `403 api_access__disabled` | — | API access not enabled for this account | contact Amen |
| `400` on `action/submit` | deal has buyer **and** seller; product deal has delivery address | parties/address missing | call `parties/` and `delivery-address` first |
| `400 price__required` on approve | `offer_type` | service deals need a final price | pass `{ "price": "…" }` |
| deal stuck in `payment_pending` | `allowed-payment-methods/` | wallet not allowed / no balance; online checkout not completed | top up sandbox wallet or complete HyperPay checkout |
| `429` | request rate | burst of calls | client backoff; batch work |
| webhook signature mismatch | you verify the **raw** body? | body re-serialised before verification | verify bytes first, parse second |
| dates in year 57000 | timestamp handling | epoch **milliseconds** treated as seconds | divide by 1000 |
