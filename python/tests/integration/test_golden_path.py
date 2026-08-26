"""Mirrors scenario/golden-path.yml step by step. Runs only with sandbox credentials."""
import time, pytest
from amen import AmenApiError

pytestmark = pytest.mark.integration


def _phone(prefix): return f"{prefix}{str(int(time.time()))[-7:]}"[:9]


def test_golden_path(sandbox):
    amen = sandbox
    buyer = amen.customers.create(first_name="Buyer", last_name="Kit", phone_code="SA", phone_number=_phone("57"))
    seller = amen.customers.create(first_name="Seller", last_name="Kit", phone_code="SA", phone_number=_phone("58"))
    assert buyer.number and seller.number

    deal = amen.deals.create(offer_type="product", offer_category=amen.lookups.categories()[0]["id"],
                             offer_title="Starter Kit golden path", offer_price="100.00", offer_delivery_fee="0.00",
                             offer_description="Reference deal created by the Amen integration starter kit")
    n = deal.number
    assert deal.status == "draft"
    assert amen.deals.set_parties(n, buyers=[buyer.number], sellers=[seller.number]).status == "draft"
    assert amen.deals.set_delivery_address(n, city=amen.lookups.cities()[0]["id"], district="Al Olaya", street="King Fahd Rd",
                                           building_number="1234", unit_number="1", zip_code="12211").status == "draft"
    assert amen.deals.actions.submit(n).status == "requested"
    assert amen.deals.actions.approve(n).status == "payment_pending"

    try:
        paid = amen.deals.actions.pay_with_wallet(n)
    except AmenApiError as e:
        pytest.skip(f"NEEDS_TOP_UP: wallet payment not available ({e.codes}); remaining steps need a paid deal")
    assert paid.status == "paid"
    assert amen.deals.actions.execution_start(n).status == "executing"
    assert amen.deals.actions.execution_complete(n).status == "executed"
    assert amen.deals.actions.complete(n).status == "completed"
    assert amen.deals.actions.transfer_seller_amount(n).status == "completed"


def test_lookups_and_account(sandbox):
    assert sandbox.lookups.categories() and sandbox.lookups.cities()
    assert sandbox.account.get().raw
