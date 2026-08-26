package sa.amnn.kit.webhooks;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Signature verification over the RAW body. Header name and algorithm are one configuration point (docs/04-webhooks.md). */
public final class WebhookSignature {
    public static final String HEADER = "X-Signature";
    private static final String ALGORITHM = "HmacSHA256";
    private WebhookSignature() {}

    public static String compute(String secret, byte[] rawBody) {
        try { Mac mac = Mac.getInstance(ALGORITHM); mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM)); return hex(mac.doFinal(rawBody)); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
    /** Accepts hex digests, optionally prefixed like "sha256=<hex>". Constant-time comparison. */
    public static boolean verify(String secret, byte[] rawBody, String received) {
        if (received == null || received.isBlank()) return false;
        String given = (received.contains("=") ? received.substring(received.indexOf('=') + 1) : received).trim().toLowerCase();
        return MessageDigest.isEqual(compute(secret, rawBody).getBytes(StandardCharsets.UTF_8), given.getBytes(StandardCharsets.UTF_8));
    }
    private static String hex(byte[] b) { StringBuilder sb = new StringBuilder(); for (byte x : b) sb.append(String.format("%02x", x)); return sb.toString(); }
}
