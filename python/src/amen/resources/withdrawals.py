from __future__ import annotations
from ..models import Withdrawal, Page


class Withdrawals:
    def __init__(self, client): self._c = client
    def create(self, *, bank_account_id: str, amount: str) -> Withdrawal:
        return Withdrawal.from_api(self._c.request("POST", "/withdrawals/", json={"bank_account_id": bank_account_id, "amount": amount}))
    def get(self, withdrawal_number: str) -> Withdrawal:
        return Withdrawal.from_api(self._c.request("GET", f"/withdrawals/{withdrawal_number}"))
    def list(self, *, page: int | None = None, page_size: int | None = None, status: str | None = None) -> Page:
        params = {k: v for k, v in {"page": page, "page_size": page_size, "status": status}.items() if v is not None}
        return Page.from_api(self._c.request("GET", "/withdrawals/", params=params), "withdrawals")
