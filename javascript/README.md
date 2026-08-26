# Amen API — JavaScript (Node) reference implementation

Plain ESM JavaScript on Node 20+ with native `fetch`; no build step. Uses only `dotenv` (config) and `express` (webhook receiver example). Tests use `node --test`.

```sh
npm install
cp ../.env.example ../.env       # set AMN_API_KEY
npm run example:golden-path
npm run test:unit                # offline
npm run test:integration         # sandbox; skipped when AMN_API_KEY is unset
```

```js
import { AmenClient, AmenApiError } from "./src/amen/index.js";
const amen = new AmenClient();
const deal = await amen.deals.create({ offer_type: "product", offer_title: "iPhone 15", offer_price: "3500.00", offer_delivery_fee: "25.00", offer_category: 12 });
await amen.deals.setParties(deal.number, { buyers: [buyer.number], sellers: [seller.number] });
try { await amen.deals.actions.submit(deal.number); }
catch (e) { if (e instanceof AmenApiError && e.has("deal__delivery_address_required")) { /* … */ } else throw e; }
```

Server-side only — never ship the API token to a browser.
