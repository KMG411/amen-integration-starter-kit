from __future__ import annotations


class AmenApiError(Exception):
    """Raised for any non-2xx response. `codes` holds the API's error codes (e.g. price__required)."""

    def __init__(self, status: int, codes: list[str], method: str, path: str, body=None):
        self.status, self.codes, self.method, self.path, self.body = status, codes, method, path, body
        super().__init__(f"{status} {method} {path}: {', '.join(codes) or body}")

    def has(self, code: str) -> bool:
        return code in self.codes

    @property
    def retryable(self) -> bool:
        return self.status == 429 or self.status >= 500


class AmenLifecycleError(ValueError):
    """Raised locally before calling the API when an action is not valid for the deal's current status."""
