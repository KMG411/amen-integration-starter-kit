#!/usr/bin/env python3
"""Withdraw available wallet funds to a linked bank account."""
from amen import AmenClient

amen = AmenClient()
account = amen.account.get()
print("wallet:", account.wallet)
banks = amen.account.bank_accounts()
if not banks:
    print("No linked bank account. Link one with amen.account.link_bank_account(iban=...) (needs review by Amen).")
    raise SystemExit(0)
w = amen.withdrawals.create(bank_account_id=banks[0]["id"], amount="10.00")
print(f"withdrawal {w.number}: {w.status}")
for item in amen.withdrawals.list().items:
    print(" -", item.get("number"), item.get("status"), item.get("amount"))
