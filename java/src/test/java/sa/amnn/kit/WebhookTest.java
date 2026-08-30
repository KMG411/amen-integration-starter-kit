package sa.amnn.kit;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import sa.amnn.kit.webhooks.WebhookHandler;
import sa.amnn.kit.webhooks.WebhookSignature;

class WebhookTest {
    static final String SECRET = "unit-test-secret";

    // Golden vector captured from a real Amen delivery (2026-08-30).
    static final String GV_SECRET = "whsec_test";
    static final String GV_TS = "2026-08-30T18:53:23.885957+00:00";
    static final String GV_BODY = "{\"event\":\"deal.status.changed\",\"timestamp\":\"2026-08-30T18:53:23.885957+00:00\",\"payload\":{\"number\":\"D-0000000002\",\"status\":\"paid\"}}";
    static final String GV_HEX = "950ca0ff7494dd435d4dc9d7e7ebe31cf54f0859a28a69a686d77e8db9dfd45c";

    @Test void goldenVector() {
        byte[] body = GV_BODY.getBytes(StandardCharsets.UTF_8);
        assertEquals("sha256=" + GV_HEX, WebhookSignature.compute(GV_SECRET, GV_TS, body));
        assertTrue(WebhookSignature.verify(GV_SECRET, GV_TS, body, "sha256=" + GV_HEX));
        assertTrue(WebhookSignature.verify(GV_SECRET, GV_TS, body, GV_HEX)); // prefix optional
    }

    @Test void signatureRoundtripAndTamper() {
        String ts = "2026-08-30T18:53:23.885957+00:00";
        byte[] body = "{\"event\":\"deal.status.changed\",\"timestamp\":\"" + ts + "\"}".getBytes(StandardCharsets.UTF_8);
        String sig = WebhookSignature.compute(SECRET, ts, body);
        assertTrue(sig.startsWith("sha256="));
        assertTrue(WebhookSignature.verify(SECRET, ts, body, sig));
        // tampered body
        assertFalse(WebhookSignature.verify(SECRET, ts, (new String(body, StandardCharsets.UTF_8) + " ").getBytes(StandardCharsets.UTF_8), sig));
        // tampered timestamp (signature is bound to it)
        assertFalse(WebhookSignature.verify(SECRET, "2026-08-30T18:53:23.885958+00:00", body, sig));
        // missing signature / missing timestamp
        assertFalse(WebhookSignature.verify(SECRET, ts, body, null));
        assertFalse(WebhookSignature.verify(SECRET, null, body, sig));
        assertFalse(WebhookSignature.verify(SECRET, "", body, sig));
    }

    @Test void handlerVerifiesDedupesAndDispatches() {
        List<String> seen = new ArrayList<>();
        WebhookHandler h = new WebhookHandler(SECRET, e -> seen.add(e.type() + ":" + e.id()));
        String ts = "2026-08-30T18:53:23.885957+00:00";
        byte[] body = ("{\"event\":\"deal.status.changed\",\"timestamp\":\"" + ts + "\",\"payload\":{\"status\":\"paid\"}}").getBytes(StandardCharsets.UTF_8);
        Map<String, String> good = new HashMap<>();
        good.put(WebhookSignature.SIGNATURE_HEADER, WebhookSignature.compute(SECRET, ts, body));
        good.put(WebhookSignature.TIMESTAMP_HEADER, ts);
        good.put(WebhookSignature.EVENT_HEADER, "deal.status.changed");

        // bad signature -> 401
        Map<String, String> bad = new HashMap<>(good);
        bad.put(WebhookSignature.SIGNATURE_HEADER, "sha256=bad");
        assertEquals(401, h.handle(bad, body).status());

        // missing timestamp -> 401
        Map<String, String> noTs = new HashMap<>(good);
        noTs.remove(WebhookSignature.TIMESTAMP_HEADER);
        assertEquals(401, h.handle(noTs, body).status());

        // case-insensitive header lookup, first delivery -> 200
        Map<String, String> lower = new HashMap<>();
        lower.put("x-webhook-signature", WebhookSignature.compute(SECRET, ts, body));
        lower.put("x-webhook-timestamp", ts);
        WebhookHandler.Result first = h.handle(lower, body);
        assertEquals(200, first.status());
        assertEquals(Map.of("ok", true), first.body());

        // duplicate (same event timestamp) -> 200 + duplicate flag, not re-dispatched
        WebhookHandler.Result dup = h.handle(good, body);
        assertEquals(200, dup.status());
        assertEquals(true, dup.body().get("duplicate"));

        assertEquals(List.of("deal.status.changed:" + ts), seen);
    }
}
