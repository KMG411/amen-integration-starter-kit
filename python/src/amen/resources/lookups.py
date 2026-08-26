from __future__ import annotations


class Lookups:
    def __init__(self, client): self._c = client
    def country_codes(self) -> list: return self._c.request("GET", "/allowed-country-codes/")
    def cities(self) -> list: return self._c.request("GET", "/cities")
    def categories(self) -> list: return self._c.request("GET", "/categories/")
    def dispute_reasons(self) -> list: return self._c.request("GET", "/dispute-reasons/")
    def dispute_resolution_reasons(self) -> list: return self._c.request("GET", "/dispute-resolution-reasons/")
    def cancel_reasons(self, party_type: str | None = None) -> list:
        return self._c.request("GET", "/cancel-reasons/", params={"party_type": party_type} if party_type else None)
