package sa.amnn.kit.webhooks;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import sa.amnn.kit.Models;

/** Framework-agnostic (Spring, Quarkus, servlet): call {@link #handle} with the headers and RAW body bytes.
 *  Verifies the {@code X-Webhook-Signature} over {@code X-Webhook-Timestamp + "." + rawBody}, de-duplicates by
 *  the delivery timestamp, then dispatches. Keep the consumer fast; queue heavy work. */
public final class WebhookHandler {
    public record Event(String id, String type, JsonNode data, byte[] raw) {}
    public record Result(int status, Map<String, Object> body) {}
    private final String secret; private final Consumer<Event> onEvent; private final Set<String> seen = Collections.synchronizedSet(new HashSet<>());
    public WebhookHandler(String secret, Consumer<Event> onEvent) { this.secret = secret; this.onEvent = onEvent; }

    public Result handle(Map<String, String> headers, byte[] rawBody) {
        String sig = header(headers, WebhookSignature.SIGNATURE_HEADER);
        String timestamp = header(headers, WebhookSignature.TIMESTAMP_HEADER);
        if (!WebhookSignature.verify(secret, timestamp, rawBody, sig)) return new Result(401, Map.of("error", "invalid signature"));
        JsonNode data;
        try { data = Models.JSON.readTree(rawBody); if (data == null || !data.isObject()) throw new IllegalArgumentException(); } catch (Exception e) { return new Result(400, Map.of("error", "invalid json")); }
        String type = data.hasNonNull("event") ? data.get("event").asText() : data.path("type").asText(null);
        String id = data.hasNonNull("timestamp") ? data.get("timestamp").asText() : String.valueOf(timestamp);
        if (!id.isEmpty() && !seen.add(id)) return new Result(200, Map.of("ok", true, "duplicate", true));
        onEvent.accept(new Event(id, type, data, rawBody));
        return new Result(200, Map.of("ok", true));
    }

    private static String header(Map<String, String> headers, String name) {
        return headers.entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase(name)).map(Map.Entry::getValue).findFirst().orElse(null);
    }
}
