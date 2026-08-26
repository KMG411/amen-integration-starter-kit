package sa.amnn.kit.webhooks

import kotlinx.serialization.json.*
import sa.amnn.kit.json
import java.util.Collections

data class WebhookEvent(val id: String, val type: String?, val data: JsonObject, val raw: ByteArray)
data class WebhookResult(val status: Int, val body: Map<String, Any>)

/** Framework-agnostic (Ktor, Spring, http4k): call [handle] with the headers and RAW body bytes.
 *  Verifies first, de-duplicates by id, then dispatches. Keep [onEvent] fast; queue heavy work. */
class WebhookHandler(private val secret: String, private val onEvent: suspend (WebhookEvent) -> Unit) {
    private val seen: MutableSet<String> = Collections.synchronizedSet(HashSet())

    suspend fun handle(headers: Map<String, String>, rawBody: ByteArray): WebhookResult {
        val sig = headers.entries.firstOrNull { it.key.equals(WebhookSignature.HEADER, ignoreCase = true) }?.value
        if (!WebhookSignature.verify(secret, rawBody, sig)) return WebhookResult(401, mapOf("error" to "invalid signature"))
        val data = runCatching { json.parseToJsonElement(rawBody.decodeToString()).jsonObject }.getOrNull() ?: return WebhookResult(400, mapOf("error" to "invalid json"))
        val id = (data["id"] ?: data["event_id"])?.jsonPrimitive?.contentOrNull ?: ""
        if (id.isNotEmpty() && !seen.add(id)) return WebhookResult(200, mapOf("ok" to true, "duplicate" to true))
        onEvent(WebhookEvent(id, (data["event"] ?: data["type"])?.jsonPrimitive?.contentOrNull, data, rawBody))
        return WebhookResult(200, mapOf("ok" to true))
    }
}
