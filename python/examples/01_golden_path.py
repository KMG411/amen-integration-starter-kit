#!/usr/bin/env python3
"""Golden path (scenario/golden-path.yml): product deal from creation to seller payout.

    python examples/01_golden_path.py            # full run
    python examples/01_golden_path.py DL-000123  # resume a deal that is already 'paid'
"""
import sys
from amen import AmenClient, AmenApiError
from _common import unique_phone, step

amen = AmenClient()
print(f"environment: {amen.config.env} ({amen.config.base_url})\n")


def continue_from_paid(n: str) -> None:
    step("execution-start", amen.deals.actions.execution_start(n))
    step("execution-complete", amen.deals.actions.execution_complete(n))
    step("complete", amen.deals.actions.complete(n))
    step("transfer-seller-amount (payout)", amen.deals.actions.transfer_seller_amount(n))
    print(f"\n🎉 deal {n} finished: {amen.deals.get(n).status}")


if len(sys.argv) > 1:
    continue_from_paid(sys.argv[1]); sys.exit(0)

# 1. customers
buyer = amen.customers.create(first_name="Buyer", last_name="Kit", phone_code="SA", phone_number=unique_phone("57"))
seller = amen.customers.create(first_name="Seller", last_name="Kit", phone_code="SA", phone_number=unique_phone("58"))
step(f"customers {buyer.number} (buyer), {seller.number} (seller)")

# 2. deal
category = amen.lookups.categories()[0]["id"]
city = amen.lookups.cities()[0]["id"]
deal = amen.deals.create(offer_type="product", offer_category=category, offer_title="Starter Kit golden path",
                         offer_description="Reference deal created by the Amen integration starter kit",
                         offer_price="100.00", offer_delivery_fee="10.00")
n = deal.number
step(f"deal {n} created", deal)
step("parties", amen.deals.set_parties(n, buyers=[buyer.number], sellers=[seller.number]))
step("delivery address", amen.deals.set_delivery_address(n, city=city, district="Al Olaya", street="King Fahd Rd",
                                                         building_number="1234", unit_number="1", zip_code="12211"))

# 3. lifecycle
step("submit", amen.deals.actions.submit(n))
step("approve", amen.deals.actions.approve(n))
methods = amen.deals.allowed_payment_methods(n)
print(f"  allowed payment methods: {methods}")
try:
    step("pay with wallet", amen.deals.actions.pay_with_wallet(n))
except AmenApiError as e:
    checkout = amen.deals.actions.pay_online(n, "mada")
    print(f"""
⏸  NEEDS_TOP_UP — wallet payment not possible ({', '.join(e.codes) or e.status}).
   An online (HyperPay) checkout was created instead: {checkout.raw}
   Either complete that checkout in a UI, or top up the sandbox wallet
   (GET /api/v1/account → wallet.top_up_account), then resume with:
       python examples/01_golden_path.py {n}""")
    sys.exit(0)

continue_from_paid(n)
