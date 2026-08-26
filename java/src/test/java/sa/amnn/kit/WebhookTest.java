package sa.amnn.kit;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import sa.amnn.kit.webhooks.WebhookHandler;
import sa.amnn.kit.webhooks.WebhookSignature;

class WebhookTest {
    static final String SECRET = "unit-test-secret";
    @Test void signatureRoundtrip() {
        byte[] body = "{\"id\":\"e1\",\"event\":\"deal.paid\"}".getBytes(StandardCharsets.UTF_8); String sig = WebhookSignature.compute(SECRET, body);
        assertTrue(WebhookSignature.verify(SECRET, body, sig)); assertTrue(WebhookSignature.verify(SECRET, body, "sha256=" + sig));
        assertFalse(WebhookSignature.verify(SECRET, "{\"id\":\"e1\",\"event\":\"deal.paid\"} ".getBytes(StandardCharsets.UTF_8), sig)); assertFalse(WebhookSignature.verify(SECRET, body, null));
    }
    @Test void handlerVerifiesAndDedupes() {
        List<String> seen = new ArrayList<>(); WebhookHandler h = new WebhookHandler(SECRET, e -> seen.add(e.id()));
        byte[] body = "{\"id\":\"e1\",\"event\":\"deal.paid\"}".getBytes(StandardCharsets.UTF_8); Map<String, String> good = Map.of("X-Signature", WebhookSignature.compute(SECRET, body));
        assertEquals(401, h.handle(Map.of("X-Signature", "bad"), body).status());
        assertEquals(Map.of("ok", true), h.handle(good, body).body());
        assertEquals(true, h.handle(good, body).body().get("duplicate"));
        assertEquals(List.of("e1"), seen);
    }
}
