package sa.amnn.kit.webhooks

import kotlinx.serialization.json.*
import sa.amnn.kit.json
import java.util.Collections

data class WebhookEvent(val id: String, val type: String?, val data: JsonObject, val raw: ByteArray)
data class WebhookResult(val status: Int, val body: Map<String, Any>)

/** Framework-agnostic (Ktor, Spring, http4k): call [handle] with the headers and RAW body bytes.
 *  Verifies the real Amen signature scheme first (timestamp + "." + rawBody), de-duplicates by the
 *  top-level event timestamp (no event id exists in the body), then dispatches. Keep [onEvent] fast;
 *  queue heavy work. */
class WebhookHandler(private val secret: String, private val onEvent: suspend (WebhookEvent) -> Unit) {
    private val seen: MutableSet<String> = Collections.synchronizedSet(HashSet())

    private fun header(headers: Map<String, String>, name: String): String? =
        headers.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value

    suspend fun handle(headers: Map<String, String>, rawBody: ByteArray): WebhookResult {
        val sig = header(headers, WebhookSignature.SIGNATURE_HEADER)
        val timestamp = header(headers, WebhookSignature.TIMESTAMP_HEADER)
        if (!WebhookSignature.verify(secret, timestamp, rawBody, sig)) return WebhookResult(401, mapOf("error" to "invalid signature"))
        val data = runCatching { json.parseToJsonElement(rawBody.decodeToString()).jsonObject }.getOrNull() ?: return WebhookResult(400, mapOf("error" to "invalid json"))
        val type = (data["event"]?.jsonPrimitive?.contentOrNull) ?: header(headers, WebhookSignature.EVENT_HEADER)
        val id = data["timestamp"]?.jsonPrimitive?.contentOrNull ?: timestamp ?: ""
        if (id.isNotEmpty() && !seen.add(id)) return WebhookResult(200, mapOf("ok" to true, "duplicate" to true))
        onEvent(WebhookEvent(id, type, data, rawBody))
        return WebhookResult(200, mapOf("ok" to true))
    }
}
