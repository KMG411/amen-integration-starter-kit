# Changelog

All notable changes to this kit. Format: [Keep a Changelog](https://keepachangelog.com). Each release notes the Amen API spec it was validated against.

## [Unreleased]
### Added
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
