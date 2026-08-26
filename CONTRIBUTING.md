# Contributing

## Conventions every stack follows
| Concern | Name |
|---|---|
| Client | `AmenClient` (one instance, one place that sets `X-API-Token`, base URL, timeout, retries) |
| Resources | `lookups`, `account`, `customers`, `deals`, `deals.actions`, `withdrawals`, `webhooks` |
| Error | `AmenApiError { status, codes[], has(code) }` |
| Config | env vars `AMN_ENV`, `AMN_API_KEY`, `AMN_WEBHOOK_SECRET`, `AMN_TIMEOUT_MS` — read once, in `config` |
| Money | string, 2 decimals, SAR; never floats |
| Timestamps | epoch-ms from the API → native datetime in models |
| Layout | `src/amen/{client,config,errors,models,resources/,webhooks/}`, `examples/01..04`, `tests/{unit,integration}` |

## Adding a stack
1. Copy the layout from `python/` or `typescript/`.
2. Implement `examples/01_golden_path` so it passes `scenario/golden-path.yml`.
3. Unit tests must run without credentials; integration tests skip when `AMN_API_KEY` is unset.
4. Add a row to the root README stack table and a job to `.github/workflows/ci.yml`.

## Rules
- Never commit tokens or `.env`. CI runs gitleaks.
- Keep examples small: the goal is code partners can copy, not a framework.
- Update `openapi/openapi.yml` only from `https://docs.amnn.sa/openapi.yml`, and note the date in `CHANGELOG.md`.
