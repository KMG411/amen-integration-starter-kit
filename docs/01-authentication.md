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

## Writes require a CSRF token (POST / PUT / DELETE)

The API is served by Django with CSRF protection on unsafe methods. `X-API-Token` alone works for `GET`, but **`POST`, `PUT` and `DELETE` also require a Django "double-submit" CSRF token** — otherwise the server returns `403` with an HTML page: *"CSRF verification failed … CSRF cookie not set."*

Every reference client handles this automatically: it generates one **32-character hex** token per client and sends it two ways on every mutating request:

```
X-CSRFToken: <32-hex-token>
Cookie: csrftoken=<same 32-hex-token>
```

The value must be 32 (or 64) characters from `[a-zA-Z0-9]`; a hex string from 16 random bytes satisfies this. Both the header **and** the cookie must be present and identical — sending only one still fails. (Note: some HTTP stacks strip or manage the `Cookie` header themselves — the .NET client disables that with `UseCookies=false`, and the Swift client uses a session with `httpShouldSetCookies=false`.)

## Localized lookups need Accept-Language

Lookup endpoints return localized names and **error `500 {"error":["internal"]}` when no language is negotiated**. The clients send `Accept-Language: en` on every request to avoid this.

## Timestamps

Timestamp fields (`created_at`, `updated_at`) are **ISO-8601 strings** (e.g. `"2026-08-26T18:04:42.825Z"`), not epoch numbers. The models parse them to native date types.

Quick check:

```sh
curl -H "X-API-Token: $AMN_API_KEY" https://sandbox-api.amnn.sa/api/v1/account
```
