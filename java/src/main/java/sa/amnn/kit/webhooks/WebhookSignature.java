package sa.amnn.kit.webhooks;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Signature verification over the RAW body, bound to the delivery timestamp.
 *  Amen signs {@code timestamp + "." + rawBody} (bytes) and sends the hex HMAC-SHA256 as
 *  {@code X-Webhook-Signature: sha256=<hex>} alongside the ISO-8601 {@code X-Webhook-Timestamp}. */
public final class WebhookSignature {
    /** Signature header: value is {@code sha256=<hex>}. */
    public static final String SIGNATURE_HEADER = "X-Webhook-Signature";
    /** ISO-8601 timestamp header; part of the signed message and the de-dupe key. */
    public static final String TIMESTAMP_HEADER = "X-Webhook-Timestamp";
    /** Event type header; mirrors the top-level {@code event} field in the JSON body. */
    public static final String EVENT_HEADER = "X-Webhook-Event";
    private static final String ALGORITHM = "HmacSHA256";
    private static final String PREFIX = "sha256=";
    private WebhookSignature() {}

    /** {@code sha256=} + hex(HmacSHA256(secret, utf8(timestamp) + "." + rawBody)). */
    public static String compute(String secret, String timestamp, byte[] rawBody) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            mac.update(timestamp.getBytes(StandardCharsets.UTF_8));
            mac.update((byte) '.');
            mac.update(rawBody);
            return PREFIX + hex(mac.doFinal());
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    /** Recompute and constant-time compare. Accepts {@code received} with or without the {@code sha256=} prefix.
     *  False if {@code received} or {@code timestamp} is null/blank. */
    public static boolean verify(String secret, String timestamp, byte[] rawBody, String received) {
        if (received == null || received.isBlank() || timestamp == null || timestamp.isBlank()) return false;
        String given = strip(received).trim().toLowerCase();
        String expected = strip(compute(secret, timestamp, rawBody));
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), given.getBytes(StandardCharsets.UTF_8));
    }

    private static String strip(String v) { return v.startsWith(PREFIX) ? v.substring(PREFIX.length()) : v; }
    private static String hex(byte[] b) { StringBuilder sb = new StringBuilder(b.length * 2); for (byte x : b) sb.append(String.format("%02x", x)); return sb.toString(); }
}
