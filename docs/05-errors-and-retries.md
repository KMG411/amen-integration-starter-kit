# 05 · Errors & retries

Error body: `{ "error": ["code", …] }`. Full list: [`openapi/error-catalogue.md`](../openapi/error-catalogue.md).

| Class | Retry? | Do |
|---|---|---|
| `400` validation (`field__required`, `field__invalid`) | no | fix the request |
| `400` lifecycle (`deal__*status*`) | no | re-read the deal; follow `02-deal-lifecycle.md` |
| `401` / `403 api_token__invalid` | no | check token / environment |
| `403 api_access__disabled` | no | contact Amen to enable API access |
| `404` | no | check identifier |
| `429 rate_limit__exceeded` | yes | exponential backoff + jitter, honour `Retry-After` |
| `5xx` / network | yes | backoff, max 3 attempts, then alert |

Every stack exposes one exception type — `AmenApiError` — with `status`, `codes`, and `has(code)`, and applies the retry policy above inside the client so callers do not reimplement it.
