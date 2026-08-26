from __future__ import annotations
from ..models import Account as AccountModel


class Account:
    def __init__(self, client): self._c = client
    def get(self) -> AccountModel: return AccountModel.from_api(self._c.request("GET", "/account"))
    def bank_accounts(self) -> list: return self._c.request("GET", "/account/bank-accounts/")
    def link_bank_account(self, iban: str, proof_document: tuple | None = None) -> dict:
        files = {"proof_document": proof_document} if proof_document else None
        return self._c.request("POST", "/account/bank-accounts/", data={"iban": iban}, files=files or {"_": (None, "")})
    def delete_bank_account(self, bank_account_id: str) -> None:
        self._c.request("DELETE", f"/account/bank-accounts/{bank_account_id}")
