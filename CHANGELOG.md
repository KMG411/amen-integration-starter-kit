# Changelog

All notable changes to this kit. Format: [Keep a Changelog](https://keepachangelog.com). Each release notes the Amen API spec it was validated against.

## [0.1.0] — 2026-08-30
Validated against Amen Platform API **v1** (spec vendored at `openapi/openapi.yml`), and the full golden path run end to end against the live sandbox.

### Verified against the live sandbox
- Ran the complete golden path against `sandbox-api.amnn.sa`: create → pay (wallet) → execute → complete → seller payout.
- Captured real webhook deliveries and confirmed the signature scheme.

### Changed / fixed from real-API testing
- **Webhooks: corrected the signature scheme across all 10 stacks.** Real Amen deliveries sign `X-Webhook-Timestamp + "." + raw_body` (HMAC-SHA256) under header `X-Webhook-Signature: sha256=<hex>`, with event type in `X-Webhook-Event` — not the earlier placeholder (`X-Signature` over the body alone). De-duplicate on the event timestamp (no event id in the body). `docs/04-webhooks.md` now documents the scheme with a real captured payload.
- **Deal creation requires a positive `offer_delivery_fee`** (`"0.00"` is rejected as `offer_delivery_fee__required`); golden-path example updated. Documented in `docs/03-payments.md` and `docs/08-troubleshooting.md`.
- Documented that `Checkout.created_at` is epoch-ms while `Deal.created_at` is ISO-8601, and that sandbox wallet top-up is handled by Amen support (no API endpoint).

### Added
- Developer Guide (PDF + HTML source) under `docs/guide/` — how to use this repository end to end.
- Partner Integration Guide (PDF + HTML source) under `docs/guide/`.
- Repository skeleton: shared OpenAPI spec (v1.0), error catalogue (100 codes), golden-path scenario, docs 01–09, Postman collection + environments.
- Python reference implementation (`python/`).
- TypeScript reference implementation (`typescript/`).
- JavaScript (Node) reference implementation (`javascript/`).
- PHP reference implementation, zero runtime dependencies (`php/`).
- Flutter/Dart: pure-Dart `amen_client` package + sample app demonstrating the back-end proxy pattern (`flutter/`).
- Java 17 reference implementation, Maven + Jackson (`java/`).
- C# / .NET 8 reference implementation, System.Text.Json (`csharp/`).
- Kotlin reference implementation, OkHttp + kotlinx.serialization + coroutines, with Android proxy notes (`kotlin/`).
- Swift: SwiftPM `AmenClient` package + CLI golden path + iOS proxy notes (`swift/`).
- CI: unit tests, gitleaks, nightly sandbox run, weekly spec-drift check.
