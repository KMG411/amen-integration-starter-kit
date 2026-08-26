# 03 · Payments

1. After `approve` the deal is `payment_pending`.
2. `GET /deals/{n}/allowed-payment-methods/` tells you which methods apply (`wallet`, `mada`, `visa_master`, `applepay`).
3. **Wallet**: `POST action/make-payment-wallet` — synchronous, deal becomes `paid` if the buyer wallet has balance.
4. **Online**: `POST action/make-payment-online {payment_method}` returns a `Checkout` (`provider: hyperpay`, `hyperpay.checkout_id`). Render the HyperPay widget with that id in your UI; Amen marks the deal `paid` when the provider confirms. Poll `GET /deals/{n}` or wait for the webhook.
5. Money is always a decimal **string** with two places (`"100.00"`), in SAR. Never send floats.

In the sandbox, top up the wallet using the `top_up_account` details from `GET /api/v1/account`.
