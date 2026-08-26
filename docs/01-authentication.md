# 01 · Authentication & environments

| | Sandbox | Live |
|---|---|---|
| Base URL | `https://sandbox-api.amnn.sa` | `https://api.amnn.sa` |
| Token | issued per environment | issued per environment |

* Every request carries `X-API-Token: <token>` and `Accept: application/json`.
* Tokens are **environment-specific**. Using a token against the wrong environment returns `403 {"error": ["api_token__invalid"]}` (observed) — the same code as a mistyped token, so check `AMN_ENV` first.
* `403 {"error": ["api_access__disabled"]}` means the account exists but API access has not been enabled for it — contact Amen.
* A missing token returns `401`.
* Never embed the token in a mobile or browser app — call your own back end, which holds the token (see `docs/06-security-checklist.md`).
* Configuration in every stack comes from environment variables: `AMN_ENV`, `AMN_API_KEY`, `AMN_WEBHOOK_SECRET`, `AMN_TIMEOUT_MS`. Nothing is hard-coded.

Quick check:

```sh
curl -H "X-API-Token: $AMN_API_KEY" https://sandbox-api.amnn.sa/api/v1/account
```
