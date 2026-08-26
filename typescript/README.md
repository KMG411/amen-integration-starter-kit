# Amen API — TypeScript reference implementation

Node 20+, native `fetch`, zero runtime dependencies besides `dotenv`. The same client is safe to use from a Next.js/Express server — **never from the browser** (the token must stay server-side; see `docs/06-security-checklist.md`).

```sh
npm install
cp ../.env.example ../.env       # set AMN_API_KEY
npm run example:golden-path
npm run test:unit                # offline
npm run test:integration         # sandbox; skipped when AMN_API_KEY is unset
```

```ts
import { AmenClient, AmenApiError } from "./src/amen/index.js";

const amen = new AmenClient();                      // config from env
const deal = await amen.deals.create({ offer_type: "product", offer_title: "iPhone 15",
                                       offer_price: "3500.00", offer_delivery_fee: "25.00", offer_category: 12 });
await amen.deals.setParties(deal.number, { buyers: [buyer.number], sellers: [seller.number] });
try { await amen.deals.actions.submit(deal.number); }
catch (e) { if (e instanceof AmenApiError && e.has("deal__delivery_address_required")) { /* … */ } else throw e; }
```

Dashboard pattern: put `AmenClient` in a server route (`/api/amen/*`) and have the React/Next.js UI call that route.
