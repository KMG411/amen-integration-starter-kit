package sa.amnn.kit.webhooks

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** Signature verification over the RAW body. Header name and algorithm are one configuration point (docs/04-webhooks.md). */
object WebhookSignature {
    const val HEADER = "X-Signature"
    private const val ALGORITHM = "HmacSHA256"

    fun compute(secret: String, rawBody: ByteArray): String =
        Mac.getInstance(ALGORITHM).apply { init(SecretKeySpec(secret.toByteArray(), ALGORITHM)) }.doFinal(rawBody).joinToString("") { "%02x".format(it) }

    /** Accepts hex digests, optionally prefixed like "sha256=<hex>". Constant-time comparison. */
    fun verify(secret: String, rawBody: ByteArray, received: String?): Boolean {
        if (received.isNullOrBlank()) return false
        val given = received.substringAfter('=', received).trim().lowercase()
        return MessageDigest.isEqual(compute(secret, rawBody).toByteArray(), given.toByteArray())
    }
}
