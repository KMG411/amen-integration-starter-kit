package sa.amnn.kit;

import static org.junit.jupiter.api.Assertions.*;

import java.net.http.HttpRequest;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import sa.amnn.kit.Models.CreateCustomer;

class ClientTest {
    static AmenClient client(java.util.function.Function<HttpRequest, AmenClient.Response> f) {
        return new AmenClient(new Config("sandbox", "test-token", "https://sandbox-api.amnn.sa", 20000, null, 1), f::apply);
    }
    static AmenClient.Response json(int status, String body) { return new AmenClient.Response(status, body, Map.of()); }

    @Test void authHeaderAndBaseUrl() {
        var acc = client(r -> { assertEquals("https://sandbox-api.amnn.sa/api/v1/account", r.uri().toString()); assertEquals("test-token", r.headers().firstValue("X-API-Token").orElseThrow()); return json(200, "{\"id\":\"a1\",\"name\":\"test\"}"); }).account().get();
        assertEquals("a1", acc.id());
    }
    @Test void errorCodesParsed() {
        AmenApiError e = assertThrows(AmenApiError.class, () -> client(r -> json(400, "{\"error\":[\"first_name__required\"]}")).customers().create(new CreateCustomer("", "x", "SA", "5")));
        assertEquals(400, e.status); assertTrue(e.has("first_name__required")); assertFalse(e.isRetryable());
    }
    @Test void retries429ThenSucceeds() {
        AtomicInteger calls = new AtomicInteger();
        var cities = client(r -> calls.incrementAndGet() == 1 ? json(429, "{\"error\":[\"rate_limit__exceeded\"]}") : json(200, "[{\"id\":1,\"name\":\"Riyadh\"}]")).lookups().cities();
        assertEquals(1, cities.get(0).id()); assertEquals(2, calls.get());
    }
    @Test void lifecycleGuardBlocksLocally() {
        AtomicInteger calls = new AtomicInteger();
        assertThrows(AmenLifecycleError.class, () -> client(r -> { calls.incrementAndGet(); return json(200, "{\"number\":\"DL-1\",\"status\":\"draft\"}"); }).deals().actions().approve("DL-1", null));
        assertEquals(1, calls.get());
    }
    @Test void originOnMutatingRequests() {
        var wh = client(r -> { assertEquals("https://sandbox-api.amnn.sa", r.headers().firstValue("Origin").orElseThrow()); return json(201, "{\"id\":\"w\",\"url\":\"u\",\"secret_key\":\"s\"}"); }).webhooks().create("https://example.com/hook");
        assertEquals("s", wh.secretKey());
    }
    @Test void snakeCaseAndEpochMillis() throws Exception {
        assertTrue(Models.JSON.writeValueAsString(new Models.CreateDeal("product", "t").price("1.00")).contains("\"offer_type\""));
        var deal = Models.JSON.readValue("{\"number\":\"DL-1\",\"status\":\"draft\",\"created_at\":1679568486000,\"unknown\":1}", Models.Deal.class);
        assertEquals(2023, Models.toInstant(deal.createdAt()).atZone(java.time.ZoneOffset.UTC).getYear());
    }
}
