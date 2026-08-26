# Amen API — Java reference implementation

Java 17, `java.net.http.HttpClient`, Jackson. Maven. No framework dependency — drop `sa.amnn.kit` into Spring Boot, Quarkus or plain Java.

```sh
mvn -q test                                  # offline unit tests
AMN_API_KEY=… mvn -q compile exec:java       # golden path (or put the key in ../.env)
mvn -q -Pintegration test                    # sandbox; skipped when AMN_API_KEY is unset
```

```java
AmenClient amen = new AmenClient();                       // config from env
Deal deal = amen.deals().create(new CreateDeal("product", "iPhone 15").price("3500.00").deliveryFee("25.00").category(12));
amen.deals().setParties(deal.number(), List.of(buyer.number()), List.of(seller.number()));
try { amen.deals().actions().submit(deal.number()); }
catch (AmenApiError e) { if (e.has("deal__delivery_address_required")) { /* … */ } else throw e; }
```

**Spring Boot:** register `AmenClient` as a `@Bean`; read `AMN_*` via `@Value`/`Environment`. The webhook `@PostMapping` must take the raw body (`@RequestBody byte[]`) and pass it to `WebhookHandler` before any JSON parsing.
