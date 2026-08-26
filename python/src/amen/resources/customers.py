from __future__ import annotations
from typing import Iterator
from ..models import Customer, Page


class Customers:
    def __init__(self, client): self._c = client

    def create(self, *, first_name: str, last_name: str, phone_code: str, phone_number: str) -> Customer:
        return Customer.from_api(self._c.request("POST", "/customers/", json={
            "first_name": first_name, "last_name": last_name, "phone_code": phone_code, "phone_number": phone_number}))

    def get(self, customer_number: str) -> Customer:
        return Customer.from_api(self._c.request("GET", f"/customers/{customer_number}"))

    def list(self, *, page: int | None = None, per_page: int | None = None, **filters) -> Page:
        params = {k: v for k, v in {"page": page, "per_page": per_page, **filters}.items() if v is not None}
        return Page.from_api(self._c.request("GET", "/customers/", params=params), "customers")

    def iter_all(self, **filters) -> Iterator[Customer]:
        """Iterate over every page — never process only the first page by accident."""
        page = 0
        while True:
            p = self.list(page=page, **filters)
            yield from (Customer.from_api(i) for i in p.items)
            page += 1
            if page >= p.pages or not p.items:
                return
