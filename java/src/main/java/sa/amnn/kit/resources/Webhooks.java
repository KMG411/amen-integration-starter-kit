package sa.amnn.kit.resources;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import java.util.Map;
import sa.amnn.kit.AmenClient;
import sa.amnn.kit.Models.Webhook;

public final class Webhooks {
    private final AmenClient c; public Webhooks(AmenClient c) { this.c = c; }
    public List<Webhook> list() { return c.get("/web-hooks/", Map.of(), new TypeReference<>() {}); }
    /** secretKey in the response is shown ONLY now — store it in a secret manager immediately. */
    public Webhook create(String url) { return c.post("/web-hooks/", Map.of("url", url), new TypeReference<>() {}); }
    public void delete(String id) { c.delete("/web-hooks/" + id); }
}
