# 07 · Versioning

* API base path `/api/v1` is a constant in each client.
* Kit releases are tagged `v1.x.y`; `CHANGELOG.md` records the `openapi.yml` date each release was validated against.
* Clients ignore unknown JSON fields, so additive API changes never break them.
* `.github/workflows/spec-drift.yml` compares the vendored spec with `https://docs.amnn.sa/openapi.yml` weekly and opens an issue on change.
