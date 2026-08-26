from __future__ import annotations
from typing import Iterator
from ..models import Deal, Page, Checkout
from ..errors import AmenLifecycleError

# Which deal statuses each action may be called from (docs/02-deal-lifecycle.md)
ALLOWED_FROM = {
    "submit": {"draft"}, "approve": {"requested"},
    "make-payment-wallet": {"payment_pending"}, "make-payment-online": {"payment_pending"},
    "execution-start": {"paid"}, "execution-complete": {"executing"}, "complete": {"executed"},
    "transfer-seller-amount": {"completed"}, "dispute": {"completed"},
    "dispute-approve": {"disputed"}, "dispute-decline": {"disputed"},
    "cancel": {"draft", "submitted", "requested", "payment_pending", "paid", "executing"},
}


class DealActions:
    """POST /deals/{n}/action/* — every method returns the updated Deal (or a Checkout for online payment)."""

    def __init__(self, client, deals): self._c, self._deals = client, deals

    def _act(self, deal_number: str, action: str, *, json=None, data=None, files=None, check: bool = True) -> dict:
        if check:
            status = self._deals.get(deal_number).status
            if status not in ALLOWED_FROM[action]:
                raise AmenLifecycleError(
                    f"action '{action}' is not allowed from status '{status}' (allowed: {sorted(ALLOWED_FROM[action])})")
        return self._c.request("POST", f"/deals/{deal_number}/action/{action}", json=json, data=data, files=files)

    def submit(self, n, check=True) -> Deal: return Deal.from_api(self._act(n, "submit", check=check))
    def approve(self, n, price: str | None = None, check=True) -> Deal:
        return Deal.from_api(self._act(n, "approve", json={"price": price} if price else {}, check=check))
    def pay_with_wallet(self, n, check=True) -> Deal: return Deal.from_api(self._act(n, "make-payment-wallet", check=check))
    def pay_online(self, n, payment_method: str = "mada", check=True) -> Checkout:
        return Checkout.from_api(self._act(n, "make-payment-online", json={"payment_method": payment_method}, check=check))
    def execution_start(self, n, check=True) -> Deal: return Deal.from_api(self._act(n, "execution-start", check=check))
    def execution_complete(self, n, check=True) -> Deal: return Deal.from_api(self._act(n, "execution-complete", check=check))
    def complete(self, n, check=True) -> Deal: return Deal.from_api(self._act(n, "complete", check=check))
    def transfer_seller_amount(self, n, check=True) -> Deal: return Deal.from_api(self._act(n, "transfer-seller-amount", check=check))
    def cancel(self, n, *, deal_party: str, reason: int, comment: str, check=True) -> Deal:
        return Deal.from_api(self._act(n, "cancel", json={"deal_party": deal_party, "reason": reason, "comment": comment}, check=check))
    def dispute(self, n, *, reason: int, comment: str, attachments: list[tuple] | None = None, check=True) -> Deal:
        files = {f"attachment_{i+1}": a for i, a in enumerate(attachments or [])} or {"_": (None, "")}
        return Deal.from_api(self._act(n, "dispute", data={"reason": reason, "comment": comment}, files=files, check=check))
    def dispute_approve(self, n, *, reason: int, comment: str, check=True) -> Deal:
        return Deal.from_api(self._act(n, "dispute-approve", data={"reason": reason, "comment": comment}, files={"_": (None, "")}, check=check))
    def dispute_decline(self, n, *, reason: int, comment: str, check=True) -> Deal:
        return Deal.from_api(self._act(n, "dispute-decline", data={"reason": reason, "comment": comment}, files={"_": (None, "")}, check=check))


class Deals:
    def __init__(self, client):
        self._c = client
        self.actions = DealActions(client, self)

    def create(self, *, offer_type: str, offer_title: str, offer_price: str | None = None, offer_category: int | None = None,
               offer_description: str | None = None, offer_delivery_fee: str | None = None,
               deal_subject_details: str | None = None) -> Deal:
        body = {k: v for k, v in locals().items() if k != "self" and v is not None}
        return Deal.from_api(self._c.request("POST", "/deals/", json=body))

    def get(self, deal_number: str) -> Deal:
        return Deal.from_api(self._c.request("GET", f"/deals/{deal_number}"))

    def update(self, deal_number: str, **fields) -> Deal:
        return Deal.from_api(self._c.request("PUT", f"/deals/{deal_number}", json=fields))

    def delete(self, deal_number: str) -> None:
        self._c.request("DELETE", f"/deals/{deal_number}")

    def list(self, *, page: int | None = None, per_page: int | None = None, status: str | None = None) -> Page:
        params = {k: v for k, v in {"page": page, "per_page": per_page, "status": status}.items() if v is not None}
        return Page.from_api(self._c.request("GET", "/deals/", params=params), "deals")

    def iter_all(self, **filters) -> Iterator[Deal]:
        page = 0
        while True:
            p = self.list(page=page, **filters)
            yield from (Deal.from_api(i) for i in p.items)
            page += 1
            if page >= p.pages or not p.items:
                return

    def set_parties(self, deal_number: str, *, buyers: list[str], sellers: list[str]) -> Deal:
        return Deal.from_api(self._c.request("POST", f"/deals/{deal_number}/parties/", json={"buyers": buyers, "sellers": sellers}))

    def set_delivery_address(self, deal_number: str, *, city: int, street: str, building_number: str, zip_code: str,
                             district: str | None = None, unit_number: str | None = None) -> Deal:
        body = {k: v for k, v in locals().items() if k not in ("self", "deal_number") and v is not None}
        return Deal.from_api(self._c.request("POST", f"/deals/{deal_number}/delivery-address", json=body))

    def set_billing_address(self, deal_number: str, **address) -> Deal:
        return Deal.from_api(self._c.request("POST", f"/deals/{deal_number}/billing-address", json=address))

    def allowed_payment_methods(self, deal_number: str) -> list[str]:
        return self._c.request("GET", f"/deals/{deal_number}/allowed-payment-methods/").get("payment_methods", [])
