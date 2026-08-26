package sa.amnn.kit.examples;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import sa.amnn.kit.Config;
import sa.amnn.kit.Models;
import sa.amnn.kit.webhooks.WebhookHandler;

/** Minimal receiver with the JDK HTTP server (no framework). Reads the RAW body before parsing. Listens on :8080/webhook */
public final class WebhookReceiver {
    public static void main(String[] args) throws Exception {
        String secret = Config.fromEnv().webhookSecret();
        if (secret == null) throw new IllegalStateException("AMN_WEBHOOK_SECRET is required");
        WebhookHandler handler = new WebhookHandler(secret, e -> System.out.println("📩 " + e.type() + " " + e.id() + ": " + e.data()));
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/webhook", ex -> {
            byte[] raw = ex.getRequestBody().readAllBytes();
            Map<String, String> headers = new HashMap<>(); ex.getRequestHeaders().forEach((k, v) -> headers.put(k, v.get(0)));
            WebhookHandler.Result r = "POST".equals(ex.getRequestMethod()) ? handler.handle(headers, raw) : new WebhookHandler.Result(405, Map.of());
            byte[] body = Models.JSON.writeValueAsBytes(r.body());
            ex.getResponseHeaders().set("Content-Type", "application/json"); ex.sendResponseHeaders(r.status(), body.length); ex.getResponseBody().write(body); ex.close();
        });
        System.out.println("listening on http://0.0.0.0:8080/webhook"); server.start();
    }
}
