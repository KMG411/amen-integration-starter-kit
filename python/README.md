# Amen API — Python reference implementation

```sh
pip install -e ".[dev]"
cp ../.env.example ../.env   # set AMN_API_KEY (the client also reads ./.env)
python examples/01_golden_path.py
pytest tests/unit            # offline
pytest tests/integration     # sandbox; skipped when AMN_API_KEY is unset
```

```python
from amen import AmenClient, AmenApiError

amen = AmenClient()                       # config from env: AMN_ENV, AMN_API_KEY
deal = amen.deals.create(offer_type="product", offer_title="iPhone 15",
                         offer_price="3500.00", offer_delivery_fee="25.00", offer_category=12)
amen.deals.set_parties(deal.number, buyers=[buyer.number], sellers=[seller.number])
try:
    amen.deals.actions.submit(deal.number)
except AmenApiError as e:
    if e.has("deal__delivery_address_required"): ...
```

Layout follows the kit convention: `src/amen/{client,config,errors,models,resources/,webhooks/}`, `examples/`, `tests/`.
