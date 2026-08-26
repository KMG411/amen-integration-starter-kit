# 06 · Security checklist

- [ ] API token only from environment / secret manager; `.env` is git-ignored.
- [ ] Mobile and browser apps never hold the token — they call **your** back end, which calls Amen.
- [ ] TLS only; no token in URLs or logs (client redacts `X-API-Token`).
- [ ] Separate sandbox and live tokens; rotate on staff change; revoke on suspected leak.
- [ ] Webhook: verify signature on raw body, de-duplicate, reply 200 fast, re-read the deal before acting.
- [ ] Least data: store deal/customer numbers, not full payloads, unless you need them.
- [ ] Dependencies pinned; `gitleaks` runs in CI on every push.
