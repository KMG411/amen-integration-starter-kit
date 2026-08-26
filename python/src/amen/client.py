"""AmenClient — the one place that knows about auth headers, base URL, timeouts and retries."""
from __future__ import annotations
import logging, random, time
from typing import Any
import httpx
from .config import Config, API_PREFIX
from .errors import AmenApiError
from .resources.lookups import Lookups
from .resources.account import Account as AccountResource
from .resources.customers import Customers
from .resources.deals import Deals
from .resources.withdrawals import Withdrawals
from .resources.webhooks import Webhooks

log = logging.getLogger("amen")


class AmenClient:
    def __init__(self, config: Config | None = None, transport: httpx.BaseTransport | None = None):
        self.config = config or Config.from_env()
        self._http = httpx.Client(
            base_url=self.config.base_url,
            headers={"X-API-Token": self.config.api_key, "Accept": "application/json",
                     "User-Agent": "amen-starter-kit-python/0.1"},
            timeout=self.config.timeout_s, transport=transport)
        log.info("AmenClient → %s (%s)", self.config.base_url, self.config.env)
        self.lookups, self.account = Lookups(self), AccountResource(self)
        self.customers, self.deals = Customers(self), Deals(self)
        self.withdrawals, self.webhooks = Withdrawals(self), Webhooks(self)

    # ------------------------------------------------------------------ core
    def request(self, method: str, path: str, *, json: Any = None, params: dict | None = None,
                data: dict | None = None, files: dict | None = None) -> Any:
        url = API_PREFIX + path
        headers = {}
        if method in ("POST", "PUT", "DELETE"):
            # Amen's API applies CSRF-style origin checks on mutating requests.
            headers["Origin"] = headers["Referer"] = self.config.base_url
        attempt = 0
        while True:
            attempt += 1
            try:
                resp = self._http.request(method, url, json=json, params=params, data=data, files=files, headers=headers)
            except httpx.TransportError as exc:
                if attempt > self.config.max_retries:
                    raise
                self._sleep(attempt, None); log.warning("network error %s, retry %d", exc, attempt); continue
            if resp.status_code < 400:
                return resp.json() if resp.content else None
            err = self._to_error(resp, method, url)
            if err.retryable and attempt <= self.config.max_retries:
                self._sleep(attempt, resp.headers.get("Retry-After")); log.warning("%s, retry %d", err, attempt); continue
            raise err

    @staticmethod
    def _to_error(resp: httpx.Response, method: str, url: str) -> AmenApiError:
        try:
            body = resp.json()
        except ValueError:
            body = resp.text
        codes = body.get("error", []) if isinstance(body, dict) else []
        if isinstance(codes, str):
            codes = [codes]
        return AmenApiError(resp.status_code, [str(c) for c in codes], method, url, body)

    @staticmethod
    def _sleep(attempt: int, retry_after: str | None) -> None:
        delay = float(retry_after) if retry_after and retry_after.isdigit() else min(2 ** attempt, 20) + random.random()
        time.sleep(delay)

    def close(self) -> None:
        self._http.close()

    def __enter__(self):
        return self

    def __exit__(self, *exc):
        self.close()
