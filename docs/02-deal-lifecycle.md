# 02 · Deal lifecycle

```
draft ──submit──▶ requested ──approve──▶ payment_pending ──pay──▶ paid
                                                                  │
                                                        execution-start
                                                                  ▼
completed ◀──complete── executed ◀──execution-complete── executing
    │
    ├── transfer-seller-amount  (payout; status stays completed)
    └── dispute ──▶ disputed ──▶ dispute-approve → dispute_approved
                                └─▶ dispute-decline → completed
cancel: allowed from draft, requested (submitted), payment_pending, paid, executing → canceled
payment_overdue: set by Amen when a payment_pending deal is not paid in time
```

| Action | Allowed from | Body |
|---|---|---|
| `PUT /deals/{n}` / `DELETE` | draft | — |
| `parties/`, `delivery-address`, `billing-address` | draft | see spec |
| `action/submit` | draft (needs buyer + seller; product deals need a delivery address) | none |
| `action/approve` | requested | `{ "price": "…" }` required for `offer_type=service` |
| `action/make-payment-wallet` | payment_pending (wallet method must be allowed) | none |
| `action/make-payment-online` | payment_pending | `{ "payment_method": "mada" \| "visa_master" \| "applepay" }` → HyperPay checkout |
| `action/execution-start` | paid | none |
| `action/execution-complete` | executing | none |
| `action/complete` | executed | none |
| `action/transfer-seller-amount` | completed | none |
| `action/dispute` | completed | multipart: `reason`, `comment`, optional attachments |
| `action/dispute-approve` / `dispute-decline` | disputed | multipart: `reason`, `comment` |
| `action/cancel` | draft, submitted, payment_pending, paid, executing | `{ deal_party, reason, comment }` |

Calling an action from the wrong status returns `400` with a `*__status`/`*__not_allowed` code — never retry those; re-read the deal instead.
