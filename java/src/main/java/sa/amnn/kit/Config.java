package sa.amnn.kit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Environment-based configuration. Walks up from cwd (max 3 levels) to find a .env. Existing env vars win. */
public record Config(String env, String apiKey, String baseUrl, int timeoutMs, String webhookSecret, int maxRetries) {
    public static final Map<String, String> BASE_URLS = Map.of("sandbox", "https://sandbox-api.amnn.sa", "live", "https://api.amnn.sa");
    public static final String API_PREFIX = "/api/v1";

    public Config(String apiKey) { this("sandbox", apiKey, BASE_URLS.get("sandbox"), 20000, null, 3); }

    public static Config fromEnv() {
        Map<String, String> file = loadDotenv();
        java.util.function.Function<String, String> get = k -> System.getenv(k) != null ? System.getenv(k) : file.get(k);
        String env = (get.apply("AMN_ENV") == null ? "sandbox" : get.apply("AMN_ENV")).toLowerCase();
        if (!BASE_URLS.containsKey(env)) throw new IllegalArgumentException("AMN_ENV must be 'sandbox' or 'live', got '" + env + "'");
        String key = get.apply("AMN_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("AMN_API_KEY is not set (see .env.example)");
        String base = get.apply("AMN_BASE_URL") != null ? get.apply("AMN_BASE_URL") : BASE_URLS.get(env);
        int timeout = get.apply("AMN_TIMEOUT_MS") != null ? Integer.parseInt(get.apply("AMN_TIMEOUT_MS")) : 20000;
        String secret = get.apply("AMN_WEBHOOK_SECRET");
        return new Config(env, key, base, timeout, secret == null || secret.isBlank() ? null : secret, 3);
    }

    static Map<String, String> loadDotenv() {
        Path dir = Path.of("").toAbsolutePath();
        for (int i = 0; i < 3 && dir != null; i++, dir = dir.getParent()) {
            Path f = dir.resolve(".env");
            if (!Files.isRegularFile(f)) continue;
            Map<String, String> out = new HashMap<>();
            try {
                for (String line : Files.readAllLines(f)) {
                    String l = line.trim();
                    if (l.isEmpty() || l.startsWith("#") || !l.contains("=")) continue;
                    int eq = l.indexOf('=');
                    String v = l.substring(eq + 1).replaceAll("\\s+#.*$", "").trim();
                    if (!v.isEmpty()) out.put(l.substring(0, eq).trim(), v);
                }
            } catch (IOException ignored) {}
            return out;   // nearest .env only — do not leak a parent project's config
        }
        return Map.of();
    }
}
