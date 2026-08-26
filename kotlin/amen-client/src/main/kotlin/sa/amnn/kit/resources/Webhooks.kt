package sa.amnn.kit.resources

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import sa.amnn.kit.AmenClient
import sa.amnn.kit.Webhook

class Webhooks(private val c: AmenClient) {
    suspend fun list() = c.request("GET", "/web-hooks/", ListSerializer(Webhook.serializer())) ?: emptyList()
    /** secretKey in the response is shown ONLY now — store it in a secret manager immediately. */
    suspend fun create(url: String) = c.request("POST", "/web-hooks/", Webhook.serializer(), body = buildJsonObject { put("url", url) })!!
    suspend fun delete(id: String) { c.request("DELETE", "/web-hooks/$id", JsonElement.serializer()) }
}
