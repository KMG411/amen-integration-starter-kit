# Amen API Integration Starter Kit

Official, runnable reference implementations for the **Amen Platform API** (escrow deals, payments, disputes, payouts, webhooks) — one folder per technology stack, all following the same layout and the same reference scenario.

Docs: https://docs.amnn.sa · Sandbox: `https://sandbox-api.amnn.sa` · Live: `https://api.amnn.sa`

## Stacks

| Stack | Folder | Status |
|---|---|---|
| Python 3.11 (`httpx`) | [`python/`](python/) | ✅ client + examples + tests |
| TypeScript / Node 20 | [`typescript/`](typescript/) | ✅ client + examples + tests |
| JavaScript / Node 20 | [`javascript/`](javascript/) | ✅ client + examples + tests |
| PHP 8.1+ (no deps, Laravel-ready) | [`php/`](php/) | ✅ client + examples + tests |
| Flutter / Dart 3 | [`flutter/`](flutter/) | ✅ Dart package + tests · sample app (proxy pattern) |
| iOS / Swift 5.9 | [`swift/`](swift/) | ✅ SwiftPM package + tests · iOS proxy notes |
| Postman | [`postman/`](postman/) | ✅ collection + environments |
| Java 17 | `java/` | planned |
| C# / .NET 8 | `csharp/` | planned |
| Android / Kotlin | `kotlin/` | planned |

> Every stack passes the same offline unit tests. Integration tests mirror `scenario/golden-path.yml` and run against the sandbox when `AMN_API_KEY` is set.

## 5-minute quick start

```sh
git clone git@github.com:KMG411/amen-integration-starter-kit.git
cd amen-integration-starter-kit
cp .env.example .env            # put your sandbox token in AMN_API_KEY

# Python
cd python && pip install -e ".[dev]" && python examples/01_golden_path.py

# TypeScript
cd typescript && npm install && npm run example:golden-path

# JavaScript · PHP · Dart · Swift
cd javascript && npm install && npm run example:golden-path
cd php && composer install && php examples/01_golden_path.php
cd flutter/amen_client && dart pub get && dart run example/golden_path.dart
cd swift && swift run golden-path
```

Each walks a product deal through `draft → requested → payment_pending → paid → executing → executed → completed → payout`, printing the deal status after each step. If the sandbox wallet has no balance, the run stops at `payment_pending` with a `NEEDS_TOP_UP` message and instructions.

## What's shared

| Path | Purpose |
|---|---|
| [`openapi/openapi.yml`](openapi/openapi.yml) | Vendored official spec — single source of truth for models |
| [`openapi/error-catalogue.md`](openapi/error-catalogue.md) | Every error code, meaning, and recommended handling |
| [`scenario/golden-path.yml`](scenario/golden-path.yml) | The reference scenario each stack's integration test mirrors |
| [`docs/`](docs/) | Authentication, deal lifecycle, payments, webhooks, errors & retries, security, versioning, troubleshooting, common mistakes |

## Inside every stack folder

```
<stack>/
├── README.md
├── src/amen/
│   ├── client        auth header, base URL, timeouts, retries
│   ├── config        environment-based configuration
│   ├── models        Deal, Customer, Withdrawal, Webhook … (from the spec)
│   ├── errors        AmenApiError with parsed error codes
│   ├── resources/    lookups, account, customers, deals, deal actions, withdrawals, webhooks
│   └── webhooks/     signature verification + event handler
├── examples/         01_golden_path · 02_cancel_and_dispute · 03_withdrawal · 04_webhook_receiver
└── tests/            unit (offline) · integration (sandbox, needs AMN_API_KEY)
```

## Contributing
See [CONTRIBUTING.md](CONTRIBUTING.md). Licence: [MIT](LICENSE).
