#!/usr/bin/env python3
"""Secondary scenarios: cancel a draft deal; dispute + dispute-approve a completed deal.

    python examples/02_cancel_and_dispute.py cancel
    python examples/02_cancel_and_dispute.py dispute DL-000123   # deal must be 'completed'
"""
import sys
from amen import AmenClient
from _common import unique_phone, step

amen = AmenClient()
mode = sys.argv[1] if len(sys.argv) > 1 else "cancel"

if mode == "cancel":
    buyer = amen.customers.create(first_name="Buyer", last_name="Kit", phone_code="SA", phone_number=unique_phone("57"))
    seller = amen.customers.create(first_name="Seller", last_name="Kit", phone_code="SA", phone_number=unique_phone("58"))
    deal = amen.deals.create(offer_type="service", offer_title="Cancel scenario", offer_price="50.00",
                             offer_category=amen.lookups.categories()[0]["id"], deal_subject_details="Kit test")
    amen.deals.set_parties(deal.number, buyers=[buyer.number], sellers=[seller.number])
    reason = amen.lookups.cancel_reasons(party_type="buyer")[0]["id"]
    step("cancel", amen.deals.actions.cancel(deal.number, deal_party="buyer", reason=reason, comment="Changed my mind"))
else:
    n = sys.argv[2]
    reason = amen.lookups.dispute_reasons()[0]["id"]
    step("dispute", amen.deals.actions.dispute(n, reason=reason, comment="Item not as described"))
    resolution = amen.lookups.dispute_resolution_reasons()[0]["id"]
    step("dispute-approve", amen.deals.actions.dispute_approve(n, reason=resolution, comment="Refund the buyer"))
