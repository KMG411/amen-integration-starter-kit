package sa.amnn.kit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import sa.amnn.kit.resources.*;

/** AmenClient — the one place that knows about auth headers, base URL, timeouts and retries. */
public final class AmenClient {
    /** Transport abstraction so tests can inject responses. */
    public interface Transport { Response send(HttpRequest request) throws IOException, InterruptedException; }
    public record Response(int status, String body, Map<String, List<String>> headers) {}

    public final Config config;
    private final Transport transport;
    private final Lookups lookups; private final AccountResource account; private final Customers customers;
    private final Deals deals; private final Withdrawals withdrawals; private final Webhooks webhooks;

    public AmenClient() { this(Config.fromEnv()); }
    public AmenClient(Config config) { this(config, defaultTransport(config)); }
    public AmenClient(Config config, Transport transport) {
        this.config = config; this.transport = transport;
        lookups = new Lookups(this); account = new AccountResource(this); customers = new Customers(this);
        deals = new Deals(this); withdrawals = new Withdrawals(this); webhooks = new Webhooks(this);
    }
    public Lookups lookups() { return lookups; } public AccountResource account() { return account; } public Customers customers() { return customers; }
    public Deals deals() { return deals; } public Withdrawals withdrawals() { return withdrawals; } public Webhooks webhooks() { return webhooks; }

    static Transport defaultTransport(Config c) {
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(c.timeoutMs())).build();
        return req -> { HttpResponse<String> r = http.send(req, HttpResponse.BodyHandlers.ofString()); return new Response(r.statusCode(), r.body(), r.headers().map()); };
    }

    /** Multipart form field: plain value or file. */
    public record Part(String name, String value, byte[] file, String filename, String mimeType) {
        public static Part text(String name, String value) { return new Part(name, value, null, null, null); }
        public static Part file(String name, String filename, byte[] data, String mimeType) { return new Part(name, null, data, filename, mimeType); }
    }

    public <T> T get(String path, Map<String, String> params, TypeReference<T> type) { return request("GET", path, params, null, null, type); }
    public <T> T post(String path, Object json, TypeReference<T> type) { return request("POST", path, Map.of(), json, null, type); }
    public <T> T postForm(String path, List<Part> parts, TypeReference<T> type) { return request("POST", path, Map.of(), null, parts, type); }
    public <T> T put(String path, Object json, TypeReference<T> type) { return request("PUT", path, Map.of(), json, null, type); }
    public void delete(String path) { request("DELETE", path, Map.of(), null, null, new TypeReference<JsonNode>() {}); }

    public <T> T request(String method, String path, Map<String, String> params, Object json, List<Part> parts, TypeReference<T> type) {
        String query = params.entrySet().stream().filter(e -> e.getValue() != null)
            .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8)).collect(Collectors.joining("&"));
        String url = config.baseUrl() + Config.API_PREFIX + path + (query.isEmpty() ? "" : "?" + query);
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofMillis(config.timeoutMs()))
            .header("X-API-Token", config.apiKey()).header("Accept", "application/json").header("User-Agent", "amen-starter-kit-java/0.1");
        if (!method.equals("GET")) { b.header("Origin", config.baseUrl()).header("Referer", config.baseUrl()); }   // origin checks on mutating calls
        try {
            if (parts != null) { String boundary = "----AmenKit" + UUID.randomUUID(); b.header("Content-Type", "multipart/form-data; boundary=" + boundary).method(method, HttpRequest.BodyPublishers.ofByteArray(multipart(parts, boundary))); }
            else if (json != null) { b.header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(Models.JSON.writeValueAsString(json))); }
            else b.method(method, HttpRequest.BodyPublishers.noBody());
        } catch (IOException e) { throw new RuntimeException(e); }
        HttpRequest req = b.build();

        for (int attempt = 1; ; attempt++) {
            Response res;
            try { res = transport.send(req); }
            catch (IOException | InterruptedException e) { if (attempt > config.maxRetries()) throw new RuntimeException("network error: " + e.getMessage(), e); sleep(attempt, null); continue; }
            if (res.status() < 400) {
                try { return res.body() == null || res.body().isBlank() ? null : Models.JSON.readValue(res.body(), type); }
                catch (IOException e) { throw new RuntimeException("invalid JSON from API: " + e.getMessage(), e); }
            }
            AmenApiError err = toError(res, method, Config.API_PREFIX + path);
            if (err.isRetryable() && attempt <= config.maxRetries()) { sleep(attempt, res.headers().getOrDefault("retry-after", List.of()).stream().findFirst().orElse(null)); continue; }
            throw err;
        }
    }

    static AmenApiError toError(Response res, String method, String path) {
        List<String> codes = new ArrayList<>();
        try { JsonNode n = Models.JSON.readTree(res.body()).get("error"); if (n != null) { if (n.isArray()) n.forEach(x -> codes.add(x.asText())); else codes.add(n.asText()); } } catch (Exception ignored) {}
        return new AmenApiError(res.status(), codes, method, path, res.body());
    }
    private static void sleep(int attempt, String retryAfter) {
        long ms = retryAfter != null && retryAfter.matches("\\d+") ? Long.parseLong(retryAfter) * 1000 : Math.min(1L << attempt, 20) * 1000 + ThreadLocalRandom.current().nextInt(1000);
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
    static byte[] multipart(List<Part> parts, String boundary) throws IOException {
        var out = new java.io.ByteArrayOutputStream();
        for (Part p : parts) {
            out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + p.name() + "\"").getBytes(StandardCharsets.UTF_8));
            if (p.file() != null) { out.write(("; filename=\"" + p.filename() + "\"\r\nContent-Type: " + p.mimeType() + "\r\n\r\n").getBytes(StandardCharsets.UTF_8)); out.write(p.file()); }
            else out.write(("\r\n\r\n" + p.value()).getBytes(StandardCharsets.UTF_8));
            out.write("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }

    /** Lenient page decoding: `{ <key>: [...], page, pages, total }` or a nested `page` object. */
    public <T> Models.Page<T> page(String path, Map<String, String> params, String key, Class<T> itemType) {
        JsonNode n = get(path, params, new TypeReference<JsonNode>() {});
        if (n == null) return new Models.Page<>(List.of(), 0, 1, 0);
        JsonNode arr = n.has(key) ? n.get(key) : n.has("results") ? n.get("results") : n.get("items");
        List<T> items = new ArrayList<>();
        if (arr != null) for (JsonNode x : arr) { try { items.add(Models.JSON.treeToValue(x, itemType)); } catch (IOException e) { throw new RuntimeException(e); } }
        JsonNode meta = n.get("page") != null && n.get("page").isObject() ? n.get("page") : n;
        return new Models.Page<>(items, meta.path("page").asInt(0), meta.path("pages").asInt(1), meta.path("total").asInt(0));
    }
}
