package sa.amnn.kit.webhooks

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Signature verification for real Amen webhook deliveries (verified 2026-08-30).
 *
 * The signed message is the timestamp header, a literal '.', then the RAW body bytes:
 *   signedMessage = utf8(timestamp) + "." + rawBody
 *   signature     = "sha256=" + lowercaseHex(HmacSHA256(secret, signedMessage))
 *
 * Never re-serialize the JSON body before signing; sign the exact bytes received.
 */
object WebhookSignature {
    const val SIGNATURE_HEADER = "X-Webhook-Signature"
    const val TIMESTAMP_HEADER = "X-Webhook-Timestamp"
    const val EVENT_HEADER = "X-Webhook-Event"
    const val PREFIX = "sha256="
    private const val ALGORITHM = "HmacSHA256"

    /** Lowercase hex of HmacSHA256 over utf8(timestamp) + "." + rawBody. No "sha256=" prefix. */
    fun compute(secret: String, timestamp: String, rawBody: ByteArray): String {
        val mac = Mac.getInstance(ALGORITHM).apply { init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), ALGORITHM)) }
        mac.update(timestamp.toByteArray(Charsets.UTF_8))
        mac.update('.'.code.toByte())
        mac.update(rawBody)
        return mac.doFinal().joinToString("") { "%02x".format(it) }
    }

    /**
     * Recompute and constant-time compare. Returns false if [received] or [timestamp] is null/blank.
     * Accepts the received value with or without the "sha256=" prefix.
     */
    fun verify(secret: String, timestamp: String?, rawBody: ByteArray, received: String?): Boolean {
        if (received.isNullOrBlank() || timestamp.isNullOrBlank()) return false
        val given = received.removePrefix(PREFIX).trim().lowercase()
        val expected = compute(secret, timestamp, rawBody)
        return MessageDigest.isEqual(expected.toByteArray(Charsets.UTF_8), given.toByteArray(Charsets.UTF_8))
    }
}
