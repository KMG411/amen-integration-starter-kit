# 03 · Payments

1. After `approve` the deal is `payment_pending`.
2. `GET /deals/{n}/allowed-payment-methods/` tells you which methods apply (`wallet`, `mada`, `visa_master`, `applepay`).
3. **Wallet**: `POST action/make-payment-wallet` — synchronous, deal becomes `paid` if the buyer wallet has balance.
4. **Online**: `POST action/make-payment-online {payment_method}` returns a `Checkout` (`provider: hyperpay`, `hyperpay.checkout_id`). Render the HyperPay widget with that id in your UI; Amen marks the deal `paid` when the provider confirms. Poll `GET /deals/{n}` or wait for the webhook.
5. Money is always a decimal **string** with two places (`"100.00"`), in SAR. Never send floats.

## Sandbox notes (verified 2026-08-27)

- **`offer_delivery_fee` must be greater than zero.** Sending `"0.00"` is rejected at deal creation with `400 offer_delivery_fee__required` — the API treats a zero fee as missing. Send a positive value (e.g. `"10.00"`) for product deals. The buyer then pays `offer_price + offer_delivery_fee` (e.g. `100.00 + 10.00 = 110.00`).
- **The `Checkout` object's `created_at` is epoch-milliseconds** (e.g. `1787841080593`), unlike `Deal.created_at` which is an ISO-8601 string. Timestamp formats are mixed across resources — parse each field defensively.
- **Funding the sandbox wallet is not an API action.** `GET /api/v1/account` → `broker_account.wallet.top_up_account` returns only bank details (name/bank/account_number/IBAN); there is no endpoint to instantly credit test balance. With a `0.00` balance, `make-payment-wallet` fails with `wallet__low_balance`, and `make-payment-online` returns a HyperPay checkout that must be completed out-of-band. **How a sandbox buyer actually pays a deal to reach `paid` is an open question with Amen support** (asked re: deal `D-0000000002`).
